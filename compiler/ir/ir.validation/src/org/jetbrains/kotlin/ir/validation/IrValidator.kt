/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrTreeSymbolsVisitor
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.validation.checkers.*
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.validation.checkers.context.ContextUpdater
import org.jetbrains.kotlin.ir.validation.checkers.context.ParentChainUpdater
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class IrValidationError(
    val file: IrFile?,
    val element: IrElement,
    val cause: Cause,
    val message: String,
    val parentChain: List<IrElement>,
) {
    interface Cause {
        object IrTreeInconsistency : Cause
        object UnboundSymbol : Cause
    }
}

open class IrValidationException(message: String? = null, cause: Throwable? = null) : IllegalStateException(message, cause)

private class IrValidator(
    val validatorConfig: IrValidatorConfig,
    val irBuiltIns: IrBuiltIns,
    val reportError: (IrValidationError) -> Unit,
) : IrVisitorVoid() {
    override fun visitElement(element: IrElement) =
        throw IllegalStateException("IR validation must start from files, modules, or declarations")

    override fun visitFile(declaration: IrFile) {
        val context = CheckerContext(irBuiltIns, declaration, reportError)
        val fileValidator = IrFileValidator(validatorConfig, context)
        declaration.acceptVoid(fileValidator)
    }

    override fun visitModuleFragment(declaration: IrModuleFragment) = declaration.acceptChildrenVoid(this)

    override fun visitDeclaration(declaration: IrDeclarationBase) {
        val context = CheckerContext(irBuiltIns, declaration.file, reportError)
        val fileValidator = IrFileValidator(validatorConfig, context)
        declaration.acceptVoid(fileValidator)
    }
}

private class IrFileValidator(
    config: IrValidatorConfig,
    private val context: CheckerContext
) : IrTreeSymbolsVisitor() {
    private val contextUpdaters: List<ContextUpdater> = listOf(ParentChainUpdater) + config.checkers.flatMap { it.requiredContextUpdaters }
    private val elementCheckers: List<IrElementChecker<*>> = config.checkers.filterIsInstance<IrElementChecker<*>>()
    private val symbolCheckers: List<IrSymbolChecker> = config.checkers.filterIsInstance<IrSymbolChecker>()
    private val typeCheckers: List<IrTypeChecker> = config.checkers.filterIsInstance<IrTypeChecker>()
    private val typeContextUpdaters: List<ContextUpdater> = typeCheckers.flatMap { it.requiredContextUpdaters }

    private val checkersPerElementCache = hashMapOf<Class<out IrElement>, List<IrElementChecker<*>>>()

    private fun List<ContextUpdater>.runWithContextUpdaters(element: IrElement, block: () -> Unit) {
        this.fold(block) { currentBlock, updater -> { updater.runInNewContext(context, element, currentBlock) } }.invoke()
    }

    private fun getCheckersFor(type: Class<out IrElement>) = checkersPerElementCache.computeIfAbsent(type) {
        elementCheckers.filter { it.elementClass.isAssignableFrom(type) }
    }

    override fun visitElement(element: IrElement) {
        // We use all context updaters for the `IrElement`, not only those required for `IrElementChecker`.
        // This is because the children of a given element may have their own properties that need context-specific validation.
        contextUpdaters.runWithContextUpdaters(element) { element.acceptChildrenVoid(this) }

        for (checker in getCheckersFor(element.javaClass)) {
            @Suppress("UNCHECKED_CAST")
            (checker as IrElementChecker<IrElement>).check(element, context)
        }
    }

    override fun visitAnnotationUsage(annotationUsage: IrConstructorCall) {
        context.withinAnnotationUsageSubTree {
            super.visitAnnotationUsage(annotationUsage)
        }
    }

    override fun visitSymbol(container: IrElement, symbol: IrSymbol) {
        for (checker in symbolCheckers) {
            checker.check(symbol, container, context)
        }
    }

    override fun visitType(container: IrElement, type: IrType) {
        // Types might be visited before their containing element's `visitElement` function is called and the type is added to the context.
        // This is an expected outcome because the type, even though introduced by the element, is its property, not its child.
        // Although it's expected, we still need to work around it by adding type's container to the context while running the following checkers;
        // otherwise the type would be seen as out-of-scope.
        typeContextUpdaters.runWithContextUpdaters(container) {
            super.visitType(container, type)
            for (checker in typeCheckers) {
                checker.check(type, container, context)
            }
        }
    }
}

/**
 * Verifies IR invariants, invokes [reportError] callback for each validation errors.
 *
 * Tries to finish gracefully, even if the IR tree is severely broken, but that is not guaranteed.
 */
fun validateIr(
    element: IrElement,
    irBuiltIns: IrBuiltIns,
    validatorConfig: IrValidatorConfig,
    reportError: (IrValidationError) -> Unit,
) {
    // Phase 1: Traverse the IR tree to check for structural consistency.
    // If any issues are detected, validation stops here to avoid problems like infinite recursion during the next phase.
    if (validatorConfig.checkTreeConsistency || validatorConfig.checkUnboundSymbols) {
        try {
            element.checkTreeConsistency(reportError, validatorConfig)
        } catch (_: IrTreeConsistencyException) {
            return
        }
    }

    if (validatorConfig.checkers.isNotEmpty()) {
        // Phase 2: Traverse the IR tree again to run additional checks based on the validator configuration.
        val validator = IrValidator(validatorConfig, irBuiltIns, reportError)
        element.acceptVoid(validator)
    }
}

enum class IrValidationSeverity(val factory: KtSourcelessDiagnosticFactory) {
    WARNING(IrValidationDiagnostics.IR_VALIDATION_WARNING),
    ERROR(IrValidationDiagnostics.IR_VALIDATION_ERROR),
}

/**
 * Verifies IR invariants, logs validation errors into [diagnosticReporter].
 *
 * If any error with [IrValidationSeverity.ERROR] severity is found, throws [IrValidationException] at the end,
 * thus allowing to collect as many errors as possible instead of aborting after the first one.
 */
fun validateIr(
    element: IrElement,
    irBuiltIns: IrBuiltIns,
    validatorConfig: IrValidatorConfig,
    diagnosticReporter: IrDiagnosticReporter,
    getSeverity: (IrValidationError) -> IrValidationSeverity?,
    phaseName: String? = null,
    customMessagePrefix: String? = null,
): Boolean {
    var hasAnyViolations = false
    var hasAnyErrors = false
    validateIr(element, irBuiltIns, validatorConfig) { error ->
        val severity = getSeverity(error)
        if (severity != null) {
            diagnosticReporter.report(error, severity, phaseName, customMessagePrefix)
            hasAnyViolations = true
        }
        if (severity == IrValidationSeverity.ERROR) {
            hasAnyErrors = true
        }
    }

    if (hasAnyErrors) {
        throw IrValidationException()
    }
    return hasAnyViolations
}

/**
 * Verifies IR invariants, logs validation errors into [diagnosticReporter].
 *
 * If [mode] is [IrVerificationMode.ERROR], throws [IrValidationException] at the end,
 * thus allowing to collect as many errors as possible instead of aborting after the first one.
 */
fun validateIr(
    element: IrElement,
    irBuiltIns: IrBuiltIns,
    validatorConfig: IrValidatorConfig,
    diagnosticReporter: IrDiagnosticReporter,
    mode: IrVerificationMode,
    phaseName: String? = null,
    customMessagePrefix: String? = null,
): Boolean {
    val severity = when (mode) {
        IrVerificationMode.NONE -> return false
        IrVerificationMode.WARNING -> IrValidationSeverity.WARNING
        IrVerificationMode.ERROR -> IrValidationSeverity.ERROR
    }
    return validateIr(element, irBuiltIns, validatorConfig, diagnosticReporter, { severity }, phaseName, customMessagePrefix)
}

fun IrDiagnosticReporter.report(
    error: IrValidationError,
    severity: IrValidationSeverity,
    phaseName: String?,
    customMessagePrefix: String?,
) {
    report(
        severity.factory,
        error.render(phaseName, customMessagePrefix),
        error.file?.let {
            val sourceRangeInfo = it.fileEntry.getSourceRangeInfo(error.element.startOffset, error.element.endOffset)
            CompilerMessageLocation.create(
                path = sourceRangeInfo.filePath,
                line = sourceRangeInfo.startLineNumber + 1,
                column = sourceRangeInfo.startColumnNumber + 1,
                lineContent = null
            )
        },
    )
}

fun IrValidationError.render(phaseName: String?, customMessagePrefix: String?): String = buildString {
    val phaseMessage = if (!phaseName.isNullOrEmpty()) "$phaseName: " else ""
    val customMessagePrefix = customMessagePrefix
    if (customMessagePrefix == null) {
        append("[IR VALIDATION] ")
        append(phaseMessage)
    } else {
        append(customMessagePrefix)
        append(" ")
    }
    appendLine(message)
    append(element.render())
    for ((i, parent) in parentChain.asReversed().withIndex()) {
        appendLine()
        append("  ".repeat(i + 1))
        append("inside ")
        append(parent.render())
    }
}
