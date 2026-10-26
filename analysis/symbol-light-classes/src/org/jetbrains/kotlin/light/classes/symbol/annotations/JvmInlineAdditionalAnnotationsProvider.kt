/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForClassOrObject
import org.jetbrains.kotlin.name.JvmStandardClassIds

/**
 * Adds `@JvmInline` to the light class of an inline value class.
 *
 * The JVM backend adds `@JvmInline` to the class file of every inline value class (see `JvmInlineClassLowering`), so Java sees the
 * annotation even if the class doesn't declare it, e.g., for an `inline value class` or a deprecated `inline class`.
 *
 * The provider checks whether its owner belongs to an inline value class only when the annotations are requested, so creating the
 * modifier list of a light class doesn't compute [SymbolLightClassForClassOrObject.isInlineClass].
 *
 * @see SymbolLightClassForClassOrObject.isInlineClass
 */
internal object JvmInlineAdditionalAnnotationsProvider : AdditionalAnnotationsProvider {
    private val JVM_INLINE_QUALIFIER: String = JvmStandardClassIds.Annotations.JvmInline.asFqNameString()

    override fun addAllAnnotations(
        currentRawAnnotations: MutableList<in PsiAnnotation>,
        foundQualifiers: MutableSet<String>,
        owner: PsiElement,
    ) {
        // Skip the check for a declared `@JvmInline`, as it resolves a `value` class to the STATUS phase
        if (JVM_INLINE_QUALIFIER !in foundQualifiers && owner.parent.isInlineClass()) {
            addSimpleAnnotationIfMissing(
                qualifier = JVM_INLINE_QUALIFIER,
                currentRawAnnotations = currentRawAnnotations,
                foundQualifiers = foundQualifiers,
                owner = owner,
            )
        }
    }

    // Called only if the class doesn't declare `@JvmInline`, as the box checks the declared annotations first
    override fun findSpecialAnnotation(
        annotationsBox: GranularAnnotationsBox,
        qualifiedName: String,
        owner: PsiElement,
    ): PsiAnnotation? {
        return if (qualifiedName == JVM_INLINE_QUALIFIER && owner.parent.isInlineClass()) {
            SymbolLightSimpleAnnotation(JVM_INLINE_QUALIFIER, owner)
        } else {
            null
        }
    }

    override fun isSpecialQualifier(qualifiedName: String): Boolean = qualifiedName == JVM_INLINE_QUALIFIER
}

private fun PsiElement.isInlineClass(): Boolean = this is SymbolLightClassForClassOrObject && isInlineClass
