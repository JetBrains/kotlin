/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.stubs.StubUtils

internal object KtParenthesizedExpressionElementType : KtPlaceHolderStubElementType<KtParenthesizedExpression>(
    "PARENTHESIZED",
    KtParenthesizedExpression::class.java,
) {
    override fun shouldCreateStub(node: ASTNode): Boolean {
        // Parenthesized expressions are stubbed only in the argument position
        return StubUtils.isDeclaredInsideValueArgument(node) && super.shouldCreateStub(node)
    }
}
