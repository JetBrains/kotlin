/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierList
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.load.java.JvmAnnotationNames

internal object MethodAdditionalAnnotationsProvider : AdditionalAnnotationsProvider {
    override fun addAllAnnotations(
        currentRawAnnotations: MutableList<in PsiAnnotation>,
        foundQualifiers: MutableSet<String>,
        owner: PsiModifierList,
    ) {
        if (owner.parent.isMethodWithOverride()) {
            addSimpleAnnotationIfMissing(JvmAnnotationNames.OVERRIDE_ANNOTATION.asString(), currentRawAnnotations, foundQualifiers, owner)
        }
    }

    override fun findSpecialAnnotation(
        annotationsBox: GranularAnnotationsBox,
        qualifiedName: String,
        owner: PsiModifierList,
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

private fun PsiElement.isMethodWithOverride(): Boolean = this is SymbolLightMethodBase && (isDelegated || isOverride())
