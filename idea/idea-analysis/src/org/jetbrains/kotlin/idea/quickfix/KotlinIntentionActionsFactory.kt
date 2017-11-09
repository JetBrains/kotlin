/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtCodeFragment

abstract class KotlinIntentionActionsFactory {
    protected open fun isApplicableForCodeFragment(): Boolean = false

    protected abstract fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction>

    protected open fun doCreateActionsForAllProblems(
            sameTypeDiagnostics: Collection<Diagnostic>): List<IntentionAction> = emptyList()

    fun createActions(diagnostic: Diagnostic): List<IntentionAction> =
            createActions(listOfNotNull(diagnostic), false)

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
