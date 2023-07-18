/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.backend.jvm.ir.receiverAndArgs
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.org.objectweb.asm.Label

object OrOr : IntrinsicMethod() {

    private class BooleanDisjunction(
        val arg0: IrExpression, val arg1: IrExpression, codegen: ExpressionCodegen, val data: BlockInfo
    ) : BooleanValue(codegen) {

        override fun jumpIfFalse(target: Label) {
            val stayLabel = Label()
            arg0.accept(codegen, data).coerceToBoolean().jumpIfTrue(stayLabel)
            arg1.accept(codegen, data).coerceToBoolean().jumpIfFalse(target)
            mv.visitLabel(stayLabel)
        }

        override fun jumpIfTrue(target: Label) {
            arg0.accept(codegen, data).coerceToBoolean().jumpIfTrue(target)
            arg1.accept(codegen, data).coerceToBoolean().jumpIfTrue(target)
        }

        override fun discard() {
            val end = Label()
            arg0.accept(codegen, data).coerceToBoolean().jumpIfTrue(end)
            arg1.accept(codegen, data).discard()
            mv.visitLabel(end)
        }
    }

    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue {
        val (left, right) = expression.receiverAndArgs()
        return BooleanDisjunction(left, right, codegen, data)
    }
}
