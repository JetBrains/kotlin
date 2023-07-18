/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.backend.jvm.ir.unwrapInlineLambda
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression

object SingleArgumentInlineFunctionIntrinsic : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue {
        val sourceCompiler = IrSourceCompilerForInline(codegen.state, expression, expression.symbol.owner, codegen, data)
        val argumentExpression = expression.getValueArgument(0)!!
        val inlineLambda = argumentExpression.unwrapInlineLambda()
        if (inlineLambda != null) {
            val lambdaInfo = IrExpressionLambdaImpl(codegen, inlineLambda)
            lambdaInfo.generateLambdaBody(sourceCompiler)
            codegen.context.typeToCachedSMAP[lambdaInfo.lambdaClassType] = lambdaInfo.node.classSMAP
        }

        return codegen.unitValue
    }
}
