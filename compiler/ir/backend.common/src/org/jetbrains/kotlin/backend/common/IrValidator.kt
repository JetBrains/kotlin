/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.checkers.IrElementChecker
import org.jetbrains.kotlin.backend.common.checkers.context.*
import org.jetbrains.kotlin.backend.common.checkers.declaration.*
import org.jetbrains.kotlin.backend.common.checkers.expression.*
import org.jetbrains.kotlin.backend.common.checkers.symbol.IrSymbolChecker
import org.jetbrains.kotlin.backend.common.checkers.symbol.IrVisibilityChecker
import org.jetbrains.kotlin.backend.common.checkers.symbol.check
import org.jetbrains.kotlin.backend.common.checkers.type.IrTypeChecker
import org.jetbrains.kotlin.backend.common.checkers.type.IrTypeParameterScopeChecker
import org.jetbrains.kotlin.backend.common.checkers.type.check
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.util.IrTreeSymbolsVisitor
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

typealias ReportIrValidationError = (IrFile?, IrElement, String, List<IrElement>) -> Unit

data class IrValidatorConfig(
    val checkTreeConsistency: Boolean = true,
    val checkTypes: Boolean = false,
    val checkProperties: Boolean = false,
    val checkValueScopes: Boolean = false,
    val checkTypeParameterScopes: Boolean = false,
    val checkCrossFileFieldUsage: Boolean = false,
    val checkAllKotlinFieldsArePrivate: Boolean = false,
    val checkVisibilities: Boolean = false,
    val checkVarargTypes: Boolean = false,
    val checkFunctionBody: Boolean = true,
    val checkUnboundSymbols: Boolean = false,
    val checkInlineFunctionUseSites: InlineFunctionUseSiteChecker? = null,
)

fun interface InlineFunctionUseSiteChecker {
    /**
     * Check if the given use site of the inline function is permitted at the current phase of IR validation.
     *
     * Example 1: Check use sites after inlining all private functions.
     *   It is permitted to have only use sites of non-private functions in the whole IR tree. So, for a use site
     *   of a private inline function we should return `false` if it is met in the IR. For any other use site
     *   we should return `true` (== permitted).
     *
     * Example 2: Check use sites after inlining all functions.
     *   Normally, no use sites of inline functions should remain in the whole IR tree. So, if we met one we shall
     *   return `false` (== not permitted). However, there are a few exceptions that are temporarily permitted.
     *   For example, `inline external` intrinsics in Native (KT-66734).
     */
    fun isPermitted(inlineFunctionUseSite: IrFunctionAccessExpression): Boolean
}

private class IrValidator(
    val validatorConfig: IrValidatorConfig,
    val irBuiltIns: IrBuiltIns,
    val reportError: ReportIrValidationError,
) : IrVisitorVoid() {
    override fun visitElement(element: IrElement) =
        throw IllegalStateException("IR validation must start from files, modules, or declarations")

    override fun visitFile(declaration: IrFile) {
        val context = CheckerContext(irBuiltIns, validatorConfig.checkInlineFunctionUseSites, declaration, reportError)
        val fileValidator = IrFileValidator(validatorConfig, context)
        declaration.acceptVoid(fileValidator)
    }

    override fun visitModuleFragment(declaration: IrModuleFragment) = declaration.acceptChildrenVoid(this)

    override fun visitDeclaration(declaration: IrDeclarationBase) {
        val context = CheckerContext(irBuiltIns, validatorConfig.checkInlineFunctionUseSites, declaration.file, reportError)
        val fileValidator = IrFileValidator(validatorConfig, context)
        declaration.acceptVoid(fileValidator)
    }
}

private class IrFileValidator(
    config: IrValidatorConfig,
    private val context: CheckerContext
) : IrTreeSymbolsVisitor() {
    private val contextUpdaters: MutableList<ContextUpdater> = mutableListOf(ParentChainUpdater)
    private val elementCheckers: MutableList<IrElementChecker<*>> = mutableListOf(
        IrNoInlineUseSitesChecker, IrFunctionReferenceFunctionDispatchReceiverChecker, IrSetValueAssignabilityChecker,
        IrFunctionDispatchReceiverChecker, IrFunctionParametersChecker, IrConstructorReceiverChecker,
        IrPrivateDeclarationOverrideChecker, IrExpressionTypeChecker, IrTypeOperatorTypeOperandChecker,
        // TODO: Why don't we check parameters as well?
        IrCallFunctionDispatchReceiverChecker,
    )
    private val symbolCheckers: MutableList<IrSymbolChecker> = mutableListOf()
    private val typeCheckers: MutableList<IrTypeChecker> = mutableListOf()

    init {
        if (config.checkValueScopes) {
            contextUpdaters.add(ValueScopeUpdater)
            elementCheckers.add(IrValueAccessScopeChecker)
        }
        if (config.checkTypeParameterScopes) {
            contextUpdaters.add(TypeParameterScopeUpdater)
            typeCheckers.add(IrTypeParameterScopeChecker)
        }
        if (config.checkAllKotlinFieldsArePrivate) {
            elementCheckers.add(IrFieldVisibilityChecker)
        }
        if (config.checkCrossFileFieldUsage) {
            elementCheckers.add(IrCrossFileFieldUsageChecker)
        }
        if (config.checkVisibilities) {
            symbolCheckers.add(IrVisibilityChecker)
        }
        if (config.checkVarargTypes) {
            elementCheckers.add(IrVarargTypesChecker)
            elementCheckers.add(IrValueParameterVarargTypesChecker)
        }
        if (config.checkTypes) {
            elementCheckers.add(IrConstTypeChecker)
            elementCheckers.add(IrStringConcatenationTypeChecker)
            elementCheckers.add(IrGetObjectValueTypeChecker)
            elementCheckers.add(IrGetValueTypeChecker)
            elementCheckers.add(IrUnitTypeExpressionChecker)
            elementCheckers.add(IrNothingTypeExpressionChecker)
            elementCheckers.add(IrGetFieldTypeChecker)
            elementCheckers.add(IrCallTypeChecker)
            elementCheckers.add(IrTypeOperatorTypeChecker)
            elementCheckers.add(IrDynamicTypeFieldAccessChecker)
        }
        if (config.checkProperties) {
            elementCheckers.add(IrCallFunctionPropertiesChecker)
            elementCheckers.add(IrFunctionPropertiesChecker)
            elementCheckers.add(IrFunctionReferenceFunctionPropertiesChecker)
            elementCheckers.add(IrPropertyAccessorsChecker)
        }
        if (config.checkFunctionBody) {
            elementCheckers.add(IrFunctionBodyChecker)
        }
    }

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

private fun IrElement.checkTreeConsistency(reportError: ReportIrValidationError, config: IrValidatorConfig) {
    val checker = CheckTreeConsistencyVisitor(reportError, config)
    accept(checker, null)
    if (checker.hasInconsistency) throw TreeConsistencyError(this)
}

private class CheckTreeConsistencyVisitor(val reportError: ReportIrValidationError, val config: IrValidatorConfig) :
    IrTreeSymbolsVisitor() {
    var hasInconsistency = false

    private val visitedElements = hashSetOf<IrElement>()
    private val parentChain: MutableList<IrElement> = mutableListOf()
    private var currentActualParent: IrDeclarationParent? = null

    override fun visitElement(element: IrElement) {
        checkDuplicateNode(element)
        parentChain.temporarilyPushing(element) {
            element.acceptChildrenVoid(this)
        }
    }

    override fun visitTypeRecursively(container: IrElement, type: IrType) {
        // Skip `type.annotations` to avoid visiting the same annotation nodes multiple times,
        // since `IrType` instances can be shared across the IR tree and are not guaranteed to be unique.
        visitType(container, type)
        if (type is IrSimpleType) {
            type.arguments.forEach {
                if (it is IrTypeProjection) {
                    visitTypeRecursively(container, it.type)
                }
            }
        }
    }

    override fun visitDeclaration(declaration: IrDeclarationBase) {
        checkDuplicateNode(declaration)
        parentChain.temporarilyPushing(declaration) {
            handleParent(declaration, currentActualParent)
            val previousActualParent = currentActualParent
            currentActualParent = declaration as? IrDeclarationParent ?: currentActualParent
            declaration.acceptChildrenVoid(this)
            currentActualParent = previousActualParent
        }
    }

    override fun visitPackageFragment(declaration: IrPackageFragment) {
        currentActualParent = declaration
        visitElement(declaration)
    }

    override fun visitSymbol(container: IrElement, symbol: IrSymbol) {
        if (config.checkUnboundSymbols && !symbol.isBound) {
            hasInconsistency = true
            reportError(null, container, "Unexpected unbound symbol", parentChain)
        }
    }

    private fun handleParent(declaration: IrDeclaration, actualParent: IrDeclarationParent?) {
        if (actualParent == null) return
        try {
            val assignedParent = declaration.parent
            if (assignedParent != actualParent) {
                reportWrongParent(declaration, assignedParent, actualParent)
            }
        } catch (_: Exception) {
            reportWrongParent(declaration, null, actualParent)
        }
    }

    private fun reportWrongParent(declaration: IrDeclaration, expectedParent: IrDeclarationParent?, actualParent: IrDeclarationParent) {
        hasInconsistency = true
        reportError(
            null,
            declaration,
            buildString {
                appendLine("Declaration with wrong parent:")
                appendLine("declaration: ${declaration.render()}")
                appendLine("expectedParent: ${expectedParent?.render()}")
                appendLine("actualParent: ${actualParent.render()}")
            },
            parentChain,
        )
    }

    private fun checkDuplicateNode(element: IrElement) {
        if (!visitedElements.add(element)) {
            val renderString = if (element is IrTypeParameter) element.render() + " of " + element.parent.render() else element.render()
            reportError(null, element, "Duplicate IR node: $renderString", parentChain)

            // The IR tree is completely messed up if it includes one element twice. It may not be a tree at all, there may be cycles.
            // Give up early to avoid stack overflow.
            throw TreeConsistencyError(element)
        }
    }
}

open class IrValidationError(message: String? = null, cause: Throwable? = null) : IllegalStateException(message, cause)

class TreeConsistencyError(element: IrElement) : IrValidationError(element.render())

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
    if (validatorConfig.checkTreeConsistency) {
        try {
            element.checkTreeConsistency(reportError, validatorConfig)
        } catch (_: TreeConsistencyError) {
            return
        }
    }

    // Phase 2: Traverse the IR tree again to run additional checks based on the validator configuration.
    val validator = IrValidator(validatorConfig, irBuiltIns, reportError)
    element.acceptVoid(validator)
}

/**
 * [IrValidationContext] is responsible for collecting validation errors, logging them and optionally throwing [IrValidationError]
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
     * **Note:** this method does **not** throw [IrValidationError]. Use [throwValidationErrorIfNeeded] for checking for errors and throwing
     * [IrValidationError]. This gives the caller the opportunity to perform additional (for example, backend-specific) validation before
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
            throw IrValidationError()
        }
    }
}

/**
 * Logs validation errors encountered during the execution of the [runValidationRoutines] closure into [messageCollector].
 *
 * If [mode] is [IrVerificationMode.ERROR], throws [IrValidationError] after [runValidationRoutines] has finished,
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
