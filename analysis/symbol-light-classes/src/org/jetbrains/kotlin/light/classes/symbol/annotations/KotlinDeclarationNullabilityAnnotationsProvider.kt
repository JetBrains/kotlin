/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtModifierListOwner

internal class KotlinDeclarationNullabilityAnnotationsProvider(
    private val declarationProvider: () -> KtModifierListOwner?,
) : AdditionalAnnotationsProvider {
    override fun addAllAnnotations(
        currentRawAnnotations: MutableList<in PsiAnnotation>,
        foundQualifiers: MutableSet<String>,
        owner: PsiElement,
    ) {
        declarationProvider()?.annotationEntries
            ?.mapNotNull(::explicitNullabilityQualifier)
            ?.forEach { qualifier ->
                addSimpleAnnotationIfMissing(qualifier, currentRawAnnotations, foundQualifiers, owner)
            }
    }

    override fun isSpecialQualifier(qualifiedName: String): Boolean = false

    override fun findSpecialAnnotation(
        annotationsBox: GranularAnnotationsBox,
        qualifiedName: String,
        owner: PsiElement,
    ): PsiAnnotation? = null

    private fun explicitNullabilityQualifier(annotationEntry: KtAnnotationEntry): String? {
        val shortName = annotationEntry.shortName?.asString() ?: return null
        return when (shortName) {
            "NotNull" -> JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION.asString()
            "Nullable" -> JvmAnnotationNames.JETBRAINS_NULLABLE_ANNOTATION.asString()
            else -> null
        }
    }
}
