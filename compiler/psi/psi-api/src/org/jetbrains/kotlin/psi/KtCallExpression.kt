/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub

/**
 * Represents a function call expression, including the callee and arguments.
 *
 * ### Example:
 *
 * ```kotlin
 * fun main() {
 *     println(0)
 * //  ^________^
 * }
 * ```
 *
 * Note: this class is not intended to be extended and is marked `open` solely for backward compatibility.
 */
open class KtCallExpression : KtExpressionImplStub<KotlinPlaceHolderStub<KtCallExpression>>, KtCallElement, KtReferenceExpression {
    @KtImplementationDetail
    constructor(node: ASTNode) : super(node)

    @KtImplementationDetail
    constructor(stub: KotlinPlaceHolderStub<KtCallExpression>) : super(stub, KtNodeTypes.CALL_EXPRESSION)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitCallExpression(this, data)
    }

    override fun getCalleeExpression(): KtExpression? {
        return getStubOrPsiChild(KtNodeTypes.REFERENCE_EXPRESSION, KtNameReferenceExpression::class.java)
            ?: findChildByClass(KtExpression::class.java)
    }

    override fun getValueArgumentList(): KtValueArgumentList? {
        return getStubOrPsiChild(KtNodeTypes.VALUE_ARGUMENT_LIST, KtValueArgumentList::class.java)
    }

    override fun getTypeArgumentList(): KtTypeArgumentList? {
        return getStubOrPsiChild(KtNodeTypes.TYPE_ARGUMENT_LIST, KtTypeArgumentList::class.java)
    }

    /**
     * Returns the trailing lambda arguments of this call — lambdas passed outside the value-argument parentheses, as in `foo { ... }`.
     *
     * Normally there is only one (or zero) such argument. The return type is a list only to gracefully handle the common mistake of calling
     * a function that takes a lambda and itself returns a function, as in `foo { } { }`; most callers can simply ignore lists with more
     * than one element.
     */
    override fun getLambdaArguments(): List<KtLambdaArgument> {
        return getStubOrPsiChildren(KtNodeTypes.LAMBDA_ARGUMENT, KtLambdaArgument.EMPTY_ARRAY).asList()
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
