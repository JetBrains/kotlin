/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation

/**
 * Provider a filter for resulted annotations from [GranularAnnotationsBox]
 *
 * @see GranularAnnotationsBox
 */
internal sealed interface AnnotationFilter {
    /**
     * @return **true** if an annotations with [qualifiedName] is allowed
     */
    fun isAllowed(qualifiedName: String): Boolean

    /**
     * @return a filtered collection where each annotation in a list has an allowed qualifier
     */
    fun filtered(annotations: Collection<PsiAnnotation>): Collection<PsiAnnotation>
}
