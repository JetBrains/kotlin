/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.stubs.KotlinValueArgumentStub

/**
 * Represents a trailing lambda argument passed outside of parentheses.
 *
 * ### Example:
 *
 * ```kotlin
 * list.forEach { println(it) }
 * //           ^_____________^
 * ```
 */
@OptIn(KtImplementationDetail::class)
class KtLambdaArgument : KtValueArgument, LambdaArgument {
    @KtImplementationDetail
    constructor(node: ASTNode) : super(node)

    @KtImplementationDetail
    constructor(stub: KotlinValueArgumentStub<KtLambdaArgument>) : super(stub, KtNodeTypes.LAMBDA_ARGUMENT)

    override fun getLambdaExpression(): KtLambdaExpression? = getArgumentExpression()?.unpackFunctionLiteral()

    companion object {
        /** A shared empty array, which can be reused to avoid unnecessary allocations. */
        @JvmField
        val EMPTY_ARRAY: Array<KtLambdaArgument> = emptyArray()
    }
}
