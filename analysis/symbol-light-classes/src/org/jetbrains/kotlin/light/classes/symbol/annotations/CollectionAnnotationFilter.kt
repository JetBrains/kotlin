/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation

internal class CollectionAnnotationFilter(private val allowedQualifiers: Collection<String>) : AnnotationFilter {
    override fun isAllowed(qualifiedName: String): Boolean = qualifiedName in allowedQualifiers
    override fun filtered(annotations: Collection<PsiAnnotation>): Collection<PsiAnnotation> = annotations.filter { annotation ->
        annotation.qualifiedName?.let(::isAllowed) == true
    }
}
