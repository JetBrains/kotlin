/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode

/**
 * Represents a safe-call expression using the `?.` operator.
 *
 * ### Example:
 *
 * ```kotlin
 * val len = str?.length
 * //        ^_________^
 * ```
 */
class KtSafeQualifiedExpression(node: ASTNode) : KtExpressionImpl(node), KtQualifiedExpression {
    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitSafeQualifiedExpression(this, data)
    }
}
