/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.codeReferences

import com.intellij.psi.*
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.kotlin.light.classes.symbol.annotations.ReferenceInformationProvider

internal sealed class SymbolLightPsiJavaCodeReferenceElementBase(
    private val ktElement: PsiElement,
    private val referenceInformationProvider: ReferenceInformationProvider,
) : PsiElement by ktElement, PsiJavaCodeReferenceElement {
    override fun multiResolve(incompleteCode: Boolean): Array<JavaResolveResult> = JavaResolveResult.EMPTY_ARRAY

    override fun processVariants(processor: PsiScopeProcessor) {}

    override fun advancedResolve(incompleteCode: Boolean): JavaResolveResult = JavaResolveResult.EMPTY

    override fun getQualifier(): PsiElement? = null

    /**
     * @see com.intellij.psi.impl.PsiImplUtil.findAnnotation
     */
    override fun getReferenceName(): String? = referenceInformationProvider.referenceName

    override fun getReferenceNameElement(): PsiElement? = null

    override fun getParameterList(): PsiReferenceParameterList? = null

    override fun getTypeParameters(): Array<PsiType> = PsiType.EMPTY_ARRAY

    override fun isQualified(): Boolean = false

    override fun getQualifiedName(): String? = null
}