/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.name.JvmStandardClassIds

/**
 * Adds `@JvmInline` to the light class of an inline value class which doesn't declare it, e.g., an `inline value class`.
 *
 * The JVM backend annotates the class file of every inline value class with `@JvmInline` (see `JvmInlineClassLowering`), whichever
 * syntax declares the class. The provider is meant for inline value classes only, so it doesn't check its owner.
 *
 * @see org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForClassOrObject.isInlineClass
 */
internal object JvmInlineAdditionalAnnotationsProvider : AdditionalAnnotationsProvider {
    private val JVM_INLINE_QUALIFIER: String = JvmStandardClassIds.Annotations.JvmInline.asFqNameString()

    override fun addAllAnnotations(
        currentRawAnnotations: MutableList<in PsiAnnotation>,
        foundQualifiers: MutableSet<String>,
        owner: PsiElement,
    ) {
        addSimpleAnnotationIfMissing(
            qualifier = JVM_INLINE_QUALIFIER,
            currentRawAnnotations = currentRawAnnotations,
            foundQualifiers = foundQualifiers,
            owner = owner,
        )
    }

    override fun findSpecialAnnotation(
        annotationsBox: GranularAnnotationsBox,
        qualifiedName: String,
        owner: PsiElement,
    ): PsiAnnotation? = createSimpleAnnotationIfMatches(qualifier = qualifiedName, expectedQualifier = JVM_INLINE_QUALIFIER, owner = owner)

    override fun isSpecialQualifier(qualifiedName: String): Boolean = qualifiedName == JVM_INLINE_QUALIFIER
}
