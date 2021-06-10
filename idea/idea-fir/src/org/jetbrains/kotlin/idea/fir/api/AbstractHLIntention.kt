/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.api

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.api.applicator.HLApplicator
import org.jetbrains.kotlin.idea.api.applicator.HLApplicatorInput
import org.jetbrains.kotlin.idea.fir.api.applicator.HLApplicabilityRange
import org.jetbrains.kotlin.idea.fir.api.applicator.HLApplicatorInputProvider
import org.jetbrains.kotlin.idea.frontend.api.tokens.HackToForceAllowRunningAnalyzeOnEDT
import org.jetbrains.kotlin.idea.frontend.api.analyseWithReadAction
import org.jetbrains.kotlin.idea.frontend.api.tokens.hackyAllowRunningOnEdt
import org.jetbrains.kotlin.idea.intentions.SelfTargetingIntention
import org.jetbrains.kotlin.psi.KtElement
import kotlin.reflect.KClass

abstract class AbstractHLIntention<PSI : KtElement, INPUT : HLApplicatorInput>(
    elementType: KClass<PSI>,
    val applicator: HLApplicator<PSI, INPUT>
) : SelfTargetingIntention<PSI>(elementType.java, applicator::getFamilyName) {
    final override fun isApplicableTo(element: PSI, caretOffset: Int): Boolean {
        val project = element.project// TODO expensive operation, may require traversing the tree up to containing PsiFile
        if (!applicator.isApplicableByPsi(element, project)) return false
        val ranges = applicabilityRange.getApplicabilityRanges(element)
        if (ranges.isEmpty()) return false

        // An HLApplicabilityRange should be relative to the element, while `caretOffset` is absolute
        val relativeCaretOffset = caretOffset - element.textRange.startOffset
        if (ranges.none { it.containsOffset(relativeCaretOffset) }) return false

        val input = getInput(element)
        if (input != null && input.isValidFor(element)) {
            setFamilyNameGetter(applicator::getFamilyName)
            setTextGetter { applicator.getActionName(element, input) }
            return true
        }
        return false
    }


    final override fun applyTo(element: PSI, project: Project, editor: Editor?) {
        val input = getInput(element) ?: return
        if (input.isValidFor(element)) {
            applicator.applyTo(element, input, project, editor)
        }
    }

    final override fun applyTo(element: PSI, editor: Editor?) {
        applyTo(element, element.project, editor)
    }


    @OptIn(HackToForceAllowRunningAnalyzeOnEDT::class)
    private fun getInput(element: PSI): INPUT? = hackyAllowRunningOnEdt {
        analyseWithReadAction(element) {
            with(inputProvider) { provideInput(element) }
        }
    }

    abstract val applicabilityRange: HLApplicabilityRange<PSI>
    abstract val inputProvider: HLApplicatorInputProvider<PSI, INPUT>
}
