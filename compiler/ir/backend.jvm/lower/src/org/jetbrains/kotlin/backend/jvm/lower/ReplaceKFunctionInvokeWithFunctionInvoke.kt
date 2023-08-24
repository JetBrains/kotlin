/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isKFunction
import org.jetbrains.kotlin.ir.util.isKSuspendFunction
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
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
private class ReplaceKFunctionInvokeWithFunctionInvoke : FileLoweringPass, IrElementVisitorVoid {
    override fun lower(irFile: IrFile) {
        irFile.acceptChildrenVoid(this)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitCall(expression: IrCall) {
        expression.acceptChildren(this, null)

        val callee = expression.symbol.owner
        if (callee.name != OperatorNameConventions.INVOKE) return

        val parentClass = callee.parent as? IrClass ?: return
        if (!parentClass.defaultType.isKFunction() && !parentClass.defaultType.isKSuspendFunction()) {
            implicitCastKFunctionReceiverIntoFunctionIfNeeded(expression, parentClass)
            return
        }

        // The single overridden function of KFunction{n}.invoke must be Function{n}.invoke.
        expression.symbol = callee.overriddenSymbols.single()
        expression.dispatchReceiver = expression.dispatchReceiver?.let {
            val newType = expression.symbol.owner.parentAsClass.defaultType
            IrTypeOperatorCallImpl(expression.startOffset, expression.endOffset, newType, IrTypeOperator.IMPLICIT_CAST, newType, it)
        }
    }

    // This method suppose to cover case when we have `Function{n}.invoke` but receiver has type of `KFunction`
    private fun implicitCastKFunctionReceiverIntoFunctionIfNeeded(expression: IrCall, parentClass: IrClass) {
        val receiver = expression.dispatchReceiver
        if (receiver != null && (receiver.type.isKFunction() || receiver.type.isKSuspendFunction())) {
            val newType = parentClass.defaultType

            expression.dispatchReceiver = IrTypeOperatorCallImpl(
                expression.startOffset, expression.endOffset, newType, IrTypeOperator.IMPLICIT_CAST, newType, receiver
            )
        }
    }
}
