/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.FakePsiElement
import com.intellij.psi.search.SearchScope
import com.jetbrains.cidr.lang.psi.OCSymbolDeclarator
import com.jetbrains.cidr.lang.search.scopes.OCSearchScope
import com.jetbrains.cidr.lang.symbols.OCSymbol

class KotlinOCPsiWrapper(
    private val myPsi: PsiElement,
    private val mySymbol: OCSymbol
) : FakePsiElement(), PsiElement, OCSymbolDeclarator<OCSymbol>, PsiNamedElement {

    override fun getParent(): PsiElement = myPsi.containingFile
    override fun getSymbol(): OCSymbol = mySymbol

    override fun getTextRange(): TextRange = myPsi.textRange
    override fun getTextOffset(): Int = myPsi.textOffset

    val psi: PsiElement
        get() = myPsi

    override fun getName(): String {
        return mySymbol.name
    }

    override fun getUseScope(): SearchScope {
        //todo better scope inference
        return OCSearchScope.getProjectSourcesScope(project)
    }
}
