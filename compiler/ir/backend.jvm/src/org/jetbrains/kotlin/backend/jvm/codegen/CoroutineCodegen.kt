/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.ir.allOverridden
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.backend.jvm.ir.isStaticInlineClassReplacement
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.InlineClassAbi
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.unboxInlineClass
import org.jetbrains.kotlin.backend.jvm.lower.isMultifileBridge
import org.jetbrains.kotlin.backend.jvm.lower.suspendFunctionOriginal
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.coroutines.CoroutineTransformerMethodVisitor
import org.jetbrains.kotlin.codegen.coroutines.INVOKE_SUSPEND_METHOD_NAME
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_IMPL_NAME_SUFFIX
import org.jetbrains.kotlin.codegen.coroutines.reportSuspensionPointInsideMonitor
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.MethodNode

internal fun MethodNode.acceptWithStateMachine(
    irFunction: IrFunction,
    classCodegen: ClassCodegen,
    methodVisitor: MethodVisitor,
    varsCountByType: Map<Type, Int>,
    obtainContinuationClassBuilder: () -> ClassBuilder,
) {
    val state = classCodegen.context.state
    val languageVersionSettings = state.languageVersionSettings
    assert(languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines)) { "Experimental coroutines are unsupported in JVM_IR backend" }
    val element = if (irFunction.isSuspend)
        irFunction.psiElement ?: classCodegen.irClass.psiElement
    else
        classCodegen.context.suspendLambdaToOriginalFunctionMap[classCodegen.irClass.attributeOwnerId]!!.psiElement

    val lineNumber = if (irFunction.isSuspend) {
        val irFile = irFunction.file
        if (irFunction.startOffset >= 0) {
            // if it suspend function like `suspend fun foo(...)`
            irFile.fileEntry.getLineNumber(irFunction.startOffset) + 1
        } else {
            val klass = classCodegen.irClass
            if (klass.startOffset >= 0) {
                // if it suspend lambda transformed into class `runSuspend { .... }`
                irFile.fileEntry.getLineNumber(klass.startOffset) + 1
            } else 0
        }
    } else element?.let { CodegenUtil.getLineNumberForElement(it, false) } ?: 0

    val visitor = CoroutineTransformerMethodVisitor(
        methodVisitor, access, name, desc, signature, exceptions.toTypedArray(),
        containingClassInternalName = classCodegen.type.internalName,
        obtainClassBuilderForCoroutineState = obtainContinuationClassBuilder,
        isForNamedFunction = irFunction.isSuspend,
        shouldPreserveClassInitialization = state.constructorCallNormalizationMode.shouldPreserveClassInitialization,
        disableTailCallOptimizationForFunctionReturningUnit = irFunction.isSuspend && irFunction.suspendFunctionOriginal().let {
            it.returnType.isUnit() && it.anyOfOverriddenFunctionsReturnsNonUnit()
        },
        reportSuspensionPointInsideMonitor = { reportSuspensionPointInsideMonitor(element as KtElement, state, it) },
        lineNumber = lineNumber,
        sourceFile = classCodegen.irClass.file.name, // SuspendLambda.invokeSuspend is not suspend
        needDispatchReceiver = irFunction.isSuspend && (irFunction.dispatchReceiverParameter != null
                || irFunction.origin == JvmLoweredDeclarationOrigin.SUSPEND_IMPL_STATIC_FUNCTION),
        internalNameForDispatchReceiver = classCodegen.type.internalName,
        putContinuationParameterToLvt = false,
        useOldSpilledVarTypeAnalysis = state.configuration.getBoolean(JVMConfigurationKeys.USE_OLD_SPILLED_VAR_TYPE_ANALYSIS),
        initialVarsCountByType = varsCountByType,
    )
    accept(visitor)
}

private fun IrFunction.anyOfOverriddenFunctionsReturnsNonUnit(): Boolean =
    this is IrSimpleFunction && allOverridden().any { !it.returnType.isUnit() }

internal fun IrFunction.suspendForInlineToOriginal(): IrSimpleFunction? {
    if (origin != JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE &&
        origin != JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE
    ) return null
    return parentAsClass.declarations.find {
        // The function may not be named `it.name.asString() + FOR_INLINE_SUFFIX` due to name mangling,
        // e.g., for internal declarations. We check for a function with the same `attributeOwnerId` instead.
        // This is copied in `AddContinuationLowering`.
        it is IrSimpleFunction && it.attributeOwnerId == (this as IrSimpleFunction).attributeOwnerId
    } as IrSimpleFunction?
}

internal fun IrFunction.isSuspendCapturingCrossinline(): Boolean =
    this is IrSimpleFunction && hasContinuation() && parentAsClass.declarations.any {
        it is IrSimpleFunction && it.attributeOwnerId == attributeOwnerId &&
                it.origin == JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE
    }

internal fun IrFunction.continuationClass(): IrClass? =
    (body as? IrBlockBody)?.statements?.find { it is IrClass && it.origin == JvmLoweredDeclarationOrigin.CONTINUATION_CLASS }
            as IrClass?

internal fun IrFunction.continuationParameter(): IrValueParameter? = when {
    isInvokeSuspendOfLambda() || isInvokeSuspendForInlineOfLambda() -> dispatchReceiverParameter
    else -> valueParameters.singleOrNull { it.origin == JvmLoweredDeclarationOrigin.CONTINUATION_CLASS }
}

internal fun IrFunction.isInvokeSuspendOfLambda(): Boolean =
    name.asString() == INVOKE_SUSPEND_METHOD_NAME && parentAsClass.origin == JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA

private fun IrFunction.isInvokeSuspendForInlineOfLambda(): Boolean =
    origin == JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE
            && parentAsClass.origin == JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA

internal fun IrFunction.isInvokeSuspendOfContinuation(): Boolean =
    name.asString() == INVOKE_SUSPEND_METHOD_NAME && parentAsClass.origin == JvmLoweredDeclarationOrigin.CONTINUATION_CLASS

private fun IrFunction.isInvokeOfSuspendCallableReference(): Boolean =
    isSuspend && name.asString().let { name -> name == "invoke" || name.startsWith("invoke-") }
            && parentAsClass.origin == JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL

private fun IrFunction.isBridgeToSuspendImplMethod(): Boolean =
    isSuspend && this is IrSimpleFunction && parentAsClass.functions.any {
        it.name.asString() == name.asString() + SUSPEND_IMPL_NAME_SUFFIX && it.attributeOwnerId == attributeOwnerId
    }

private fun IrFunction.isStaticInlineClassReplacementDelegatingCall(): Boolean =
    this is IrAttributeContainer && !isStaticInlineClassReplacement &&
            parentAsClass.declarations.find { it is IrAttributeContainer && it.attributeOwnerId == attributeOwnerId && it !== this }
                ?.isStaticInlineClassReplacement == true

private val BRIDGE_ORIGINS = setOf(
    IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER,
    JvmLoweredDeclarationOrigin.JVM_STATIC_WRAPPER,
    JvmLoweredDeclarationOrigin.JVM_OVERLOADS_WRAPPER,
    JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR,
    JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR_FOR_HIDDEN_CONSTRUCTOR,
    JvmLoweredDeclarationOrigin.SUPER_INTERFACE_METHOD_BRIDGE,
    IrDeclarationOrigin.BRIDGE,
    IrDeclarationOrigin.BRIDGE_SPECIAL,
    IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE,
    IrDeclarationOrigin.ADAPTER_FOR_SUSPEND_CONVERSION
)

// These functions contain a single `suspend` tail call, the value of which should be returned as is
// (i.e. if it's an unboxed inline class value, it should remain unboxed).
internal fun IrFunction.isNonBoxingSuspendDelegation(): Boolean =
    origin in BRIDGE_ORIGINS || isMultifileBridge() || isBridgeToSuspendImplMethod()

internal fun IrFunction.shouldContainSuspendMarkers(): Boolean = !isNonBoxingSuspendDelegation() &&
        // These functions also contain a single `suspend` tail call, but if it returns an unboxed inline class value,
        // the return of it should be checked for a suspension and potentially boxed to satisfy an interface.
        origin != IrDeclarationOrigin.DELEGATED_MEMBER &&
        !isInvokeSuspendOfContinuation() &&
        !isInvokeOfSuspendCallableReference() &&
        !isStaticInlineClassReplacementDelegatingCall()

internal fun IrFunction.hasContinuation(): Boolean = isInvokeSuspendOfLambda() ||
        isSuspend && shouldContainSuspendMarkers() &&
        // These are templates for the inliner; the continuation is borrowed from the caller method.
        !isEffectivelyInlineOnly() &&
        origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA &&
        origin != JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE &&
        origin != JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE

internal fun IrExpression?.isReadOfCrossinline(): Boolean = when (this) {
    is IrGetValue -> (symbol.owner as? IrValueParameter)?.isCrossinline == true
    is IrGetField -> symbol.owner.origin == LocalDeclarationsLowering.DECLARATION_ORIGIN_FIELD_FOR_CROSSINLINE_CAPTURED_VALUE
    else -> false
}

internal fun IrExpression?.isReadOfInlineLambda(): Boolean = isReadOfCrossinline() ||
        (this is IrGetValue && origin == IrStatementOrigin.VARIABLE_AS_FUNCTION && (symbol.owner as? IrValueParameter)?.isNoinline == false)

internal fun createFakeContinuation(context: JvmBackendContext): IrExpression = IrErrorExpressionImpl(
    UNDEFINED_OFFSET,
    UNDEFINED_OFFSET,
    context.ir.symbols.continuationClass.createType(true, listOf(makeTypeProjection(context.irBuiltIns.anyNType, Variance.INVARIANT))),
    "FAKE_CONTINUATION"
)

internal fun IrFunction.originalReturnTypeOfSuspendFunctionReturningUnboxedInlineClass(): IrType? {
    if (this !is IrSimpleFunction || !isSuspend) return null
    // Unlike `suspendFunctionOriginal()`, this also maps `$default` stubs to the original function.
    val original = attributeOwnerId as IrSimpleFunction
    val unboxedReturnType = InlineClassAbi.unboxType(original.returnType) ?: return null
    // 1. Can't unbox into a primitive, since suspend functions have to return a reference type.
    // 2. Force boxing if the function overrides function with different type modulo nullability ignoring type parameters
    if (unboxedReturnType.isPrimitiveType() || original.overridesReturningDifferentType(original.returnType)) return null
    return original.returnType
}

private fun IrSimpleFunction.overridesReturningDifferentType(returnType: IrType): Boolean {
    val visited = hashSetOf<IrSimpleFunction>()

    fun dfs(function: IrSimpleFunction): Boolean {
        if (!visited.add(function)) return false

        for (overridden in function.overriddenSymbols) {
            val owner = overridden.owner
            val overriddenReturnType = owner.returnType

            if (!overriddenReturnType.erasedUpperBound.isInline) return true

            if (overriddenReturnType.isNullable() &&
                overriddenReturnType.makeNotNull().unboxInlineClass().isNullable()
            ) return true

            if (overriddenReturnType.classOrNull != returnType.classOrNull) return true

            if (dfs(owner)) return true
        }
        return false
    }

    return dfs(this)
}
