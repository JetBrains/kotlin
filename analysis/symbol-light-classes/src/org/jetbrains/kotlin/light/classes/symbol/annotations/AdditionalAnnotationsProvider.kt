/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifierList

internal sealed interface AdditionalAnnotationsProvider {
    fun addAllAnnotations(currentRawAnnotations: MutableList<in PsiAnnotation>, foundQualifiers: MutableSet<String>, owner: PsiModifierList)
    fun findAdditionalAnnotation(annotationsBox: LazyAnnotationsBox, qualifiedName: String, owner: PsiModifierList): PsiAnnotation?

    fun addSimpleAnnotationIfMissing(
        qualifier: String,
        currentRawAnnotations: MutableList<in PsiAnnotation>,
        foundQualifiers: MutableSet<String>,
        owner: PsiModifierList,
    ) {
        if (!foundQualifiers.add(qualifier)) return
        currentRawAnnotations += SymbolLightSimpleAnnotation(qualifier, owner)
    }

    fun createSimpleAnnotationIfMatches(qualifier: String, expectedQualifier: String, owner: PsiModifierList): PsiAnnotation? =
        if (qualifier == expectedQualifier) SymbolLightSimpleAnnotation(expectedQualifier, owner) else null
}
