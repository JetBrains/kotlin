/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.checkers.IrElementChecker
import org.jetbrains.kotlin.backend.common.checkers.IrTreeInconsistencyException
import org.jetbrains.kotlin.backend.common.checkers.checkTreeConsistency
import org.jetbrains.kotlin.backend.common.checkers.context.*
import org.jetbrains.kotlin.backend.common.checkers.declaration.*
import org.jetbrains.kotlin.backend.common.checkers.expression.*
import org.jetbrains.kotlin.backend.common.checkers.symbol.IrSymbolChecker
import org.jetbrains.kotlin.backend.common.checkers.symbol.IrVisibilityChecker
import org.jetbrains.kotlin.backend.common.checkers.type.IrTypeChecker
import org.jetbrains.kotlin.backend.common.checkers.type.IrTypeParameterScopeChecker
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrTreeSymbolsVisitor
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class IrValidationError(
    val element: IrElement,
    val file: IrFile?,
    val cause: Cause,
    val message: String,
    val parentChain: List<IrElement>
) {
    interface Cause {
        object IrTreeInconsistency : Cause
        object IrUnboundSymbol : Cause
    }
}

open class IrValidationException(message: String? = null, cause: Throwable? = null) : IllegalStateException(message, cause)

data class IrValidatorConfig(
    val contextUpdaters: List<ContextUpdater> = emptyList(),
    val elementCheckers: List<IrElementChecker<*>> = emptyList(),
    val symbolCheckers: List<IrSymbolChecker> = emptyList(),
    val typeCheckers: List<IrTypeChecker> = emptyList(),
    val checkTreeConsistency: Boolean = true,
    val checkUnboundSymbols: Boolean = false,
    val checkInlineFunctionUseSites: InlineFunctionUseSiteChecker? = null,
) {
    fun withCommonCheckers(
        checkTypes: Boolean = false,
        checkProperties: Boolean = false,
        checkValueScopes: Boolean = false,
        checkTypeParameterScopes: Boolean = false,
        checkCrossFileFieldUsage: Boolean = false,
        checkAllKotlinFieldsArePrivate: Boolean = false,
        checkVisibilities: Boolean = false,
        checkVarargTypes: Boolean = false,
        checkFunctionBody: Boolean = true,
    ): IrValidatorConfig {
        val contextUpdaters = contextUpdaters.toMutableList()
        val elementCheckers = elementCheckers.toMutableList()
        val symbolCheckers = symbolCheckers.toMutableList()
        val typeCheckers = typeCheckers.toMutableList()

        elementCheckers += listOf(
            IrNoInlineUseSitesChecker, IrFunctionReferenceFunctionDispatchReceiverChecker, IrSetValueAssignabilityChecker,
            IrFunctionDispatchReceiverChecker, IrFunctionParametersChecker, IrConstructorReceiverChecker,
            IrPrivateDeclarationOverrideChecker, IrExpressionTypeChecker, IrTypeOperatorTypeOperandChecker,
            // TODO: Why don't we check parameters as well?
            IrCallFunctionDispatchReceiverChecker
        )
        contextUpdaters += ParentChainUpdater

        if (checkValueScopes) {
            contextUpdaters.add(ValueScopeUpdater)
            elementCheckers.add(IrValueAccessScopeChecker)
        }
        if (checkTypeParameterScopes) {
            contextUpdaters.add(TypeParameterScopeUpdater)
            typeCheckers.add(IrTypeParameterScopeChecker)
        }
        if (checkAllKotlinFieldsArePrivate) {
            elementCheckers.add(IrFieldVisibilityChecker)
        }
        if (checkCrossFileFieldUsage) {
            elementCheckers.add(IrCrossFileFieldUsageChecker)
        }
        if (checkVisibilities) {
            symbolCheckers.add(IrVisibilityChecker)
        }
        if (checkVarargTypes) {
            elementCheckers.add(IrVarargTypesChecker)
            elementCheckers.add(IrValueParameterVarargTypesChecker)
        }
        if (checkTypes) {
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
        if (checkProperties) {
            elementCheckers.add(IrCallFunctionPropertiesChecker)
            elementCheckers.add(IrFunctionPropertiesChecker)
            elementCheckers.add(IrFunctionReferenceFunctionPropertiesChecker)
            elementCheckers.add(IrPropertyAccessorsChecker)
        }
        if (checkFunctionBody) {
            elementCheckers.add(IrFunctionBodyChecker)
        }

        return copy(
            contextUpdaters = contextUpdaters,
            elementCheckers = elementCheckers,
            symbolCheckers = symbolCheckers,
            typeCheckers = typeCheckers,
        )
    }
}

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
    val reportError: (IrValidationError) -> Unit,
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
    private val config: IrValidatorConfig,
    private val context: CheckerContext
) : IrTreeSymbolsVisitor() {

    private val checkersPerElement = object : ClassValue<List<IrElementChecker<*>>>() {
        override fun computeValue(type: Class<*>): List<IrElementChecker<*>> =
            config.elementCheckers.filter { it.elementClass.isAssignableFrom(type) }
    }

    override fun visitElement(element: IrElement) {
        var block = { element.acceptChildrenVoid(this) }
        for (contextUpdater in config.contextUpdaters) {
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
        for (checker in config.symbolCheckers) {
            checker.check(symbol, container, context)
        }
    }

    override fun visitType(container: IrElement, type: IrType) {
        super.visitType(container, type)
        for (checker in config.typeCheckers) {
            checker.check(type, container, context)
        }
    }
}

/**
 * Verifies common IR invariants that should hold in all the backends.
 */
fun validateIr(
    element: IrElement,
    irBuiltIns: IrBuiltIns,
    validatorConfig: IrValidatorConfig,
    reportError: (IrValidationError) -> Unit,
) {
    // Phase 1: Traverse the IR tree to check for structural consistency.
    // If any issues are detected, validation stops here to avoid problems like infinite recursion during the next phase.
    if (validatorConfig.checkTreeConsistency) {
        try {
            element.checkTreeConsistency(reportError, validatorConfig)
        } catch (_: IrTreeInconsistencyException) {
            return
        }
    }

    // Phase 2: Traverse the IR tree again to run additional checks based on the validator configuration.
    val validator = IrValidator(validatorConfig, irBuiltIns, reportError)
    element.acceptVoid(validator)
}

/**
 * Logs validation errors encountered during the execution of the [runValidationRoutines] closure into [messageCollector].
 *
 * If [mode] is [IrVerificationMode.ERROR], throws [IrValidationException] after [runValidationRoutines] has finished,
 * thus allowing to collect as many errors as possible instead of aborting after the first one.
 */
fun validateIr(
    element: IrElement,
    irBuiltIns: IrBuiltIns,
    validatorConfig: IrValidatorConfig,
    messageCollector: MessageCollector,
    mode: IrVerificationMode,
    phaseName: String? = null,
    customMessagePrefix: String? = null,
    throwIfAnyError: Boolean = true,
): Boolean {
    val severity = when (mode) {
        IrVerificationMode.NONE -> return false
        IrVerificationMode.WARNING -> CompilerMessageSeverity.WARNING
        IrVerificationMode.ERROR -> CompilerMessageSeverity.ERROR
    }
    var hasAnyErrors = false
    validateIr(element, irBuiltIns, validatorConfig) { validation ->
        hasAnyErrors = true
        val phaseMessage = if (!phaseName.isNullOrEmpty()) "$phaseName: " else ""
        messageCollector.report(validation, severity, phaseName, customMessagePrefix)
    }

    if (hasAnyErrors && throwIfAnyError) {
        throw IrValidationException()
    }

    return hasAnyErrors
}

fun MessageCollector.report(
    validation: IrValidationError,
    severity: CompilerMessageSeverity,
    phaseName: String? = null,
    customMessagePrefix: String? = null,
) {
    report(
        severity,
        validation.render(phaseName, customMessagePrefix),
        validation.file?.let(validation.element::getCompilerMessageLocation),
    )
}

fun IrValidationError.render(phaseName: String? = null, customMessagePrefix: String? = null): String = buildString {
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