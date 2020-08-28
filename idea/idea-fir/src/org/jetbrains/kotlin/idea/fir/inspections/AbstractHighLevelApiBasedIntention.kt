/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.inspections

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import org.jetbrains.annotations.Nls
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyzeWithReadAction
import org.jetbrains.kotlin.idea.frontend.api.computation.ApplicableComputation
import org.jetbrains.kotlin.idea.intentions.SelfTargetingIntention
import org.jetbrains.kotlin.psi.KtElement

abstract class AbstractHighLevelApiBasedIntention<ELEMENT : KtElement, DATA : Any>(
    elementType: Class<ELEMENT>,
    @Nls private val textGetter: () -> String,
    @Nls familyNameGetter: () -> String = textGetter,
) : SelfTargetingIntention<ELEMENT>(elementType, textGetter, familyNameGetter) {

    protected abstract fun isApplicableByPsi(element: ELEMENT): Boolean

    protected open fun isApplicableByPsiAtOffset(element: ELEMENT, offset: Int): Boolean =
        isApplicableByPsi(element)

    protected abstract fun KtAnalysisSession.analyzeAndGetData(element: ELEMENT): DATA?

    protected abstract fun applyTo(element: ELEMENT, data: DATA, editor: Editor?)

    final override fun isApplicableTo(element: ELEMENT, caretOffset: Int): Boolean {
        if (!isApplicableByPsi(element)) return false
        if (!isApplicableByPsiAtOffset(element, caretOffset)) return false
        val data = ApplicationManager.getApplication().executeOnPooledThread<DATA?> {
            analyzeWithReadAction(element) { analyzeAndGetData(element) }
        }.get()
        return data != null
    }

    final override fun applyTo(element: ELEMENT, editor: Editor?) {
        if (!isApplicableByPsi(element)) return
        ApplicationManager.getApplication().executeOnPooledThread {
            val computation = ApplicableComputation(
                computation = { analyzeAndGetData(it) },
                application = { element, data ->
                    applyTo(element, data, editor)
                },
                psiChecker = ::isApplicableByPsi,
                computationTitle = textGetter()
            )
            computation.computeAndApply(element)
        }
    }
}