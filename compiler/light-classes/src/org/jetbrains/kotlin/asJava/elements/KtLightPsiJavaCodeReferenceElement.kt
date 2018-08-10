/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.*
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.kotlin.asJava.classes.lazyPub

class KtLightPsiJavaCodeReferenceElement(
    private val ktElement: PsiElement,
    private val reference: PsiReference,
    private val clsDelegateProvider: () -> PsiJavaCodeReferenceElement?
) :
    PsiElement by ktElement,
    PsiReference by reference,
    PsiJavaCodeReferenceElement {

    private val delegate by lazyPub { clsDelegateProvider() }

    override fun advancedResolve(incompleteCode: Boolean): JavaResolveResult =
        delegate?.advancedResolve(incompleteCode) ?: JavaResolveResult.EMPTY

    override fun getReferenceNameElement(): PsiElement? = ktElement

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