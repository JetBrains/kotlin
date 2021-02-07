/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.api.applicator

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.frontend.api.ForbidKtResolve

abstract class HLPresentation<PSI : PsiElement> internal constructor() {
    fun getHighlightType(element: PSI): ProblemHighlightType = ForbidKtResolve.forbidResolveIn("HLPresentation.getHighlightType") {
        getHighlightTypeImpl(element)
    }

    fun getMessage(element: PSI): String = ForbidKtResolve.forbidResolveIn("HLPresentation.getMessage") {
        getMessageImpl(element)
    }

    abstract fun getMessageImpl(element: PSI): String
    abstract fun getHighlightTypeImpl(element: PSI): ProblemHighlightType
}

private class HLPresentationImpl<PSI : PsiElement>(
    private val getHighlightType: (element: PSI) -> ProblemHighlightType,
    private val getMessage: (element: PSI) -> String,
) : HLPresentation<PSI>() {
    override fun getHighlightTypeImpl(element: PSI): ProblemHighlightType =
        getHighlightType.invoke(element)

    override fun getMessageImpl(element: PSI): String =
        getMessage.invoke(element)
}


class HLInspectionPresentationProviderBuilder<PSI : PsiElement> internal constructor() {
    private var getHighlightType: ((element: PSI) -> ProblemHighlightType)? = null
    private var getMessage: ((element: PSI) -> String)? = null

    fun highlightType(getType: (element: PSI) -> ProblemHighlightType) {
        getHighlightType = getType
    }

    fun highlightType(type: ProblemHighlightType) {
        getHighlightType = { type }
    }

    fun inspectionText(getText: (element: PSI) -> String) {
        getMessage = getText
    }

    fun inspectionText(text: String) {
        getMessage = { text }
    }


    internal fun build(): HLPresentation<PSI> =
        HLPresentationImpl(getHighlightType!!, getMessage!!)
}

fun <PSI : PsiElement> presentation(
    init: HLInspectionPresentationProviderBuilder<PSI>.() -> Unit
): HLPresentation<PSI> =
    HLInspectionPresentationProviderBuilder<PSI>().apply(init).build()


