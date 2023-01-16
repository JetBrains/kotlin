/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.codeReferences

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

internal class SymbolLightPsiJavaCodeReferenceElementWithReference(private val ktElement: PsiElement, reference: PsiReference) :
    SymbolLightPsiJavaCodeReferenceElementBase(ktElement),
    PsiReference by reference {

    override fun getElement(): PsiElement = ktElement
}