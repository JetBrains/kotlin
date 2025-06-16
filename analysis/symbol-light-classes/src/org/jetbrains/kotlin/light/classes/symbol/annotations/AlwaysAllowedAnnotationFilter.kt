/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation

/**
 * An implementation of [AnnotationFilter] that allows all annotations without any filtering.
 *
 * This filter always returns `true` for any annotation qualified name and does not modify the collection of annotations.
 */
internal object AlwaysAllowedAnnotationFilter : AnnotationFilter {
    override fun isAllowed(qualifiedName: String): Boolean = true
    override fun filtered(annotations: Collection<PsiAnnotation>): Collection<PsiAnnotation> = annotations
}
