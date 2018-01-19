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

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorCallExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.types.ErrorUtils

class ErrorExpressionGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {
    private val ignoreErrors: Boolean get() = context.configuration.ignoreErrors

    private inline fun generateErrorExpression(ktElement: KtElement, e: Exception? = null, body: () -> IrExpression) =
        if (ignoreErrors)
            body()
        else
            throw RuntimeException("${e?.message}: ${ktElement::class.java.simpleName}:\n${ktElement.text}", e)

    fun generateErrorExpression(ktElement: KtElement, e: Exception): IrExpression =
        generateErrorExpression(ktElement, e) {
            IrErrorExpressionImpl(
                ktElement.startOffset, ktElement.endOffset,
                if (ktElement is KtExpression) getErrorExpressionType(ktElement) else ErrorUtils.createErrorType(""),
                e.message ?: ""
            )
        }

    fun generateErrorCall(ktCall: KtCallExpression): IrExpression = generateErrorExpression(ktCall) {
        val type = getErrorExpressionType(ktCall)

        val irErrorCall = IrErrorCallExpressionImpl(ktCall.startOffset, ktCall.endOffset, type, "") // TODO problem description?
        irErrorCall.explicitReceiver = (ktCall.parent as? KtDotQualifiedExpression)?.let {
            statementGenerator.generateExpression(it.receiverExpression)
        }

        ktCall.valueArguments.forEach {
            val ktArgument = it.getArgumentExpression()
            if (ktArgument != null) {
                irErrorCall.addArgument(statementGenerator.generateExpression(ktArgument))
            }
        }

        ktCall.lambdaArguments.forEach {
            irErrorCall.addArgument(statementGenerator.generateExpression(it.getArgumentExpression()))
        }

        irErrorCall
    }

    private fun getErrorExpressionType(ktExpression: KtExpression) =
        getInferredTypeWithImplicitCasts(ktExpression) ?: ErrorUtils.createErrorType("")

    fun generateErrorSimpleName(ktName: KtSimpleNameExpression): IrExpression = generateErrorExpression(ktName) {
        val type = getErrorExpressionType(ktName)

        val irErrorCall = IrErrorCallExpressionImpl(ktName.startOffset, ktName.endOffset, type, "") // TODO problem description?
        irErrorCall.explicitReceiver = (ktName.parent as? KtDotQualifiedExpression)?.let { ktParent ->
            if (ktParent.receiverExpression == ktName) null
            else statementGenerator.generateExpression(ktParent.receiverExpression)
        }

        irErrorCall
    }

}