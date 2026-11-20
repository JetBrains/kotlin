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
        if (owner.parent.isMethodWithOverride()) {
            addSimpleAnnotationIfMissing(JvmAnnotationNames.OVERRIDE_ANNOTATION.asString(), currentRawAnnotations, foundQualifiers, owner)
        }
    }

    override fun findSpecialAnnotation(
        annotationsBox: GranularAnnotationsBox,
        qualifiedName: String,
        owner: PsiElement,
    ): PsiAnnotation? = if (owner.parent.isMethodWithOverride())
        createSimpleAnnotationIfMatches(
            qualifier = qualifiedName,
            expectedQualifier = JvmAnnotationNames.OVERRIDE_ANNOTATION.asString(),
            owner = owner,
        )
    else
        null

    override fun isSpecialQualifier(qualifiedName: String): Boolean = false
}

/**
 * Whether this element is a light method which overrides a supertype method in Java, so it gets an `@Override` annotation.
 *
 * A light method created for an overriding or delegated Kotlin member overrides the supertype method unless it is static: a static
 * method never overrides anything, even if the declaration it is created for does.
 */
private fun PsiElement.isMethodWithOverride(): Boolean =
    this is SymbolLightMethodBase && (isDelegated || isOverride()) && !hasModifierProperty(PsiModifier.STATIC)
