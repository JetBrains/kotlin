/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.modifierLists

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.psi.KtModifierListOwner

internal class SymbolLightClassModifierList<T : KtLightElement<KtModifierListOwner, PsiModifierListOwner>> : SymbolLightModifierList<T> {
    constructor(
        containingDeclaration: T,
        initialValue: Map<String, Boolean> = emptyMap(),
        lazyModifiersComputer: LazyModifiersComputer,
        annotationsComputer: ((PsiModifierList) -> List<PsiAnnotation>)?,
    ) : super(containingDeclaration, initialValue, lazyModifiersComputer, annotationsComputer)

    constructor(
        containingDeclaration: T,
        staticModifiers: Set<String>,
        annotationsComputer: ((PsiModifierList) -> List<PsiAnnotation>)?,
    ) : super(containingDeclaration, staticModifiers, annotationsComputer)
}
