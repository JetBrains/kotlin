/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase

/**
 * The JVM backend generates the [compatibility delegates][SymbolLightMethodBase.isDefaultImplsCompatibilityDelegate] in `DefaultImpls`
 * with a `@java.lang.Deprecated` annotation, so it should be added manually.
 */
internal object DefaultImplsAdditionalAnnotationsProvider : AdditionalAnnotationsProvider {
    override fun addAllAnnotations(
        currentRawAnnotations: MutableList<in PsiAnnotation>,
        foundQualifiers: MutableSet<String>,
        owner: PsiElement,
    ) {
        if (owner.parent.isDefaultImplsCompatibilityDelegate()) {
            addSimpleAnnotationIfMissing(
                qualifier = CommonClassNames.JAVA_LANG_DEPRECATED,
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
    ): PsiAnnotation? {
        return if (owner.parent.isDefaultImplsCompatibilityDelegate()) {
            createSimpleAnnotationIfMatches(
                qualifier = qualifiedName,
                expectedQualifier = CommonClassNames.JAVA_LANG_DEPRECATED,
                owner = owner,
            )
        } else {
            null
        }
    }

    override fun isSpecialQualifier(qualifiedName: String): Boolean = false
}

private fun PsiElement.isDefaultImplsCompatibilityDelegate(): Boolean =
    this is SymbolLightMethodBase && isDefaultImplsCompatibilityDelegate
