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

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.bindingContextUtil.isInlineableFunctionLiteral

internal class LocalFunctionGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {

    fun generateLambda(ktLambda: KtLambdaExpression): IrStatement {
        val ktFun = ktLambda.functionLiteral
        val lambdaExpressionType = getTypeInferredByFrontendOrFail(ktLambda).toIrType()
        val loopResolver = if (context.languageVersionSettings.supportsFeature(LanguageFeature.BreakContinueInInlineLambdas)
            && isInlineableFunctionLiteral(ktLambda, context.bindingContext)
        )
            statementGenerator.bodyGenerator
        else null
        val irLambdaFunction = FunctionGenerator(context).generateLambdaFunctionDeclaration(ktFun, loopResolver)

        return IrFunctionExpressionImpl(
            ktLambda.startOffset, ktLambda.endOffset,
            lambdaExpressionType,
            irLambdaFunction,
            IrStatementOrigin.LAMBDA
        )
    }

    fun generateFunction(ktFun: KtNamedFunction): IrStatement {
        val irFun = generateFunctionDeclaration(ktFun)
        if (ktFun.name != null) return irFun

        val funExpressionType = getTypeInferredByFrontendOrFail(ktFun).toIrType()
        return IrFunctionExpressionImpl(
            ktFun.startOffset, ktFun.endOffset,
            funExpressionType,
            irFun,
            IrStatementOrigin.ANONYMOUS_FUNCTION
        )
    }

    private fun generateFunctionDeclaration(ktFun: KtNamedFunction) =
        FunctionGenerator(context).generateFunctionDeclaration(
            ktFun,
            if (context.languageVersionSettings.supportsFeature(LanguageFeature.BreakContinueInInlineLambdas)
                && isInlineableFunctionLiteral(ktFun, context.bindingContext)
            ) statementGenerator.bodyGenerator
            else null,
            IrDeclarationOrigin.LOCAL_FUNCTION
        )
}
