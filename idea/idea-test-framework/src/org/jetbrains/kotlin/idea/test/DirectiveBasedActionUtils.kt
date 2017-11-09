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

package org.jetbrains.kotlin.idea.test

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiFile
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils

object DirectiveBasedActionUtils {
    fun checkForUnexpectedErrors(file: KtFile) {
        if (InTextDirectivesUtils.findLinesWithPrefixesRemoved(file.text, "// DISABLE-ERRORS").isNotEmpty()) {
            return
        }

        val expectedErrors = InTextDirectivesUtils.findLinesWithPrefixesRemoved(file.text, "// ERROR:").sorted()

        val actualErrors = file.analyzeFully().getDiagnostics()
                .filter { it.getSeverity() == Severity.ERROR }
                .map { DefaultErrorMessages.render(it).replace("\n", "<br>") }
                .sorted()

        UsefulTestCase.assertOrderedEquals("All actual errors should be mentioned in test data with // ERROR: directive. But no unnecessary errors should be me mentioned",
                                           actualErrors,
                                           expectedErrors)
    }

    fun checkAvailableActionsAreExpected(file: PsiFile, availableActions: Collection<IntentionAction>) {
        val expectedActions = InTextDirectivesUtils.findLinesWithPrefixesRemoved(file.text, "// ACTION:").sorted()

        UsefulTestCase.assertEmpty("Irrelevant actions should not be specified in ACTION directive for they are not checked anyway",
                                   expectedActions.filter { isIrrelevantAction(it) })

        if (InTextDirectivesUtils.findLinesWithPrefixesRemoved(file.text, "// IGNORE_IRRELEVANT_ACTIONS").isNotEmpty()) {
            return
        }

        val actualActions = availableActions.map { it.text }.sorted()

        UsefulTestCase.assertOrderedEquals("Some unexpected actions available at current position. Use // ACTION: directive",
                                           filterOutIrrelevantActions(actualActions),
                                           expectedActions)
    }

    //TODO: hack, implemented because irrelevant actions behave in different ways on build server and locally
    // this behaviour should be investigated and hack can be removed
    private fun filterOutIrrelevantActions(actions: Collection<String>): Collection<String> {
        return actions.filter { !isIrrelevantAction(it) }
    }

    private fun isIrrelevantAction(action: String) = action.isEmpty() || IRRELEVANT_ACTION_PREFIXES.any { action.startsWith(it) }

    private val IRRELEVANT_ACTION_PREFIXES = listOf(
            "Disable ",
            "Edit intention settings",
            "Edit inspection profile setting",
            "Inject language or reference",
            "Suppress '",
            "Run inspection on",
            "Inspection '",
            "Suppress for ",
            "Suppress all ",
            "Edit cleanup profile settings",
            "Fix all '",
            "Cleanup code",
            "Show local variable type hints",
            "Show function return type hints",
            "Show property type hints",
            "Show parameter type hints",
            "Show argument name hints"
    )
}
