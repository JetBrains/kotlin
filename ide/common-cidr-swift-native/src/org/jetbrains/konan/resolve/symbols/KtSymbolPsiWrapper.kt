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
import com.jetbrains.swift.psi.SwiftSymbolDeclarator
import com.jetbrains.swift.symbols.SwiftSymbol

sealed class KtSymbolPsiWrapper(val psi: PsiElement) : FakePsiElement(), PsiNamedElement {
    override fun getParent(): PsiElement = psi.containingFile

    override fun getTextRange(): TextRange = psi.textRange
    override fun getTextOffset(): Int = psi.textOffset

    override fun getUseScope(): SearchScope {
        //todo better scope inference
        return OCSearchScope.getProjectSourcesScope(project)
    }
}

class KtOCSymbolPsiWrapper(psi: PsiElement, private val symbol: OCSymbol) : KtSymbolPsiWrapper(psi), OCSymbolDeclarator<OCSymbol> {
    override fun getSymbol(): OCSymbol = symbol
    override fun getName(): String = symbol.name
}

class KtSwiftSymbolPsiWrapper(psi: PsiElement, private val symbol: SwiftSymbol) : KtSymbolPsiWrapper(psi),
    SwiftSymbolDeclarator<SwiftSymbol> {
    override fun getSwiftSymbol(): SwiftSymbol = symbol
    override fun getName(): String = symbol.name
}
