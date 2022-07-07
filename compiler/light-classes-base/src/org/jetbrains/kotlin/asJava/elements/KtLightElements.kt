/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement

interface KtLightElement<out T : KtElement, out D : PsiElement> : PsiElement {
    val kotlinOrigin: T?

    /**
     * KtLightModifierList by default retrieves annotation from the relevant KtElement or from clsDelegate
     * But we have none of them for KtUltraLightAnnotationForDescriptor built upon descriptor
     * For that case, KtLightModifierList in the beginning checks `givenAnnotations` and uses them if it's not null
     * Probably, it's a bit dirty solution. But, for now it's not clear how to make it better
     */
    val givenAnnotations: List<KtLightAbstractAnnotation>? get() = null
}

interface KtLightDeclaration<out T : KtDeclaration, out D : PsiElement> : KtLightElement<T, D>, PsiNamedElement

interface KtLightFieldForSourceDeclarationSupport : PsiField {
    val kotlinOrigin: KtDeclaration?
}
