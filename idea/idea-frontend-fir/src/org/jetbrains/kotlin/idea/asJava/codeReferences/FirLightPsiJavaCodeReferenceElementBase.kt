/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.*
import com.intellij.psi.scope.PsiScopeProcessor

internal abstract class FirLightPsiJavaCodeReferenceElementBase(private val ktElement: PsiElement) :
    PsiElement by ktElement,
    PsiJavaCodeReferenceElement {

    override fun multiResolve(incompleteCode: Boolean): Array<JavaResolveResult> = emptyArray()

    override fun processVariants(processor: PsiScopeProcessor) { }

    override fun advancedResolve(incompleteCode: Boolean): JavaResolveResult =
        JavaResolveResult.EMPTY

    override fun getQualifier(): PsiElement? = null

    override fun getReferenceName(): String? = null

    override fun getReferenceNameElement(): PsiElement? = null

    override fun getParameterList(): PsiReferenceParameterList? = null

    override fun getTypeParameters(): Array<PsiType> = emptyArray()

    override fun isQualified(): Boolean = false

    override fun getQualifiedName(): String? = null
}