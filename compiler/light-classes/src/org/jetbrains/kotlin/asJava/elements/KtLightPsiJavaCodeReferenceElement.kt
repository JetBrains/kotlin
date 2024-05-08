/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.asJava.classes.lazyPub

class KtLightPsiJavaCodeReferenceElement(
    private val ktElement: PsiElement,
    reference: () -> PsiReference?,
    private val customReferenceName: String? = null,
) :
    PsiElement by ktElement,
    PsiReference by LazyPsiReferenceDelegate(ktElement, reference),
    PsiJavaCodeReferenceElement {

    override fun advancedResolve(incompleteCode: Boolean): JavaResolveResult = JavaResolveResult.EMPTY

    override fun getReferenceNameElement(): PsiElement? = null

    override fun getTypeParameters(): Array<PsiType> = emptyArray()

    override fun getReferenceName(): String? = customReferenceName

    override fun isQualified(): Boolean = false

    override fun processVariants(processor: PsiScopeProcessor) = Unit

    override fun multiResolve(incompleteCode: Boolean): Array<JavaResolveResult> = emptyArray()

    override fun getQualifiedName(): String? = null

    override fun getQualifier(): PsiElement? = null

    override fun getParameterList(): PsiReferenceParameterList? = null
}

private class LazyPsiReferenceDelegate(
    private val psiElement: PsiElement,
    referenceProvider: () -> PsiReference?
) : PsiReference {

    private val delegate by lazyPub(referenceProvider)

    override fun getElement(): PsiElement = psiElement

    override fun resolve(): PsiElement? = delegate?.resolve()

    override fun getRangeInElement(): TextRange = delegate?.rangeInElement ?: psiElement.textRange

    override fun getCanonicalText(): String = delegate?.canonicalText ?: "<no-text>"

    @Throws(IncorrectOperationException::class)
    override fun handleElementRename(newElementName: String): PsiElement = delegate?.handleElementRename(newElementName) ?: element

    @Throws(IncorrectOperationException::class)
    override fun bindToElement(element: PsiElement): PsiElement =
        delegate?.bindToElement(element) ?: throw IncorrectOperationException("can't rename LazyPsiReferenceDelegate")

    override fun isSoft(): Boolean = delegate?.isSoft ?: false

    override fun isReferenceTo(element: PsiElement): Boolean = delegate?.isReferenceTo(element) ?: false

    override fun getVariants(): Array<Any> = delegate?.variants ?: emptyArray()
}