/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.codeReferences

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.light.classes.symbol.annotations.ReferenceInformationProvider
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

internal class SymbolLightPsiJavaCodeReferenceElementWithReference(
    private val ktElement: PsiElement,
    private val reference: PsiReference,
    referenceInformationProvider: ReferenceInformationProvider,
) : SymbolLightPsiJavaCodeReferenceElementBase(ktElement, referenceInformationProvider),
    PsiReference by reference {

    override fun getElement(): PsiElement = ktElement

    private val nameExpression: KtSimpleNameExpression? get() = reference.element as? KtSimpleNameExpression

    override fun getReferenceName(): String? = super.getReferenceName() ?: nameExpression?.getReferencedName()
    override fun getReferenceNameElement(): PsiElement? = nameExpression?.getReferencedNameElement()
}
