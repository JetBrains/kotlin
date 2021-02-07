/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.api.applicator

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession

/**
 * Resolves the code to provide [HLApplicator] some input
 */
abstract class HLApplicatorInputProvider<PSI : PsiElement, out INPUT : HLApplicatorInput> {
    /**
     * Provide input to the applicator, if returns `null` then the applicator is not applicable and will not be called
     * Guaranteed to be executed from read action, should not be called from EDT thread
     */
    abstract fun KtAnalysisSession.provideInput(element: PSI): INPUT?
}

private class HLApplicatorInputProviderImpl<PSI : PsiElement, out INPUT : HLApplicatorInput>(
    private val provideInput: KtAnalysisSession.(PSI) -> INPUT?
) : HLApplicatorInputProvider<PSI, INPUT>() {
    override fun KtAnalysisSession.provideInput(element: PSI): INPUT? = provideInput.invoke(this, element)
}

/**
 * Creates [HLApplicatorInputProvider]
 * The [provideInput] is guaranteed to be executed from read action, should not be called from EDT thread
 */
fun <PSI : PsiElement, INPUT : HLApplicatorInput> inputProvider(
    provideInput: KtAnalysisSession.(PSI) -> INPUT?
): HLApplicatorInputProvider<PSI, INPUT> =
    HLApplicatorInputProviderImpl(provideInput)