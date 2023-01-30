/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifierList
import org.jetbrains.kotlin.light.classes.symbol.toArrayIfNotEmptyOrDefault
import java.util.concurrent.atomic.AtomicReference

internal class SimpleAnnotationsBox(private val annotationsComputer: (PsiModifierList) -> Collection<PsiAnnotation>) : AnnotationsBox {
    private val cachedCollection: AtomicReference<Collection<PsiAnnotation>?> = AtomicReference()

    private fun getOrComputeAnnotations(owner: PsiModifierList): Collection<PsiAnnotation> {
        cachedCollection.get()?.let { return it }

        val nonCachedAnnotations = annotationsComputer(owner)
        cachedCollection.compareAndSet(null, nonCachedAnnotations)
        return getOrComputeAnnotations(owner)
    }

    override fun annotationsArray(
        owner: PsiModifierList,
    ): Array<PsiAnnotation> = getOrComputeAnnotations(owner).toArrayIfNotEmptyOrDefault(PsiAnnotation.EMPTY_ARRAY)

    override fun findAnnotation(
        owner: PsiModifierList,
        qualifiedName: String,
    ): PsiAnnotation? = getOrComputeAnnotations(owner).find { it.qualifiedName == qualifiedName }
}
