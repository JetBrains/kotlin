/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.shallowCopyOrNull
import org.jetbrains.kotlin.ir.util.statements

internal val IrSimpleFunction.returnsResultOfStdlibCall: Boolean
    get() {
        fun IrStatement.isStdlibCall() =
            this is IrCall && symbol.owner.getPackageFragment().packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME

        return when (val body = body) {
            is IrExpressionBody -> body.expression.isStdlibCall()
            is IrBlockBody -> body.statements.singleOrNull()
                ?.let { it.isStdlibCall() || (it is IrReturn && it.value.isStdlibCall()) } == true
            else -> false
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

internal fun IrProperty.getSingletonOrConstantForOptimizableDelegatedProperty(): IrExpression? {
    fun IrExpression.isInlineable(): Boolean =
        when (this) {
            is IrConst<*>, is IrGetSingletonValue -> true
            is IrCall ->
                dispatchReceiver?.isInlineable() != false
                        && extensionReceiver?.isInlineable() != false
                        && valueArgumentsCount == 0
                        && symbol.owner.run {
                    modality == Modality.FINAL
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

internal val IrMemberAccessExpression<*>.constInitializer: IrExpression?
    get() {
        if (this !is IrPropertyReference) return null
        val constPropertyField = if (field == null) {
            symbol.owner.takeIf { it.isConst }?.backingField
        } else {
            field!!.owner.takeIf { it.isFinal && it.isStatic }
        }
        return constPropertyField?.initializer?.expression?.shallowCopyOrNull()
    }