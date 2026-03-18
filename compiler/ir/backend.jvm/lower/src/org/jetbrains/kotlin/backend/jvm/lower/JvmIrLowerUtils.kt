/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.jvm.ir.JvmIrBuilder
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.isFunctionOrKFunction
import org.jetbrains.kotlin.ir.util.isSuspendFunctionOrKFunction
import org.jetbrains.kotlin.ir.util.shallowCopyOrNull
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.org.objectweb.asm.Handle

internal val IrSimpleFunction.returnsResultOfStdlibCall: Boolean
    get() {
        fun IrStatement.isStdlibCall() =
            this is IrCall && symbol.owner.getPackageFragment().packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME

        return when (val body = body) {
            is IrExpressionBody -> body.expression.isStdlibCall()
            is IrBlockBody -> body.statements.singleOrNull()
                ?.let { it.isStdlibCall() || (it is IrReturn && it.value.isStdlibCall()) } == true
            is IrSyntheticBody -> false
            null -> false
        }
    }

// Criteria for delegate optimizations on the JVM.
// All cases must be reflected in isJvmOptimizableDelegate() to inform the kotlinx-serialization plugin.
internal fun IrProperty.getPropertyReferenceForOptimizableDelegatedProperty(): IrPropertyReference? {
    if (!isDelegated || isFakeOverride || backingField == null) return null

    val delegate = backingField?.initializer?.expression
    if (delegate !is IrPropertyReference ||
        getter?.returnsResultOfStdlibCall == false ||
        setter?.returnsResultOfStdlibCall == false
    ) return null

    return delegate
}

// Criteria for delegate optimizations on the JVM.
// All cases must be reflected in isJvmOptimizableDelegate() to inform the kotlinx-serialization plugin.
internal fun IrProperty.getRichPropertyReferenceForOptimizableDelegatedProperty(): IrRichPropertyReference? {
    if (!isDelegated || isFakeOverride || backingField == null) return null

    val delegate = backingField?.initializer?.expression
    if (delegate !is IrRichPropertyReference ||
        getter?.returnsResultOfStdlibCall == false ||
        setter?.returnsResultOfStdlibCall == false
    ) return null

    return delegate
}

internal fun IrProperty.getSingletonOrConstantForOptimizableDelegatedProperty(): IrExpression? {
    fun IrExpression.isInlineable(): Boolean =
        when (this) {
            is IrConst, is IrGetSingletonValue -> true
            is IrCall -> symbol.owner.run {
                parameters.none { it.kind == IrParameterKind.Regular || it.kind == IrParameterKind.Context }
                        && arguments.all { it == null || it.isInlineable() }
                        && modality == Modality.FINAL
                        && origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                        && ((body?.statements?.singleOrNull() as? IrReturn)?.value as? IrGetField)?.symbol?.owner?.isFinal == true
            }
            is IrGetValue ->
                symbol.owner.origin == IrDeclarationOrigin.INSTANCE_RECEIVER
            else -> false
        }

    if (!isDelegated || isFakeOverride || backingField == null) return null
    return backingField?.initializer?.expression?.takeIf { it.isInlineable() }
}

/** Returns true if a delegate is optimizable on the JVM, omitting a `$delegate` auxiliary property */
fun IrProperty.isJvmOptimizableDelegate(): Boolean =
    isDelegated && !isFakeOverride && backingField != null && // fast path
            (getPropertyReferenceForOptimizableDelegatedProperty() != null || getSingletonOrConstantForOptimizableDelegatedProperty() != null)

internal val IrRichPropertyReference.constInitializer: IrExpression?
    get() {
        val symbol = reflectionTargetSymbol ?: return null
        val property = symbol.owner as? IrProperty ?: return null
        if (!property.isConst) return null
        val constPropertyField = property.backingField
        return constPropertyField?.initializer?.expression?.shallowCopyOrNull()
    }

internal fun JvmIrBuilder.jvmMethodHandle(handle: Handle): IrCall =
    irCall(backendContext.symbols.jvmMethodHandle).apply {
        arguments[0] = irInt(handle.tag)
        arguments[1] = irString(handle.owner)
        arguments[2] = irString(handle.name)
        arguments[3] = irString(handle.desc)
        arguments[4] = irBoolean(handle.isInterface)
    }

internal val IrRichPropertyReference.singleBoundValueOrNull: IrExpression?
    get() = when (boundValues.size) {
        0 -> return null
        1 -> boundValues.first()
        else -> error("Property reference can not have more than one bound value, but got: ${boundValues.size}")
    }

internal fun IrRichFunctionReference.isSamConversion(): Boolean =
    !type.isFunctionOrKFunction() && !type.isSuspendFunctionOrKFunction()