/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isKFunction
import org.jetbrains.kotlin.ir.util.isKSuspendFunction
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.util.OperatorNameConventions

internal val replaceKFunctionInvokeWithFunctionInvokePhase = makeIrFilePhase<JvmBackendContext>(
    { ReplaceKFunctionInvokeWithFunctionInvoke() },
    name = "ReplaceKFunctionInvokeWithFunctionInvoke",
    description = "Replace KFunction{n}.invoke with Function{n}.invoke"
)

/**
 * This lowering replaces calls to `KFunction{n}.invoke` with `Function{n}.invoke`, and calls to `KSuspendFunction{n}.invoke`
 * with `SuspendFunction{n}.invoke`. This is needed because normally the type e.g. `kotlin.reflect.KFunction2` is mapped to
 * `kotlin.reflect.KFunction` (a real class, without arity), which doesn't have the corresponding `invoke`.
 */
private class ReplaceKFunctionInvokeWithFunctionInvoke : FileLoweringPass, IrElementTransformerVoid() {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val callee = expression.symbol.owner
        if (callee.name != OperatorNameConventions.INVOKE) return super.visitCall(expression)

        val parentClass = callee.parent as? IrClass ?: return super.visitCall(expression)
        if (!parentClass.defaultType.isKFunction() && !parentClass.defaultType.isKSuspendFunction()) {
            implicitCastKFunctionReceiverIntoFunctionIfNeeded(expression, parentClass)
            return super.visitCall(expression)
        }

        // The single overridden function of KFunction{n}.invoke must be Function{n}.invoke.
        val newCallee = callee.overriddenSymbols.single()
        return expression.run {
            IrCallImpl.fromSymbolOwner(startOffset, endOffset, type, newCallee).apply {
                copyTypeArgumentsFrom(expression)
                dispatchReceiver = expression.dispatchReceiver?.transform(this@ReplaceKFunctionInvokeWithFunctionInvoke, null)?.let {
                    val newType = newCallee.owner.parentAsClass.defaultType
                    IrTypeOperatorCallImpl(startOffset, endOffset, newType, IrTypeOperator.IMPLICIT_CAST, newType, it)
                }
                extensionReceiver = expression.extensionReceiver?.transform(this@ReplaceKFunctionInvokeWithFunctionInvoke, null)
                for (i in 0 until valueArgumentsCount) {
                    putValueArgument(i, expression.getValueArgument(i)?.transform(this@ReplaceKFunctionInvokeWithFunctionInvoke, null))
                }
            }
        }
    }

    // This method suppose to cover case when we have `Function{n}.invoke` but receiver has type of `KFunction`
    private fun implicitCastKFunctionReceiverIntoFunctionIfNeeded(expression: IrCall, parentClass: IrClass) {
        val receiver = expression.dispatchReceiver
        if (receiver != null && (receiver.type.isKFunction() || receiver.type.isKSuspendFunction())) {
            val newType = parentClass.defaultType

            expression.dispatchReceiver = IrTypeOperatorCallImpl(
                expression.startOffset, expression.endOffset, newType, IrTypeOperator.IMPLICIT_CAST, newType, receiver.transform(this, null)
            )
        }
    }
}
