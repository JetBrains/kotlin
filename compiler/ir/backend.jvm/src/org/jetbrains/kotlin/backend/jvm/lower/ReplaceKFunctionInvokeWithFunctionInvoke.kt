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
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isKFunction
import org.jetbrains.kotlin.ir.util.isKSuspendFunction
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
        if (callee !is IrSimpleFunction || callee.name != OperatorNameConventions.INVOKE) return super.visitCall(expression)

        val parentClass = callee.parent as? IrClass ?: return super.visitCall(expression)
        if (!parentClass.defaultType.isKFunction() && !parentClass.defaultType.isKSuspendFunction()) return super.visitCall(expression)

        // The single overridden function of KFunction{n}.invoke must be Function{n}.invoke.
        val newCallee = callee.overriddenSymbols.single()
        return expression.run {
            IrCallImpl(startOffset, endOffset, type, newCallee).apply {
                copyTypeArgumentsFrom(expression)
                dispatchReceiver = expression.dispatchReceiver?.transform(this@ReplaceKFunctionInvokeWithFunctionInvoke, null)
                extensionReceiver = expression.extensionReceiver?.transform(this@ReplaceKFunctionInvokeWithFunctionInvoke, null)
                for (i in 0 until valueArgumentsCount) {
                    putValueArgument(i, expression.getValueArgument(i)?.transform(this@ReplaceKFunctionInvokeWithFunctionInvoke, null))
                }
            }
        }
    }
}
