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
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.highlighter.IdeErrorMessages
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils

public object DirectiveBasedActionUtils {
    public fun checkForUnexpectedErrors(file: JetFile) {
        if (InTextDirectivesUtils.findLinesWithPrefixesRemoved(file.getText(), "// DISABLE-ERRORS").isNotEmpty()) {
            return
        }

        val expectedErrors = InTextDirectivesUtils.findLinesWithPrefixesRemoved(file.getText(), "// ERROR:").sort()

        val actualErrors = file.analyzeFully().getDiagnostics()
                .filter { it.getSeverity() == Severity.ERROR }
                .map {  IdeErrorMessages.render(it) }
                .sort()

        UsefulTestCase.assertOrderedEquals("All actual errors should be mentioned in test data with // ERROR: directive. But no unnecessary errors should be me mentioned",
                                           actualErrors,
                                           expectedErrors)
    }

    public fun checkAvailableActionsAreExpected(file: JetFile, availableActions: Collection<IntentionAction>) {
        val expectedActions = InTextDirectivesUtils.findLinesWithPrefixesRemoved(file.getText(), "// ACTION:").sort()

        UsefulTestCase.assertEmpty("Irrelevant actions should not be specified in ACTION directive for they are not checked anyway",
                                   expectedActions.filter { isIrrelevantAction(it) })

        val actualActions = availableActions.map { it.getText() }.sort()

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
            "Suppress '"
    )
}
