/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.expressions

import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtOperationExpression
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf

fun KtOperationExpression.reportDeprecatedDefinitelyNotNullSyntax(
    rhs: KtTypeReference?,
    context: ExpressionTypingContext
) {
    val nextLeaf = nextLeaf()
    if (nextLeaf is LeafPsiElement && nextLeaf.elementType === KtTokens.EXCLEXCL && rhs?.typeElement is KtUserType) {
        val parent = PsiTreeUtil.findCommonParent(nextLeaf, this)
        if (parent is KtPostfixExpression && parent.operationToken === KtTokens.EXCLEXCL) {
            context.trace.report(Errors.DEPRECATED_SYNTAX_WITH_DEFINITELY_NOT_NULL.on((parent as KtPostfixExpression?)!!))
        }
    }
}
