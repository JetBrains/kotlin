/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import org.jetbrains.kotlin.asJava.classes.lazyPub

internal class SimpleAnnotationsBox(annotationsComputer: () -> Collection<PsiAnnotation>) : AnnotationsBox {
    private val lazyAnnotation: Array<PsiAnnotation> by lazyPub {
        val annotations = annotationsComputer()
        if (annotations.isEmpty()) PsiAnnotation.EMPTY_ARRAY else annotations.toTypedArray()
    }

    override fun getAnnotations(): Array<PsiAnnotation> = lazyAnnotation
    override fun findAnnotation(qualifiedName: String): PsiAnnotation? = lazyAnnotation.find { it.qualifiedName == qualifiedName }
}
