/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.IrCallableReferenceImpl
import org.jetbrains.kotlin.ir.expressions.IrOperator
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext

class LocalFunctionGenerator(val statementGenerator: StatementGenerator) : GeneratorWithScope {
    override val scope: Scope get() = statementGenerator.scope
    override val context: GeneratorContext get() = statementGenerator.context

    fun generateLambda(ktLambda: KtLambdaExpression): IrStatement {
        val ktFun = ktLambda.functionLiteral
        val lambdaExpressionType = getInferredTypeWithSmartcastsOrFail(ktLambda)
        val lambdaDescriptor = getOrFail(BindingContext.FUNCTION, ktFun)
        val irBlock = IrBlockImpl(ktLambda.startOffset, ktLambda.endOffset, lambdaExpressionType, IrOperator.LAMBDA)

        val irFun = IrFunctionImpl(ktFun.startOffset, ktFun.endOffset, IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA, lambdaDescriptor)
        irFun.body = BodyGenerator(lambdaDescriptor, statementGenerator.context).generateLambdaBody(ktFun)
        irBlock.addStatement(irFun)

        irBlock.addStatement(IrCallableReferenceImpl(ktLambda.startOffset, ktLambda.endOffset, lambdaExpressionType, lambdaDescriptor))

        return irBlock
    }

}