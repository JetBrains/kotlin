/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.IrInlineIntrinsicsSupport
import org.jetbrains.kotlin.backend.jvm.ir.fileParent
import org.jetbrains.kotlin.codegen.extractUsedReifiedParameters
import org.jetbrains.kotlin.codegen.inline.generateTypeOf
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression

object TypeOf : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo) = with(codegen) {
        val type = expression.getTypeArgument(0)!!
        val support = IrInlineIntrinsicsSupport(codegen.classCodegen, expression, codegen.irFunction.fileParent)
        typeMapper.typeSystem.generateTypeOf(mv, type, support)
        codegen.propagateChildReifiedTypeParametersUsages(codegen.typeMapper.typeSystem.extractUsedReifiedParameters(type))
        expression.onStack
    }
}
