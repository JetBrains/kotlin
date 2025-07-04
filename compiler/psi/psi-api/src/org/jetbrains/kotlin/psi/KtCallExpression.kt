/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtNodeTypes

open class KtCallExpression(node: ASTNode) : KtExpressionImpl(node), KtCallElement, KtReferenceExpression {
    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitCallExpression(this, data)
    }

    override fun getCalleeExpression(): KtExpression? {
        return findChildByClass(KtExpression::class.java)
    }

    override fun getValueArgumentList(): KtValueArgumentList? {
        return findChildByType(KtNodeTypes.VALUE_ARGUMENT_LIST)
    }

    override fun getTypeArgumentList(): KtTypeArgumentList? {
        return findChildByType(KtNodeTypes.TYPE_ARGUMENT_LIST)
    }

    /**
     * Normally there should be only one (or zero) function literal arguments.
     * The returned value is a list for better handling of commonly made mistake of a function taking a lambda and returning another function.
     * Most of users can simply ignore lists of more than one element.
     */
    override fun getLambdaArguments(): List<KtLambdaArgument> {
        return findChildrenByType(KtNodeTypes.LAMBDA_ARGUMENT)
    }

    override fun getValueArguments(): List<KtValueArgument> {
        val valueArgumentsInParentheses = valueArgumentList?.arguments.orEmpty()
        val functionLiteralArguments = lambdaArguments.ifEmpty {
            return valueArgumentsInParentheses
        }

        return valueArgumentsInParentheses + functionLiteralArguments
    }

    override fun getTypeArguments(): List<KtTypeProjection> = typeArgumentList?.arguments.orEmpty()
}
