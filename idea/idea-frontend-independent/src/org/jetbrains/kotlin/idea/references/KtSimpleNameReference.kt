/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.plugin.references.SimpleNameReferenceExtension
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.types.expressions.OperatorConventions

abstract class KtSimpleNameReference(expression: KtSimpleNameExpression) : KtSimpleReference<KtSimpleNameExpression>(expression) {
    override fun isReferenceTo(element: PsiElement): Boolean {
        if (!doCanBeReferenceTo(element)) return false


        @Suppress("DEPRECATION") // compatibility with 191
        for (extension in Extensions.getArea(element.project).getExtensionPoint(SimpleNameReferenceExtension.EP_NAME).extensions) {
            if (extension.isReferenceTo(this, element)) return true
        }

        return super.isReferenceTo(element)
    }

    protected abstract fun doCanBeReferenceTo(candidateTarget: PsiElement): Boolean

    override fun getRangeInElement(): TextRange {
        val element = element.getReferencedNameElement()
        val startOffset = getElement().startOffset
        return element.textRange.shiftRight(-startOffset)
    }

    override fun canRename(): Boolean {
        if (expression.getParentOfTypeAndBranch<KtWhenConditionInRange>(strict = true) { operationReference } != null) return false

        val elementType = expression.getReferencedNameElementType()
        if (elementType == KtTokens.PLUSPLUS || elementType == KtTokens.MINUSMINUS) return false

        return true
    }

   abstract override fun handleElementRename(newElementName: String): PsiElement?

    enum class ShorteningMode {
        NO_SHORTENING,
        DELAYED_SHORTENING,
        FORCED_SHORTENING
    }

    // By default reference binding is delayed
    override fun bindToElement(element: PsiElement): PsiElement =
        bindToElement(element, ShorteningMode.DELAYED_SHORTENING)

    abstract fun bindToElement(element: PsiElement, shorteningMode: ShorteningMode): PsiElement

    abstract fun bindToFqName(
        fqName: FqName,
        shorteningMode: ShorteningMode = ShorteningMode.DELAYED_SHORTENING,
        targetElement: PsiElement? = null
    ): PsiElement

    override fun getCanonicalText(): String = expression.text

    override val resolvesByNames: Collection<Name>
        get() {
            val element = element

            if (element is KtOperationReferenceExpression) {
                val tokenType = element.operationSignTokenType
                if (tokenType != null) {
                    val name = OperatorConventions.getNameForOperationSymbol(
                        tokenType, element.parent is KtUnaryExpression, element.parent is KtBinaryExpression
                    ) ?: return emptyList()
                    val counterpart = OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS[tokenType]
                    return if (counterpart != null) {
                        val counterpartName = OperatorConventions.getNameForOperationSymbol(counterpart, false, true)!!
                        listOf(name, counterpartName)
                    } else {
                        listOf(name)
                    }
                }
            }

            return listOf(element.getReferencedNameAsName())
        }

    abstract fun getImportAlias(): KtImportAlias?
}
