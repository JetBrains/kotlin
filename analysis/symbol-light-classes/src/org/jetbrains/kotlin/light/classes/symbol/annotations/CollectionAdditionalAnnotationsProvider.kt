/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement

internal class CollectionAdditionalAnnotationsProvider(
    private val additionalAnnotationQualifiers: Collection<String>,
) : AdditionalAnnotationsProvider {
    constructor(qualifiedName: String) : this(setOf(qualifiedName))

    override fun addAllAnnotations(
        currentRawAnnotations: MutableList<in PsiAnnotation>,
        foundQualifiers: MutableSet<String>,
        owner: PsiElement,
    ) {
        additionalAnnotationQualifiers.forEach { qualifiedName ->
            addSimpleAnnotationIfMissing(qualifiedName, currentRawAnnotations, foundQualifiers, owner)
        }
    }

    override fun findSpecialAnnotation(
        annotationsBox: GranularAnnotationsBox,
        qualifiedName: String,
        owner: PsiElement,
    ): PsiAnnotation? {
        if (qualifiedName !in additionalAnnotationQualifiers) return null

        return createSimpleAnnotationIfMatches(qualifiedName, qualifiedName, owner)
    }

    override fun isSpecialQualifier(qualifiedName: String): Boolean = false
}