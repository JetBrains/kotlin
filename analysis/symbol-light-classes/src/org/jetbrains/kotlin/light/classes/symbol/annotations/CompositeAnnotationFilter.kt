/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation

/**
 * An annotation is allowed only if every filter of [filters] allows it.
 */
internal class CompositeAnnotationFilter(private val filters: List<AnnotationFilter>) : AnnotationFilter {
    override fun isAllowed(qualifiedName: String): Boolean = filters.all { it.isAllowed(qualifiedName) }

    override fun filtered(annotations: Collection<PsiAnnotation>): Collection<PsiAnnotation> =
        filters.fold(annotations) { current, filter -> filter.filtered(current) }
}
