/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.api.applicator

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.frontend.api.ForbidKtResolve

/**
 * Provides a presentation to display an message in the editor which may be latter fixed by [HLApplicator]
 */
sealed class HLPresentation<PSI : PsiElement> {
    fun getHighlightType(element: PSI): ProblemHighlightType = ForbidKtResolve.forbidResolveIn("HLPresentation.getHighlightType") {
        getHighlightTypeImpl(element)
    }

    abstract fun getHighlightTypeImpl(element: PSI): ProblemHighlightType
}

private class HLPresentationImpl<PSI : PsiElement>(
    private val getHighlightType: (element: PSI) -> ProblemHighlightType,
) : HLPresentation<PSI>() {
    override fun getHighlightTypeImpl(element: PSI): ProblemHighlightType =
        getHighlightType.invoke(element)
}


class HLInspectionPresentationProviderBuilder<PSI : PsiElement> internal constructor() {
    private var getHighlightType: ((element: PSI) -> ProblemHighlightType)? = null

    fun highlightType(getType: (element: PSI) -> ProblemHighlightType) {
        getHighlightType = getType
    }

    fun highlightType(type: ProblemHighlightType) {
        getHighlightType = { type }
    }

    internal fun build(): HLPresentation<PSI> {
        val getHighlightType = getHighlightType
            ?: error("Please, provide highlightType")
        return HLPresentationImpl(getHighlightType)
    }
}

fun <PSI : PsiElement> presentation(
    init: HLInspectionPresentationProviderBuilder<PSI>.() -> Unit
): HLPresentation<PSI> =
    HLInspectionPresentationProviderBuilder<PSI>().apply(init).build()


