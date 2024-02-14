/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement

/**
 * This class is used as a proxy for [com.intellij.psi.PsiAnnotationOwner].
 *
 * [GranularAnnotationsBox] provides an ability to compute each annotation separately and in a lazy way to avoid heavy computation.
 * [ComputeAllAtOnceAnnotationsBox] provides an ability to compute all annotations once on first access.
 * [EmptyAnnotationsBox] just a box without annotations.
 *
 * @see org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightModifierList
 */
internal sealed interface AnnotationsBox {
    fun annotationsArray(owner: PsiElement): Array<PsiAnnotation>
    fun findAnnotation(owner: PsiElement, qualifiedName: String): PsiAnnotation?
    fun hasAnnotation(owner: PsiElement, qualifiedName: String): Boolean = findAnnotation(owner, qualifiedName) != null
}
