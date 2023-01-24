/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationOwner

internal sealed interface AnnotationsBox : PsiAnnotationOwner {
    override fun getApplicableAnnotations(): Array<PsiAnnotation> = annotations
    override fun addAnnotation(qualifiedName: String): PsiAnnotation = throw UnsupportedOperationException()
}
