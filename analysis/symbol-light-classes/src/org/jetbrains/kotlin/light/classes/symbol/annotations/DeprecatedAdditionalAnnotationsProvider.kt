/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.load.java.JvmAnnotationNames

internal object DeprecatedAdditionalAnnotationsProvider : AdditionalAnnotationsProvider {
    override fun addAllAnnotations(
        currentRawAnnotations: MutableList<in PsiAnnotation>,
        foundQualifiers: MutableSet<String>,
        owner: PsiElement
    ) {
        if ((owner.parent as? PsiDocCommentOwner)?.isDeprecated == true) {
            addSimpleAnnotationIfMissing(JvmAnnotationNames.DEPRECATED_ANNOTATION.asString(), currentRawAnnotations, foundQualifiers, owner)
        }
    }

    override fun isSpecialQualifier(qualifiedName: String): Boolean = false

    override fun findSpecialAnnotation(
        annotationsBox: GranularAnnotationsBox,
        qualifiedName: String,
        owner: PsiElement
    ): PsiAnnotation? = if ((owner.parent as? PsiDocCommentOwner)?.isDeprecated == true)
        createSimpleAnnotationIfMatches(
            qualifier = qualifiedName,
            expectedQualifier = JvmAnnotationNames.DEPRECATED_ANNOTATION.asString(),
            owner = owner,
        )
    else
        null
}