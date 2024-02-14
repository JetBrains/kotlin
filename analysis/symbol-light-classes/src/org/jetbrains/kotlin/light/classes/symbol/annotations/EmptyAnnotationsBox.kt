/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement

internal object EmptyAnnotationsBox : AnnotationsBox {
    override fun annotationsArray(owner: PsiElement): Array<PsiAnnotation> = PsiAnnotation.EMPTY_ARRAY
    override fun findAnnotation(owner: PsiElement, qualifiedName: String): PsiAnnotation? = null
}
