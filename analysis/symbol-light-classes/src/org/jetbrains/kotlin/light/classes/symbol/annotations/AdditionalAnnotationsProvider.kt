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
     * Returns `true` if [findSpecialAnnotation] can look up an annotation with [qualifiedName].
     *
     * For such a name, [GranularAnnotationsBox.findAnnotation] and [GranularAnnotationsBox.hasAnnotation] call [findSpecialAnnotation]
     * instead of computing all annotations of the owner, which resolves the declaration further and runs every provider.
     */
    fun isSpecialQualifier(qualifiedName: String): Boolean

    /**
     * Returns the annotation of [owner] with [qualifiedName], or `null` if there is none, without computing all annotations of [owner].
     *
     * [GranularAnnotationsBox] calls this function only if [isSpecialQualifier] returns `true` for [qualifiedName]. If the box can check
     * the declared annotations for [qualifiedName] itself, as for `@JvmInline`, it does so first and calls this function only if none
     * matches. Otherwise, it returns the result as is, so this function has to take the declared annotations into account.
     *
     * The result must agree with the full list of annotations, which includes the ones from [addAllAnnotations]: return an annotation
     * if and only if the list has one with [qualifiedName].
     *
     * [annotationsBox] is the box which owns this provider. A provider can use it to look up the declared annotations.
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
