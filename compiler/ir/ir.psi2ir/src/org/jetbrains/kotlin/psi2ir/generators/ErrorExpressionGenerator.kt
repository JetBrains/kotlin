/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorCallExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils

internal class ErrorExpressionGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {
    private val ignoreErrors: Boolean get() = context.configuration.ignoreErrors

    private inline fun generateErrorExpression(ktElement: KtElement, e: Throwable? = null, body: () -> IrExpression): IrExpression =
        if (ignoreErrors)
            body()
        else
            throw ErrorExpressionException(ktElement, e)

    fun generateErrorExpression(ktElement: KtElement, e: Throwable): IrExpression =
        generateErrorExpression(ktElement, e) {
            val errorExpressionType =
                if (ktElement is KtExpression)
                    getErrorExpressionType(ktElement)
                else
                    ErrorUtils.createErrorType(ErrorTypeKind.TYPE_FOR_GENERATED_ERROR_EXPRESSION)
            IrErrorExpressionImpl(
                ktElement.startOffsetSkippingComments, ktElement.endOffset,
                errorExpressionType.toIrType(),
                e.message ?: ktElement.text
            )
        }

    fun generateErrorCall(ktCall: KtCallExpression): IrExpression = generateErrorExpression(ktCall) {
        val type = getErrorExpressionType(ktCall).toIrType()

        val irErrorCall = IrErrorCallExpressionImpl(ktCall.startOffsetSkippingComments, ktCall.endOffset, type, ktCall.text) // TODO problem description?
        irErrorCall.explicitReceiver = (ktCall.parent as? KtDotQualifiedExpression)?.run {
            receiverExpression.genExpr()
        }

        (ktCall.valueArguments + ktCall.lambdaArguments).forEach {
            val ktArgument = it.getArgumentExpression()
            if (ktArgument != null) {
                irErrorCall.arguments.add(ktArgument.genExpr())
            }
        }

        irErrorCall
    }

    private fun getErrorExpressionType(ktExpression: KtExpression) =
        getTypeInferredByFrontend(ktExpression) ?: ErrorUtils.createErrorType(ErrorTypeKind.ERROR_EXPRESSION_TYPE)

    fun generateErrorSimpleName(ktName: KtSimpleNameExpression): IrExpression = generateErrorExpression(ktName) {
        val type = getErrorExpressionType(ktName).toIrType()

        val irErrorCall = IrErrorCallExpressionImpl(ktName.startOffsetSkippingComments, ktName.endOffset, type, ktName.text) // TODO problem description?
        irErrorCall.explicitReceiver = (ktName.parent as? KtDotQualifiedExpression)?.let { ktParent ->
            if (ktParent.receiverExpression == ktName) null
            else ktParent.receiverExpression.genExpr()
        }

        irErrorCall
    }

}

class ErrorExpressionException(val ktElement: KtElement, cause: Throwable?) : RuntimeException(
    "${cause?.message}: ${ktElement::class.java.simpleName}:\n${ktElement.text}",
    cause
)
