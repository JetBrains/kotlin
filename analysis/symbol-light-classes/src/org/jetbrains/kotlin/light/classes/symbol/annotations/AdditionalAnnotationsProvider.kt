/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement

/**
 * This class provides more annotation in addition to [AnnotationsProvider].
 *
 * [EmptyAdditionalAnnotationsProvider] is just an empty provider.
 * [CollectionAdditionalAnnotationsProvider] is a collection of additional annotations.
 * [CompositeAdditionalAnnotationsProvider] is a composition of some [AdditionalAnnotationsProvider].
 *
 * @see [GranularAnnotationsBox]
 */
internal sealed interface AdditionalAnnotationsProvider {
    /**
     * Adds new annotations to [currentRawAnnotations] and [foundQualifiers].
     * [currentRawAnnotations] and [foundQualifiers] must be consistent with each other.
     * A parent for all new annotations must be [owner].
     *
     * @param currentRawAnnotations a list of already presented annotations
     * @param foundQualifiers a list of already presented qualifiers. Used to optimize computation
     * @param owner an owner for new annotations
     */
    fun addAllAnnotations(currentRawAnnotations: MutableList<in PsiAnnotation>, foundQualifiers: MutableSet<String>, owner: PsiElement)

    /**
     * @return **true** if this provider may add an annotation with [qualifiedName] which [GranularAnnotationsBox] looks up among the
     * declared annotations only (a **special** annotation), so the box asks [findSpecialAnnotation] when no declared annotation matches
     */
    fun isSpecialQualifier(qualifiedName: String): Boolean

    /**
     * The resulted annotation must be presented among all annotations after [addAllAnnotations]
     *
     * @param annotationsBox an owner of [AdditionalAnnotationsProvider]
     * @param qualifiedName a **special** ([isSpecialQualifier] must be **true** for it) qualified name for a new annotation,
     * which no declared annotation has
     * @param owner an owner for a new annotation
     *
     * @return a new annotation with [qualifiedName]
     */
    fun findSpecialAnnotation(annotationsBox: GranularAnnotationsBox, qualifiedName: String, owner: PsiElement): PsiAnnotation?

    /**
     * Adds a new annotation with [qualifier] name to [currentRawAnnotations] and [foundQualifiers] if not already present
     */
    fun addSimpleAnnotationIfMissing(
        qualifier: String,
        currentRawAnnotations: MutableList<in PsiAnnotation>,
        foundQualifiers: MutableSet<String>,
        owner: PsiElement,
    ) {
        val isNewQualifier = foundQualifiers.add(qualifier)
        if (!isNewQualifier) return

        currentRawAnnotations += SymbolLightSimpleAnnotation(qualifier, owner)
    }

    fun createSimpleAnnotationIfMatches(
        qualifier: String,
        expectedQualifier: String,
        owner: PsiElement,
    ): PsiAnnotation? = if (qualifier == expectedQualifier) SymbolLightSimpleAnnotation(expectedQualifier, owner) else null
}
