/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.checkers.*
import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.backend.common.checkers.context.ContextUpdater
import org.jetbrains.kotlin.backend.common.checkers.context.ParentChainUpdater
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.ir.IrBuiltIns
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
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

typealias ReportIrValidationError = (IrFile?, IrElement, String, List<IrElement>) -> Unit

open class IrValidationException(message: String? = null, cause: Throwable? = null) : IllegalStateException(message, cause)

private class IrValidator(
    val validatorConfig: IrValidatorConfig,
    val irBuiltIns: IrBuiltIns,
    val reportError: ReportIrValidationError,
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

    private val checkersPerElement = object : ClassValue<List<IrElementChecker<*>>>() {
        override fun computeValue(type: Class<*>): List<IrElementChecker<*>> =
            elementCheckers.filter { it.elementClass.isAssignableFrom(type) }
    }

    override fun visitElement(element: IrElement) {
        var block = { element.acceptChildrenVoid(this) }
        for (contextUpdater in contextUpdaters) {
            val currentBlock = block
            block = { contextUpdater.runInNewContext(context, element, currentBlock) }
        }
        block()

        for (checker in checkersPerElement.get(element.javaClass)) {
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
        super.visitType(container, type)
        for (checker in typeCheckers) {
            checker.check(type, container, context)
        }
    }
}

/**
 * Verifies common IR invariants that should hold in all the backends.
 */
private fun performBasicIrValidation(
    element: IrElement,
    irBuiltIns: IrBuiltIns,
    validatorConfig: IrValidatorConfig,
    reportError: ReportIrValidationError,
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

/**
 * [IrValidationContext] is responsible for collecting validation errors, logging them and optionally throwing [IrValidationException]
 * (if the verification mode passed to [validateIr] is [IrVerificationMode.ERROR])
 */
sealed interface IrValidationContext {

    /**
     * A string that each validation error will begin with.
     */
    var customMessagePrefix: String?

    /**
     * Logs the validation error into the underlying [MessageCollector].
     */
    fun reportIrValidationError(
        file: IrFile?,
        element: IrElement,
        message: String,
        phaseName: String,
        parentChain: List<IrElement> = emptyList(),
    )

    /**
     * Allows to abort the compilation process if after or during validating the IR there were errors and the verification mode is
     * [IrVerificationMode.ERROR].
     */
    fun throwValidationErrorIfNeeded()

    /**
     * Verifies common IR invariants that should hold in all the backends.
     *
     * Reports errors to [CommonBackendContext.messageCollector].
     *
     * **Note:** this method does **not** throw [IrValidationException]. Use [throwValidationErrorIfNeeded] for checking for errors and throwing
     * [IrValidationException]. This gives the caller the opportunity to perform additional (for example, backend-specific) validation before
     * aborting. The caller decides when it's time to abort.
     */
    fun performBasicIrValidation(
        fragment: IrElement,
        irBuiltIns: IrBuiltIns,
        phaseName: String,
        config: IrValidatorConfig,
    ) {
        performBasicIrValidation(fragment, irBuiltIns, config) { file, element, message, parentChain ->
            reportIrValidationError(file, element, message, phaseName, parentChain)
        }
    }
}

private class IrValidationContextImpl(
    private val messageCollector: MessageCollector,
    private val mode: IrVerificationMode,
) : IrValidationContext {

    override var customMessagePrefix: String? = null

    private var hasValidationErrors: Boolean = false

    override fun reportIrValidationError(
        file: IrFile?,
        element: IrElement,
        message: String,
        phaseName: String,
        parentChain: List<IrElement>,
    ) {
        val severity = when (mode) {
            IrVerificationMode.WARNING -> CompilerMessageSeverity.WARNING
            IrVerificationMode.ERROR -> CompilerMessageSeverity.ERROR
            IrVerificationMode.NONE -> return
        }
        hasValidationErrors = true
        val phaseMessage = if (phaseName.isNotEmpty()) "$phaseName: " else ""
        messageCollector.report(
            severity,
            buildString {
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
            },
            file?.let(element::getCompilerMessageLocation),
        )
    }

    override fun throwValidationErrorIfNeeded() {
        if (hasValidationErrors && mode == IrVerificationMode.ERROR) {
            throw IrValidationException()
        }
    }
}

/**
 * Logs validation errors encountered during the execution of the [runValidationRoutines] closure into [messageCollector].
 *
 * If [mode] is [IrVerificationMode.ERROR], throws [IrValidationException] after [runValidationRoutines] has finished,
 * thus allowing to collect as many errors as possible instead of aborting after the first one.
 */
fun validateIr(
    messageCollector: MessageCollector,
    mode: IrVerificationMode,
    runValidationRoutines: IrValidationContext.() -> Unit,
) {
    if (mode == IrVerificationMode.NONE) return
    val validationContext = IrValidationContextImpl(messageCollector, mode)
    validationContext.runValidationRoutines()
    validationContext.throwValidationErrorIfNeeded()
}
