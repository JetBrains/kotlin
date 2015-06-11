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

import com.google.common.base.Function
import com.google.common.base.Predicate
import com.google.common.collect.Collections2
import com.google.common.collect.Lists
import com.google.common.collect.Ordering
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.highlighter.IdeErrorMessages
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils

import java.util.Arrays
import java.util.Collections

public object DirectiveBasedActionUtils {

    public fun checkForUnexpectedErrors(file: JetFile) {
        if (!InTextDirectivesUtils.findLinesWithPrefixesRemoved(file.getText(), "// DISABLE-ERRORS").isEmpty()) {
            return
        }

        val diagnostics = file.analyzeFully().getDiagnostics().all()
        val errorDiagnostics = Collections2.filter(diagnostics, object : Predicate<Diagnostic> {
            override fun apply(diagnostic: Diagnostic?): Boolean {
                assert((diagnostic != null))
                return diagnostic!!.getSeverity() == Severity.ERROR
            }
        })
        val actualErrorStrings = Collections2.transform(errorDiagnostics, object : Function<Diagnostic, String> {
            override fun apply(diagnostic: Diagnostic?): String? {
                assert((diagnostic != null))
                return IdeErrorMessages.render(diagnostic)
            }
        })

        val expectedErrorStrings = InTextDirectivesUtils.findLinesWithPrefixesRemoved(file.getText(), "// ERROR:")
        Collections.sort(expectedErrorStrings)

        UsefulTestCase.assertOrderedEquals("All actual errors should be mentioned in test data with // ERROR: directive. But no unnecessary errors should be me mentioned", Ordering.natural<Comparable>().sortedCopy(actualErrorStrings), expectedErrorStrings)
    }

    public fun checkAvailableActionsAreExpected(file: JetFile, availableActions: Collection<IntentionAction>) {
        val validActions = Ordering.natural<Comparable>().sortedCopy(Lists.newArrayList(InTextDirectivesUtils.findLinesWithPrefixesRemoved(file.getText(), "// ACTION:")))

        val actualActions = Ordering.natural<Comparable>().sortedCopy(Lists.newArrayList(Collections2.transform(availableActions, object : Function<IntentionAction, String> {
            override fun apply(input: IntentionAction?): String? {
                assert(input != null)
                return input!!.getText()
            }
        })))

        UsefulTestCase.assertOrderedEquals("Some unexpected actions available at current position. Use // ACTION: directive", filterOutIrrelevantActions(actualActions), filterOutIrrelevantActions(validActions))
    }

    private //TODO: hack, implemented because irrelevant actions behave in different ways on build server and locally
            // this behaviour should be investigated and hack can be removed
    fun filterOutIrrelevantActions(actions: Collection<String>): Collection<String> {
        return Collections2.filter(actions, object : Predicate<String> {
            override fun apply(input: String?): Boolean {
                for (prefix in IRRELEVANT_ACTION_PREFIXES) {
                    if (input!!.startsWith(prefix)) {
                        return false
                    }
                }
                return true
            }
        })
    }

    private val IRRELEVANT_ACTION_PREFIXES = Arrays.asList("Disable ", "Edit intention settings", "Edit inspection profile setting", "Inject language or reference")
}
