/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.NullabilityType
import org.jetbrains.kotlin.load.java.JvmAnnotationNames

internal class NullabilityAnnotationsProvider(private val lazyNullabilityType: Lazy<NullabilityType>) : AdditionalAnnotationsProvider {
    constructor(initializer: () -> NullabilityType) : this(lazyPub(initializer))

    override fun addAllAnnotations(
        currentRawAnnotations: MutableList<in PsiAnnotation>,
        foundQualifiers: MutableSet<String>,
        owner: PsiElement
    ) {
        val qualifier = lazyNullabilityType.qualifier ?: return
        addSimpleAnnotationIfMissing(qualifier, currentRawAnnotations, foundQualifiers, owner)
    }

    override fun findSpecialAnnotation(
        annotationsBox: GranularAnnotationsBox,
        qualifiedName: String,
        owner: PsiElement,
    ): PsiAnnotation? {
        if (!qualifiedName.isNullOrNotNullQualifiedName) {
            return null
        }

        val expectedQualifier = lazyNullabilityType.qualifier ?: return null
        return createSimpleAnnotationIfMatches(qualifiedName, expectedQualifier, owner)
    }

    override fun isSpecialQualifier(qualifiedName: String): Boolean = false
}

private val String.isNullOrNotNullQualifiedName: Boolean
    get() = this == JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION.asString() ||
            this == JvmAnnotationNames.JETBRAINS_NULLABLE_ANNOTATION.asString()

private val Lazy<NullabilityType>.qualifier: String?
    get() = when (value) {
        NullabilityType.NotNull -> JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION
        NullabilityType.Nullable -> JvmAnnotationNames.JETBRAINS_NULLABLE_ANNOTATION
        else -> null
    }?.asString()