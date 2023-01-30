/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.codeReferences

import com.intellij.psi.*
import com.intellij.psi.scope.PsiScopeProcessor

internal abstract class SymbolLightPsiJavaCodeReferenceElementBase(private val ktElement: PsiElement) :
    PsiElement by ktElement,
    PsiJavaCodeReferenceElement {

    override fun multiResolve(incompleteCode: Boolean): Array<JavaResolveResult> = JavaResolveResult.EMPTY_ARRAY

    override fun processVariants(processor: PsiScopeProcessor) {}

    override fun advancedResolve(incompleteCode: Boolean): JavaResolveResult = JavaResolveResult.EMPTY

    override fun getQualifier(): PsiElement? = null

    override fun getReferenceName(): String? = null

    override fun getReferenceNameElement(): PsiElement? = null

    override fun getParameterList(): PsiReferenceParameterList? = null

    override fun getTypeParameters(): Array<PsiType> = PsiType.EMPTY_ARRAY

    override fun isQualified(): Boolean = false

    override fun getQualifiedName(): String? = null
}