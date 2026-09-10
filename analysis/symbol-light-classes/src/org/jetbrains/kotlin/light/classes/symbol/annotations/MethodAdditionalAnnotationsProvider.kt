/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.load.java.JvmAnnotationNames

internal object MethodAdditionalAnnotationsProvider : AdditionalAnnotationsProvider {
    override fun addAllAnnotations(
        currentRawAnnotations: MutableList<in PsiAnnotation>,
        foundQualifiers: MutableSet<String>,
        owner: PsiElement,
    ) {
        val method = owner.parent
        if (method.isMethodWithOverride()) {
            addSimpleAnnotationIfMissing(JvmAnnotationNames.OVERRIDE_ANNOTATION.asString(), currentRawAnnotations, foundQualifiers, owner)
        }

        if (method.isCompatibilityBridge()) {
            addSimpleAnnotationIfMissing(JvmAnnotationNames.DEPRECATED_ANNOTATION.asString(), currentRawAnnotations, foundQualifiers, owner)
        }
    }

    override fun findSpecialAnnotation(
        annotationsBox: GranularAnnotationsBox,
        qualifiedName: String,
        owner: PsiElement,
    ): PsiAnnotation? {
        val method = owner.parent
        return when {
            method.isMethodWithOverride() -> createSimpleAnnotationIfMatches(
                qualifier = qualifiedName,
                expectedQualifier = JvmAnnotationNames.OVERRIDE_ANNOTATION.asString(),
                owner = owner,
            )

            method.isCompatibilityBridge() -> createSimpleAnnotationIfMatches(
                qualifier = qualifiedName,
                expectedQualifier = JvmAnnotationNames.DEPRECATED_ANNOTATION.asString(),
                owner = owner,
            )

            else -> null
        }
    }

    override fun isSpecialQualifier(qualifiedName: String): Boolean = false
}

/**
 * A static method never overrides anything, even if the Kotlin declaration it is created for does, e.g., a bridge in `DefaultImpls`.
 */
private fun PsiElement.isMethodWithOverride(): Boolean =
    this is SymbolLightMethodBase && (isDelegated || isOverride()) && !hasModifierProperty(PsiModifier.STATIC)

/**
 * The JVM backend annotates a compatibility bridge in `DefaultImpls` with `java.lang.Deprecated`.
 */
private fun PsiElement.isCompatibilityBridge(): Boolean = this is SymbolLightMethodBase && isCompatibilityBridge
