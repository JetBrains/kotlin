/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base.service

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiMember
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
internal fun KtElement.getLightElements(): List<PsiNamedElement> {
    return when (this) {
        is KtFile -> if (isScript()) {
            listOfNotNull(script?.symbol?.asFacadePsiClass())
        } else {
            listOfNotNull(symbol.asFacadePsiClass())
        }
        is KtScript -> listOfNotNull(symbol.asFacadePsiClass())
        is KtDeclaration -> symbol.getLightElementsFromDeclaration()
        else -> emptyList()
    }
}

context(_: KaSession)
internal fun PsiMember.getLightElementsForJavaDeclaration(): List<PsiNamedElement> {
    val symbol = when (this) {
        is PsiClass -> namedClassSymbol
        else -> callableSymbol
    }

    return symbol?.getLightElementsFromDeclaration().orEmpty()
}

context(_: KaSession)
internal fun KaSymbol.getLightElementsFromDeclaration(): List<PsiNamedElement> {
    return when (this) {
        is KaClassSymbol -> listOfNotNull(asPsiClass(), asPsiField())
        is KaEnumEntrySymbol -> listOfNotNull(asPsiField(), initializer?.asPsiClass())
        is KaFunctionSymbol -> asPsiMethods()
        is KaPropertySymbol -> {
            val accessors = getter?.asPsiMethods().orEmpty() + setter?.asPsiMethods().orEmpty()
            accessors + listOfNotNull(backingFieldSymbol?.asPsiField())
        }
        is KaTypeParameterSymbol -> asPsiTypeParameters()
        is KaParameterSymbol -> asPsiParameters()
        is KaBackingFieldSymbol -> listOfNotNull(asPsiField())
        else -> emptyList()
    }
}
