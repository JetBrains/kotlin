/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.name.FqName

/**
 * Service that is responsible for mutating PSI through [KtReference].
 *
 * This service is only needed by IDE to handle element renaming or change elements bound to a [KtReference].
 */
interface KtReferenceMutateService {

    /**
     * See [com.intellij.psi.PsiReference.handleElementRename].
     */
    fun handleElementRename(ktReference: KtReference, newElementName: String): PsiElement?

    /**
     * See [com.intellij.psi.PsiReference.bindToElement].
     */
    fun bindToElement(ktReference: KtReference, element: PsiElement): PsiElement

    fun bindToElement(simpleNameReference: KtSimpleNameReference, element: PsiElement, shorteningMode: KtSimpleNameReference.ShorteningMode): PsiElement

    fun bindToFqName(
        simpleNameReference: KtSimpleNameReference,
        fqName: FqName,
        shorteningMode: KtSimpleNameReference.ShorteningMode = KtSimpleNameReference.ShorteningMode.DELAYED_SHORTENING,
        targetElement: PsiElement? = null
    ): PsiElement
}
