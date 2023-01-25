/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifierList
import java.util.concurrent.atomic.AtomicReference

internal class SimpleAnnotationsBox(private val annotationsComputer: (PsiModifierList) -> Collection<PsiAnnotation>) : AnnotationsBox {
    private val annotations: AtomicReference<Array<PsiAnnotation>?> = AtomicReference()

    private fun getOrComputeAnnotations(owner: PsiModifierList): Array<PsiAnnotation> {
        annotations.get()?.let { return it }

        val nonCachedAnnotations = annotationsComputer(owner)
        val resultArray = if (nonCachedAnnotations.isEmpty()) PsiAnnotation.EMPTY_ARRAY else nonCachedAnnotations.toTypedArray()
        return if (annotations.compareAndSet(null, resultArray)) {
            resultArray
        } else {
            getOrComputeAnnotations(owner)
        }
    }

    override fun annotations(owner: PsiModifierList): Array<PsiAnnotation> = getOrComputeAnnotations(owner)

    override fun findAnnotation(
        owner: PsiModifierList,
        qualifiedName: String,
    ): PsiAnnotation? = getOrComputeAnnotations(owner).find { it.qualifiedName == qualifiedName }
}
