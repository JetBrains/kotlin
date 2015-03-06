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

package org.jetbrains.kotlin.idea;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.Severity;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.highlighter.IdeErrorMessages;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DirectiveBasedActionUtils {
    private DirectiveBasedActionUtils() {
    }

    public static void checkForUnexpectedErrors(JetFile file) {
        if (!InTextDirectivesUtils.findLinesWithPrefixesRemoved(file.getText(), "// DISABLE-ERRORS").isEmpty()) {
            return;
        }

        Collection<Diagnostic> diagnostics = ResolvePackage.analyzeFully(file).getDiagnostics().all();
        Collection<Diagnostic> errorDiagnostics = Collections2.filter(diagnostics, new Predicate<Diagnostic>() {
            @Override
            public boolean apply(@Nullable Diagnostic diagnostic) {
                assert (diagnostic != null);
                return diagnostic.getSeverity() == Severity.ERROR;
            }
        });
        Collection<String> actualErrorStrings = Collections2.transform(errorDiagnostics, new Function<Diagnostic, String>() {
            @Override
            public String apply(@Nullable Diagnostic diagnostic) {
                assert (diagnostic != null);
                return IdeErrorMessages.render(diagnostic);
            }
        });

        List<String> expectedErrorStrings = InTextDirectivesUtils.findLinesWithPrefixesRemoved(file.getText(), "// ERROR:");
        Collections.sort(expectedErrorStrings);

        UsefulTestCase.assertOrderedEquals(
                "All actual errors should be mentioned in test data with // ERROR: directive. But no unnecessary errors should be me mentioned",
                Ordering.natural().sortedCopy(actualErrorStrings), expectedErrorStrings);
    }

    public static void checkAvailableActionsAreExpected(JetFile file, Collection<IntentionAction> availableActions) {
        List<String> validActions = Ordering.natural().sortedCopy(
                Lists.newArrayList(InTextDirectivesUtils.findLinesWithPrefixesRemoved(file.getText(), "// ACTION:")));

        Collection<String> actualActions = Ordering.natural().sortedCopy(
                Lists.newArrayList(Collections2.transform(availableActions, new Function<IntentionAction, String>() {
                    @Override
                    public String apply(@Nullable IntentionAction input) {
                        assert input != null;
                        return input.getText();
                    }
                })));

        UsefulTestCase.assertOrderedEquals("Some unexpected actions available at current position: %s. Use // ACTION: directive",
                                           filterOutIrrelevantActions(actualActions), filterOutIrrelevantActions(validActions));
    }

    @NotNull
    //TODO: hack, implemented because irrelevant actions behave in different ways on build server and locally
    // this behaviour should be investigated and hack can be removed
    private static Collection<String> filterOutIrrelevantActions(@NotNull Collection<String> actions) {
        return Collections2.filter(actions, new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                for (String prefix : IRRELEVANT_ACTION_PREFIXES) {
                    if (input.startsWith(prefix)) {
                        return false;
                    }
                }
                return true;
            }
        });
    }

    private static final Collection<String> IRRELEVANT_ACTION_PREFIXES =
            Arrays.asList("Disable ", "Edit intention settings", "Edit inspection profile setting", "Inject language or reference");
}
