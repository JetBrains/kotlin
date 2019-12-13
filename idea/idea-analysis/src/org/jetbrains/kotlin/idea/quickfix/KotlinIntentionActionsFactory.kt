/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtCodeFragment

abstract class KotlinIntentionActionsFactory {
    protected open fun isApplicableForCodeFragment(): Boolean = false

    protected abstract fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction>

    protected open fun doCreateActionsForAllProblems(
        sameTypeDiagnostics: Collection<Diagnostic>
    ): List<IntentionAction> = emptyList()

    fun createActions(diagnostic: Diagnostic): List<IntentionAction> = createActions(listOfNotNull(diagnostic), false)

    fun createActionsForAllProblems(sameTypeDiagnostics: Collection<Diagnostic>): List<IntentionAction> =
        createActions(sameTypeDiagnostics, true)

    private fun createActions(sameTypeDiagnostics: Collection<Diagnostic>, createForAll: Boolean): List<IntentionAction> {
        if (sameTypeDiagnostics.isEmpty()) return emptyList()
        val first = sameTypeDiagnostics.first()

        if (first.psiElement.containingFile is KtCodeFragment && !isApplicableForCodeFragment()) {
            return emptyList()
        }

        if (sameTypeDiagnostics.size > 1 && createForAll) {
            assert(sameTypeDiagnostics.all { it.psiElement == first.psiElement && it.factory == first.factory }) {
                "It's expected to be the list of diagnostics of same type and for same element"
            }

            return doCreateActionsForAllProblems(sameTypeDiagnostics)
        }

        return sameTypeDiagnostics.flatMapTo(arrayListOf()) { doCreateActions(it) }
    }
}
