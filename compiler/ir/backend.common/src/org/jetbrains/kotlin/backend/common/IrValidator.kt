/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.checkers.context.*
import org.jetbrains.kotlin.backend.common.checkers.declaration.*
import org.jetbrains.kotlin.backend.common.checkers.expression.*
import org.jetbrains.kotlin.backend.common.checkers.type.IrSimpleTypeVisibilityChecker
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
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DeclarationParentsVisitor
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.IrTypeVisitorVoid
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
    fun isPermitted(inlineFunctionUseSite: IrMemberAccessExpression<IrFunctionSymbol>): Boolean
}

private class IrValidator(
    val validatorConfig: IrValidatorConfig,
    val irBuiltIns: IrBuiltIns,
    val reportError: ReportIrValidationError,
) : IrElementVisitorVoid {
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
    private val config: IrValidatorConfig,
    private val context: CheckerContext
) : IrTypeVisitorVoid() {
    private val visitedElements = hashSetOf<IrElement>()

    private val contextUpdaters: MutableList<ContextUpdater> = mutableListOf(ParentChainUpdater)

    private val fieldCheckers: MutableList<IrFieldChecker> = mutableListOf()
    private val fieldAccessExpressionCheckers: MutableList<IrFieldAccessChecker> = mutableListOf()
    private val typeCheckers: MutableList<IrTypeChecker> = mutableListOf()
    private val declarationReferenceCheckers: MutableList<IrDeclarationReferenceChecker> = mutableListOf(IrDeclarationReferenceBoundChecker)
    private val varargCheckers: MutableList<IrVarargChecker> = mutableListOf()
    private val valueParameterCheckers: MutableList<IrValueParameterChecker> = mutableListOf()
    private val valueAccessCheckers: MutableList<IrValueAccessChecker> = mutableListOf()
    private val functionAccessCheckers: MutableList<IrFunctionAccessChecker> = mutableListOf(IrNoInlineUseSitesChecker)
    private val functionReferenceCheckers: MutableList<IrFunctionReferenceChecker> =
        mutableListOf(IrFunctionReferenceFunctionDispatchReceiverChecker, IrNoInlineUseSitesChecker)
    private val constCheckers: MutableList<IrConstChecker> = mutableListOf()
    private val stringConcatenationCheckers: MutableList<IrStringConcatenationChecker> = mutableListOf()
    private val getObjectValueCheckers: MutableList<IrGetObjectValueChecker> = mutableListOf()
    private val getValueCheckers: MutableList<IrGetValueChecker> = mutableListOf()
    private val setValueCheckers: MutableList<IrSetValueChecker> = mutableListOf(IrSetValueAssignabilityChecker)
    private val getFieldCheckers: MutableList<IrGetFieldChecker> = mutableListOf()
    private val setFieldCheckers: MutableList<IrSetFieldChecker> = mutableListOf()
    private val delegatingConstructorCallCheckers: MutableList<IrDelegatingConstructorCallChecker> = mutableListOf()
    private val instanceInitializerCallCheckers: MutableList<IrInstanceInitializerCallChecker> =
        mutableListOf(IrInstanceInitializerCallBoundChecker)
    private val loopCheckers: MutableList<IrLoopChecker> = mutableListOf()
    private val breakContinueCheckers: MutableList<IrBreakContinueChecker> = mutableListOf()
    private val returnCheckers: MutableList<IrReturnChecker> = mutableListOf(IrReturnBoundChecker)
    private val throwCheckers: MutableList<IrThrowChecker> = mutableListOf()
    private val functionCheckers: MutableList<IrFunctionChecker> =
        mutableListOf(IrFunctionDispatchReceiverChecker, IrFunctionParametersChecker)
    private val declarationBaseCheckers: MutableList<IrDeclarationChecker<IrDeclaration>> =
        mutableListOf(IrPrivateDeclarationOverrideChecker)
    private val propertyReferenceCheckers: MutableList<IrPropertyReferenceChecker> = mutableListOf(IrPropertyReferenceBoundChecker)
    private val localDelegatedPropertyReferenceCheckers: MutableList<IrLocalDelegatedPropertyReferenceChecker> =
        mutableListOf(IrLocalDelegatedPropertyReferenceBoundChecker)
    private val expressionCheckers: MutableList<IrExpressionChecker<IrExpression>> = mutableListOf(IrExpressionTypeChecker)
    private val typeOperatorCheckers: MutableList<IrTypeOperatorChecker> = mutableListOf(IrTypeOperatorTypeOperandChecker)

    // TODO: Why don't we check parameters as well?
    private val callCheckers: MutableList<IrCallChecker> = mutableListOf(IrCallFunctionDispatchReceiverChecker, IrCallBoundChecker)

    init {
        if (config.checkValueScopes) {
            contextUpdaters.add(ValueScopeUpdater)
            valueAccessCheckers.add(IrValueAccessScopeChecker)
        }
        if (config.checkTypeParameterScopes) {
            contextUpdaters.add(TypeParameterScopeUpdater)
            typeCheckers.add(IrTypeParameterScopeChecker)
        }
        if (config.checkAllKotlinFieldsArePrivate) {
            fieldCheckers.add(IrFieldVisibilityChecker)
        }
        if (config.checkCrossFileFieldUsage) {
            fieldAccessExpressionCheckers.add(IrCrossFileFieldUsageChecker)
        }
        if (config.checkVisibilities) {
            typeCheckers.add(IrSimpleTypeVisibilityChecker)
            declarationReferenceCheckers.add(IrDeclarationReferenceVisibilityChecker)
        }
        if (config.checkVarargTypes) {
            varargCheckers.add(IrVarargTypesChecker)
            valueParameterCheckers.add(IrValueParameterVarargTypesChecker)
        }
        if (config.checkTypes) {
            constCheckers.add(IrConstTypeChecker)
            stringConcatenationCheckers.add(IrStringConcatenationTypeChecker)
            getObjectValueCheckers.add(IrGetObjectValueTypeChecker)
            getValueCheckers.add(IrGetValueTypeChecker)
            setValueCheckers.add(IrUnitTypeExpressionChecker)
            getFieldCheckers.add(IrGetFieldTypeChecker)
            setFieldCheckers.add(IrUnitTypeExpressionChecker)
            callCheckers.add(IrCallTypeChecker)
            delegatingConstructorCallCheckers.add(IrUnitTypeExpressionChecker)
            instanceInitializerCallCheckers.add(IrUnitTypeExpressionChecker)
            typeOperatorCheckers.add(IrTypeOperatorTypeChecker)
            loopCheckers.add(IrUnitTypeExpressionChecker)
            breakContinueCheckers.add(IrNothingTypeExpressionChecker)
            returnCheckers.add(IrNothingTypeExpressionChecker)
            throwCheckers.add(IrNothingTypeExpressionChecker)
        }
        if (config.checkProperties) {
            callCheckers.add(IrCallFunctionPropertiesChecker)
            functionCheckers.add(IrFunctionPropertiesChecker)
            functionReferenceCheckers.add(IrFunctionReferenceFunctionPropertiesChecker)
        }
    }

    override fun visitElement(element: IrElement) {
        checkTreeConsistency(element)
        var block = { element.acceptChildrenVoid(this) }
        for (contextUpdater in contextUpdaters) {
            val currentBlock = block
            block = { contextUpdater.runInNewContext(context, element, currentBlock) }
        }
        block()
    }

    override fun visitConst(expression: IrConst) {
        super.visitConst(expression)
        constCheckers.check(expression, context)
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation) {
        super.visitStringConcatenation(expression)
        stringConcatenationCheckers.check(expression, context)
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue) {
        super.visitGetObjectValue(expression)
        getObjectValueCheckers.check(expression, context)
    }

    // TODO: visitGetEnumValue

    override fun visitGetValue(expression: IrGetValue) {
        super.visitGetValue(expression)
        getValueCheckers.check(expression, context)
    }

    override fun visitSetValue(expression: IrSetValue) {
        super.visitSetValue(expression)
        setValueCheckers.check(expression, context)
    }

    override fun visitGetField(expression: IrGetField) {
        super.visitGetField(expression)
        getFieldCheckers.check(expression, context)
    }

    override fun visitSetField(expression: IrSetField) {
        super.visitSetField(expression)
        setFieldCheckers.check(expression, context)
    }

    override fun visitCall(expression: IrCall) {
        super.visitCall(expression)
        callCheckers.check(expression, context)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        super.visitDelegatingConstructorCall(expression)
        delegatingConstructorCallCheckers.check(expression, context)
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) {
        super.visitInstanceInitializerCall(expression)
        instanceInitializerCallCheckers.check(expression, context)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall) {
        super.visitTypeOperator(expression)
        typeOperatorCheckers.check(expression, context)
    }

    override fun visitLoop(loop: IrLoop) {
        super.visitLoop(loop)
        loopCheckers.check(loop, context)
    }

    override fun visitBreakContinue(jump: IrBreakContinue) {
        super.visitBreakContinue(jump)
        breakContinueCheckers.check(jump, context)
    }

    override fun visitReturn(expression: IrReturn) {
        super.visitReturn(expression)
        returnCheckers.check(expression, context)
    }

    override fun visitThrow(expression: IrThrow) {
        super.visitThrow(expression)
        throwCheckers.check(expression, context)
    }

    override fun visitFunction(declaration: IrFunction) {
        super.visitFunction(declaration)
        functionCheckers.check(declaration, context)
    }

    override fun visitValueAccess(expression: IrValueAccessExpression) {
        super.visitValueAccess(expression)
        valueAccessCheckers.check(expression, context)
    }

    override fun visitField(declaration: IrField) {
        super.visitField(declaration)
        fieldCheckers.check(declaration, context)
    }

    override fun visitFieldAccess(expression: IrFieldAccessExpression) {
        super.visitFieldAccess(expression)
        fieldAccessExpressionCheckers.check(expression, context)
    }

    override fun visitType(container: IrElement, type: IrType) {
        typeCheckers.check(type, container, context)
    }

    override fun visitDeclarationReference(expression: IrDeclarationReference) {
        super.visitDeclarationReference(expression)
        declarationReferenceCheckers.check(expression, context)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase) {
        super.visitDeclaration(declaration)
        declarationBaseCheckers.check(declaration, context)
    }

    override fun visitPropertyReference(expression: IrPropertyReference) {
        super.visitPropertyReference(expression)
        propertyReferenceCheckers.check(expression, context)
    }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference) {
        super.visitLocalDelegatedPropertyReference(expression)
        localDelegatedPropertyReferenceCheckers.check(expression, context)
    }

    override fun visitExpression(expression: IrExpression) {
        super.visitExpression(expression)
        expressionCheckers.check(expression, context)
    }

    override fun visitVararg(expression: IrVararg) {
        super.visitVararg(expression)
        varargCheckers.check(expression, context)
    }

    override fun visitValueParameter(declaration: IrValueParameter) {
        super.visitValueParameter(declaration)
        valueParameterCheckers.check(declaration, context)
    }

    override fun visitFunctionReference(expression: IrFunctionReference) {
        super.visitFunctionReference(expression)
        functionReferenceCheckers.check(expression, context)
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
        super.visitFunctionAccess(expression)
        functionAccessCheckers.check(expression, context)
    }

    private fun checkTreeConsistency(element: IrElement) {
        if (config.checkTreeConsistency && !visitedElements.add(element)) {
            val renderString = if (element is IrTypeParameter) element.render() + " of " + element.parent.render() else element.render()
            context.error(element, "Duplicate IR node: $renderString")

            // The IR tree is completely messed up if it includes one element twice. It may not be a tree at all, there may be cycles.
            // Give up early to avoid stack overflow.
            throw DuplicateIrNodeError(element)
        }
    }
}

private fun IrElement.checkDeclarationParents(reportError: ReportIrValidationError) {
    val checker = CheckDeclarationParentsVisitor()
    accept(checker, null)
    if (checker.errors.isNotEmpty()) {
        val expectedParents = LinkedHashSet<IrDeclarationParent>()
        reportError(
            null,
            this,
            buildString {
                append("Declarations with wrong parent: ")
                append(checker.errors.size)
                append("\n")
                checker.errors.forEach {
                    append("declaration: ")
                    append(it.declaration.render())
                    append("\nexpectedParent: ")
                    append(it.expectedParent.render())
                    append("\nactualParent: ")
                    append(it.actualParent?.render())
                }
                append("\nExpected parents:\n")
                expectedParents.forEach {
                    append(it.dump())
                }
            },
            emptyList(),
        )
    }
}

private class CheckDeclarationParentsVisitor : DeclarationParentsVisitor() {
    class Error(val declaration: IrDeclaration, val expectedParent: IrDeclarationParent, val actualParent: IrDeclarationParent?)

    val errors = ArrayList<Error>()

    override fun handleParent(declaration: IrDeclaration, actualParent: IrDeclarationParent) {
        try {
            val assignedParent = declaration.parent
            if (assignedParent != actualParent) {
                errors.add(Error(declaration, assignedParent, actualParent))
            }
        } catch (e: Exception) {
            errors.add(Error(declaration, actualParent, null))
        }
    }
}

open class IrValidationError(message: String? = null, cause: Throwable? = null) : IllegalStateException(message, cause)

class DuplicateIrNodeError(element: IrElement) : IrValidationError(element.render())

/**
 * Verifies common IR invariants that should hold in all the backends.
 */
private fun performBasicIrValidation(
    element: IrElement,
    irBuiltIns: IrBuiltIns,
    validatorConfig: IrValidatorConfig,
    reportError: ReportIrValidationError,
) {
    val validator = IrValidator(validatorConfig, irBuiltIns, reportError)
    try {
        element.acceptVoid(validator)
    } catch (e: DuplicateIrNodeError) {
        // Performing other checks may cause e.g. infinite recursion.
        return
    }
    if (validatorConfig.checkTreeConsistency) {
        element.checkDeclarationParents(reportError)
    }
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
