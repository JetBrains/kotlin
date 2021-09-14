/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.org.objectweb.asm.Type

class PostfixIinc(private val delta: Int) : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue? {
        val argument = expression.getValueArgument(0) as? IrGetValue
            ?: error("IrGetValue expected: ${expression.dump()}")
        val varIndex = codegen.frameMap.getIndex(argument.symbol)
        if (varIndex == -1)
            error("Unmapped variable: ${argument.render()}")
        argument.accept(codegen, data).materialize()
        codegen.mv.iinc(varIndex, delta)
        return MaterialValue(codegen, Type.INT_TYPE, argument.type)
    }
}