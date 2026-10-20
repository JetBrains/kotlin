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
     * Returns the special annotation with [qualifiedName] if this provider adds it.
     *
     * [GranularAnnotationsBox] looks up a **special** annotation, which the compiler resolves in an early phase, among the declared
     * annotations only, without computing all annotations. When no declared annotation matches, it calls this function instead.
     * A provider which adds a special annotation overrides this function, and any other provider keeps the default implementation,
     * which returns **null**.
     *
     * The returned annotation must be equal to the one which [addAllAnnotations] adds.
     *
     * @param qualifiedName the qualified name of a special annotation which no declared annotation has
     * @param owner the owner of the new annotation
     *
     * @return a new annotation with [qualifiedName] if this provider adds one, or **null** otherwise
     */
    fun findSpecialAnnotation(qualifiedName: String, owner: PsiElement): PsiAnnotation? = null

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
