/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.codeReferences

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.light.classes.symbol.annotations.ReferenceInformationProvider

internal class SymbolLightPsiJavaCodeReferenceElementWithNoReference(
    private val ktElement: PsiElement,
    referenceInformationProvider: ReferenceInformationProvider,
) : SymbolLightPsiJavaCodeReferenceElementBase(ktElement, referenceInformationProvider), PsiReference {

    override fun getElement(): PsiElement = ktElement

    override fun getRangeInElement(): TextRange = ktElement.textRange

    override fun resolve(): PsiElement? = null

    override fun getCanonicalText(): String = "<no-text>"

    override fun handleElementRename(newElementName: String): PsiElement = element

    @Throws(IncorrectOperationException::class)
    override fun bindToElement(element: PsiElement): PsiElement =
        throw IncorrectOperationException("can't rename SymbolLightPsiJavaCodeReferenceElementWithNoReference")

    override fun isReferenceTo(element: PsiElement): Boolean = false

    override fun isSoft(): Boolean = false
}