/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyzeWithReadAction
import org.jetbrains.kotlin.idea.frontend.api.computation.ApplicableComputation
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtVisitorVoid

abstract class AbstractHighLevelApiBasedInspection<ELEMENT : KtElement, DATA : Any>(
    val elementType: Class<ELEMENT>
) : AbstractKotlinInspection() {

    final override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
        object : KtVisitorVoid() {
            override fun visitKtElement(element: KtElement) {
                super.visitKtElement(element)

                if (!elementType.isInstance(element) || element.textLength == 0) return
                @Suppress("UNCHECKED_CAST")
                visitTargetElement(element as ELEMENT, holder, isOnTheFly)
            }
        }

    protected fun visitTargetElement(element: ELEMENT, holder: ProblemsHolder, isOnTheFly: Boolean) {
        if (!isApplicableByPsi(element)) return
        if (analyzeWithReadAction(element) { analyzeAndGetData(element) == null }) return

        holder.registerProblemWithoutOfflineInformation(
            element,
            inspectionText(element),
            isOnTheFly,
            inspectionHighlightType(element),
            inspectionHighlightRangeInElement(element),
            LocalFix(fixText(element))
        )
    }

    open fun inspectionHighlightRangeInElement(element: ELEMENT): TextRange? = null

    open fun inspectionHighlightType(element: ELEMENT): ProblemHighlightType = ProblemHighlightType.GENERIC_ERROR_OR_WARNING

    abstract fun inspectionText(element: ELEMENT): String

    abstract val defaultFixText: String

    open fun fixText(element: ELEMENT) = defaultFixText

    abstract fun isApplicableByPsi(element: ELEMENT): Boolean

    abstract fun KtAnalysisSession.analyzeAndGetData(element: ELEMENT): DATA?

    abstract fun applyTo(element: ELEMENT, data: DATA, project: Project = element.project, editor: Editor? = null)

    private inner class LocalFix(val text: String) : LocalQuickFix {
        override fun startInWriteAction() = false

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            @Suppress("UNCHECKED_CAST")
            val element = descriptor.psiElement as ELEMENT
            if (!isApplicableByPsi(element)) return

            ApplicationManager.getApplication().executeOnPooledThread {
                val computation = ApplicableComputation(
                    computation = { analyzeAndGetData(it) },
                    application = { element, data ->
                        applyTo(element, data, project, element.findExistingEditor())
                    },
                    psiChecker = ::isApplicableByPsi,
                    computationTitle = fixText(element)
                )
                computation.computeAndApply(element)
            }
        }

        override fun getFamilyName() = defaultFixText

        override fun getName() = text
    }
}