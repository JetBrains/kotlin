/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.name.JvmStandardClassIds

/**
 * The auto-generated boxed methods in [JvmExposeBoxedMode.IMPLICIT][org.jetbrains.kotlin.light.classes.symbol.methods.JvmExposeBoxedMode.IMPLICIT] mode
 * don't have [JvmExposeBoxed] annotations by default, so they should be added manually.
 *
 * @see org.jetbrains.kotlin.light.classes.symbol.methods.JvmExposeBoxedMode
 */
internal object JvmExposeBoxedAdditionalAnnotationsProvider : AdditionalAnnotationsProvider {
    override fun addAllAnnotations(
        currentRawAnnotations: MutableList<in PsiAnnotation>,
        foundQualifiers: MutableSet<String>,
        owner: PsiElement,
    ) {
        if (owner.parent.isJvmExposeBoxed()) {
            addSimpleAnnotationIfMissing(
                qualifier = JvmStandardClassIds.JVM_EXPOSE_BOXED_ANNOTATION_FQ_NAME.asString(),
                currentRawAnnotations = currentRawAnnotations,
                foundQualifiers = foundQualifiers,
                owner = owner,
            )
        }
    }

    override fun findSpecialAnnotation(
        annotationsBox: GranularAnnotationsBox,
        qualifiedName: String,
        owner: PsiElement,
    ): PsiAnnotation? = if (owner.parent.isJvmExposeBoxed())
        createSimpleAnnotationIfMatches(
            qualifier = qualifiedName,
            expectedQualifier = JvmStandardClassIds.JVM_EXPOSE_BOXED_ANNOTATION_FQ_NAME.asString(),
            owner = owner,
        )
    else
        null

    override fun isSpecialQualifier(qualifiedName: String): Boolean = false
}

private fun PsiElement.isJvmExposeBoxed(): Boolean = this is SymbolLightMethodBase && isJvmExposedBoxed
