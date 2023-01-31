/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifierList
import com.intellij.util.concurrency.AtomicFieldUpdater
import org.jetbrains.kotlin.light.classes.symbol.toArrayIfNotEmptyOrDefault

internal class SimpleAnnotationsBox(private val annotationsComputer: (PsiModifierList) -> Collection<PsiAnnotation>) : AnnotationsBox {
    @Volatile
    private var cachedAnnotations: Collection<PsiAnnotation>? = null

    private fun getOrComputeAnnotations(owner: PsiModifierList): Collection<PsiAnnotation> {
        cachedAnnotations?.let { return it }

        val nonCachedAnnotations = annotationsComputer(owner)
        fieldUpdater.compareAndSet(this, null, nonCachedAnnotations)

        return getOrComputeAnnotations(owner)
    }

    override fun annotationsArray(
        owner: PsiModifierList,
    ): Array<PsiAnnotation> = getOrComputeAnnotations(owner).toArrayIfNotEmptyOrDefault(PsiAnnotation.EMPTY_ARRAY)

    override fun findAnnotation(
        owner: PsiModifierList,
        qualifiedName: String,
    ): PsiAnnotation? = getOrComputeAnnotations(owner).find { it.qualifiedName == qualifiedName }

    companion object {
        private val fieldUpdater = AtomicFieldUpdater.forFieldOfType(SimpleAnnotationsBox::class.java, Collection::class.java)
    }
}
