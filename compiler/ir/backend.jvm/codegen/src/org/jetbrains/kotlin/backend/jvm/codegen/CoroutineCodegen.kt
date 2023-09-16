/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.InlineClassAbi
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.backend.jvm.ir.hasContinuation
import org.jetbrains.kotlin.backend.jvm.ir.isReadOfCrossinline
import org.jetbrains.kotlin.backend.jvm.ir.suspendFunctionOriginal
import org.jetbrains.kotlin.backend.jvm.unboxInlineClass
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.coroutines.CoroutineTransformerMethodVisitor
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.allOverridden
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmBackendErrors
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
    val context = classCodegen.context
    val languageVersionSettings = context.config.languageVersionSettings
    assert(languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines)) { "Experimental coroutines are unsupported in JVM_IR backend" }

    val lineNumber = if (irFunction.startOffset >= 0) {
        // if it suspend function like `suspend fun foo(...)`
        irFunction.file.fileEntry.getLineNumber(irFunction.startOffset) + 1
    } else {
        val klass = classCodegen.irClass
        if (klass.startOffset >= 0) {
            // if it suspend lambda transformed into class `runSuspend { .... }`
            irFunction.file.fileEntry.getLineNumber(klass.startOffset) + 1
        } else 1 // This lambda might be synthetic
    }

    val visitor = CoroutineTransformerMethodVisitor(
        methodVisitor, access, name, desc, signature, exceptions.toTypedArray(),
        containingClassInternalName = classCodegen.type.internalName,
        obtainClassBuilderForCoroutineState = obtainContinuationClassBuilder,
        isForNamedFunction = irFunction.isSuspend,
        disableTailCallOptimizationForFunctionReturningUnit = irFunction.isSuspend && irFunction.suspendFunctionOriginal().let {
            it.returnType.isUnit() && it.anyOfOverriddenFunctionsReturnsNonUnit()
        },
        reportSuspensionPointInsideMonitor = {
            classCodegen.context.ktDiagnosticReporter.at(irFunction, classCodegen.irClass)
                .report(JvmBackendErrors.SUSPENSION_POINT_INSIDE_MONITOR, it)
        },
        lineNumber = lineNumber,
        sourceFile = classCodegen.irClass.file.name, // SuspendLambda.invokeSuspend is not suspend
        needDispatchReceiver = irFunction.isSuspend && (irFunction.dispatchReceiverParameter != null
                || irFunction.origin == JvmLoweredDeclarationOrigin.SUSPEND_IMPL_STATIC_FUNCTION),
        internalNameForDispatchReceiver = classCodegen.type.internalName,
        putContinuationParameterToLvt = false,
        initialVarsCountByType = varsCountByType,
        shouldOptimiseUnusedVariables = !context.config.enableDebugMode
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

internal fun IrExpression?.isReadOfInlineLambda(): Boolean = isReadOfCrossinline() ||
        (this is IrGetValue && origin == IrStatementOrigin.VARIABLE_AS_FUNCTION && (symbol.owner as? IrValueParameter)?.isNoinline == false)

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

            if (!overriddenReturnType.erasedUpperBound.isSingleFieldValueClass) return true

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
