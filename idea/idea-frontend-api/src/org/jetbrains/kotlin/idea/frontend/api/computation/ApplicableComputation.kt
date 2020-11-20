/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.computation

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyzeWithReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtElement


/**
 * Computation which computes some value by [computation] in context of [KtAnalysisSession],
 * If computations is successful (i.e. it returns non-null value) then it tries to apply result of this computation by [application]
 * [application] is ran in EDT & write action and supposed to modify given KtElement somehow
 * Application happens only if world has not changed since we called [computation]
 * If world changed we [computation] again and again until we success or exceed the number of attempts represented by [tryCount]
 * Should be run from non-EDT thread
 * [computation] 8 [psiChecker] should be pure functions
 */
class ApplicableComputation<ELEMENT : KtElement, DATA : Any>(
    val computation: KtAnalysisSession.(ELEMENT) -> DATA?,
    val application: (ELEMENT, DATA) -> Unit,
    val psiChecker: (ELEMENT) -> Boolean = { true },
    val computationTitle: String,
) {
    @Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")
    fun computeAndApply(element: ELEMENT, tryCount: UInt = UInt.MAX_VALUE): ApplicableComputationResult {
        if (!element.isValid) return ApplicableComputationResult.NonApplicable
        val ideaApplication = ApplicationManager.getApplication()
        if (ideaApplication.isDispatchThread) {
            error("ApplicableComputation.apply should be called from non-EDT thread")
        }
        val project = element.project
        val modificationTracker = PsiModificationTracker.SERVICE.getInstance(project)
        var completed = false
        var exception: Throwable? = null
        var tries = 0u
        while (!completed && tries < tryCount) {
            if (!element.isValid) return ApplicableComputationResult.NonApplicable
            if (!psiChecker(element)) return ApplicableComputationResult.NonApplicable
            val data = analyzeWithReadAction(element) { computation(element) } ?: return ApplicableComputationResult.NonApplicable
            val timestamp = modificationTracker.modificationCount
            ideaApplication.invokeAndWait {
                CommandProcessor.getInstance().executeCommand(
                    project,
                    {
                        runWriteAction {
                            tries++
                            if (modificationTracker.modificationCount == timestamp) {
                                try {
                                    application(element, data)
                                } catch (e: Throwable) {
                                    exception = e
                                } finally {
                                    completed = true
                                }
                            }
                        }
                    },
                    computationTitle,
                    null
                )
            }
        }
        return exception?.let(ApplicableComputationResult::WithException) ?: ApplicableComputationResult.Applied
    }
}

sealed class ApplicableComputationResult {
    @Suppress("SpellCheckingInspection")
    object NonApplicable : ApplicableComputationResult()

    @Suppress("SpellCheckingInspection")
    object Applied : ApplicableComputationResult()

    data class WithException(val exception: Throwable) : ApplicableComputationResult()
}