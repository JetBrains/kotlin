/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.codeInsight.hint.DeclarationRangeHandler
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class KotlinClassDeclarationRangeHandler : DeclarationRangeHandler<KtClassOrObject> {
    override fun getDeclarationRange(container: KtClassOrObject): TextRange {
        val body = container.getBody() ?: return container.textRange
        val prevSibling = body.getPrevSiblingIgnoringWhitespaceAndComments()
        return TextRange(
            container.getDeclarationKeyword()?.startOffset ?: container.startOffset,
            prevSibling?.endOffset ?: body.startOffset
        )
    }
}

class KotlinFunDeclarationRangeHandler : DeclarationRangeHandler<KtDeclarationWithBody> {
    override fun getDeclarationRange(container: KtDeclarationWithBody): TextRange {
        val body = container.bodyExpression ?: return container.textRange
        val prevSibling = body.getPrevSiblingIgnoringWhitespaceAndComments()
        return TextRange(
            (container as? KtNamedFunction)?.funKeyword?.startOffset
                    ?: (container as? PsiNameIdentifierOwner)?.nameIdentifier?.startOffset
                    ?: container.modifierList?.startOffset
                    ?: container.startOffset,
            prevSibling?.endOffset ?: body.startOffset
        )
    }
}
