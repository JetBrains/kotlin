/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode

@OptIn(KtImplementationDetail::class, KtExperimentalApi::class)
internal class KtErrorSafeQualifiedExpressionImpl : KtExpressionImpl, KtErrorSafeQualifiedExpression {
    constructor(node: ASTNode) : super(node)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitErrorSafeQualifiedExpression(this, data)
    }
}
