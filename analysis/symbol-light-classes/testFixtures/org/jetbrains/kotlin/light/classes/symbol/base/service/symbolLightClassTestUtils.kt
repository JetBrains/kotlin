/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base.service

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.SyntaxTraverser
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.javaInterop.*
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.psi.*

internal fun getLightClassesFromFile(ktFile: KtFile): List<PsiClass> {
    val ktClasses = SyntaxTraverser.psiTraverser(ktFile).filter(KtClassOrObject::class.java).toList()
    return ktClasses.plus(ktFile).flatMap { ktElement ->
        analyze(ktElement) {
            ktElement.getLightElements()
        }
    }.filterIsInstance<PsiClass>()
}

context(_: KaSession)
internal fun KtElement.getLightElements(): List<PsiElement> {
    return when (this) {
        is KtFile -> if (isScript()) {
            listOfNotNull(script?.symbol?.asFacadePsiClass())
        } else {
            listOfNotNull(symbol.asFacadePsiClass())
        }
        is KtScript -> listOfNotNull(symbol.asFacadePsiClass())
        is KtDeclaration -> getLightElementsFromDeclaration()
        else -> emptyList()
    }
}


context(_: KaSession)
internal fun KtDeclaration.getLightElementsFromDeclaration(): List<PsiElement> {
    return buildList {
        when (val symbol = this@getLightElementsFromDeclaration.symbol) {
            is KaClassSymbol -> listOfNotNull(symbol.asPsiClass(), (symbol as? KaNamedClassSymbol)?.asPsiField())
            is KaEnumEntrySymbol -> listOfNotNull(symbol.initializer?.asPsiClass())
            is KaFunctionSymbol -> symbol.asPsiMethods()
            is KaPropertySymbol -> {
                val accessors = symbol.getter?.asPsiMethods().orEmpty() + symbol.setter?.asPsiMethods().orEmpty()
                accessors + listOfNotNull(symbol.backingFieldSymbol?.asPsiField())
            }
            is KaTypeParameterSymbol -> symbol.asPsiTypeParameters()
            is KaParameterSymbol -> symbol.asPsiParameters()
            is KaBackingFieldSymbol -> listOfNotNull(symbol.asPsiField())
            else -> null
        }?.let { addAll(it) }
    }
}
