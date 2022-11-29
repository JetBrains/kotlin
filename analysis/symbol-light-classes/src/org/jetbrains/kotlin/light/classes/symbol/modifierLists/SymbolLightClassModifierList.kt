/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.modifierLists

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.psi.KtModifierListOwner

internal class SymbolLightClassModifierList<T : KtLightElement<KtModifierListOwner, PsiModifierListOwner>>(
    containingDeclaration: T,
    staticModifiers: Set<String> = emptySet(),
    lazyModifiers: Lazy<Set<String>>? = null,
    annotationsComputer: (PsiModifierList) -> List<PsiAnnotation>,
) : SymbolLightModifierList<T>(containingDeclaration, staticModifiers, lazyModifiers, annotationsComputer)
