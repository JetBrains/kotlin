/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.stubs.StubUtils

internal object KtBinaryExpressionElementType : KtPlaceHolderStubElementType<KtBinaryExpression>(
    "BINARY_EXPRESSION",
    KtBinaryExpression::class.java,
) {
    override fun shouldCreateStub(node: ASTNode): Boolean {
        // Binary expressions are stubbed only in the argument position
        return StubUtils.isDeclaredInsideValueArgument(node) && super.shouldCreateStub(node)
    }
}
