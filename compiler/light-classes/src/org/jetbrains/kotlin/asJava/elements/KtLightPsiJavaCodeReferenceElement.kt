/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
    clsDelegateProvider: () -> PsiJavaCodeReferenceElement?
) :
    PsiElement by ktElement,
    PsiReference by LazyPsiReferenceDelegate(ktElement, reference),
    PsiJavaCodeReferenceElement {

    private val delegate by lazyPub(clsDelegateProvider)

    override fun advancedResolve(incompleteCode: Boolean): JavaResolveResult =
        delegate?.advancedResolve(incompleteCode) ?: JavaResolveResult.EMPTY

    override fun getReferenceNameElement(): PsiElement? = delegate?.referenceNameElement

    override fun getTypeParameters(): Array<PsiType> = delegate?.typeParameters ?: emptyArray()

    override fun getReferenceName(): String? = delegate?.referenceName

    override fun isQualified(): Boolean = delegate?.isQualified ?: false

    override fun processVariants(processor: PsiScopeProcessor) {
        delegate?.processVariants(processor)
    }

    override fun multiResolve(incompleteCode: Boolean): Array<JavaResolveResult> = delegate?.multiResolve(incompleteCode) ?: emptyArray()

    override fun getQualifiedName(): String? = delegate?.qualifiedName

    override fun getQualifier(): PsiElement? = delegate?.qualifier

    override fun getParameterList(): PsiReferenceParameterList? = delegate?.parameterList
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