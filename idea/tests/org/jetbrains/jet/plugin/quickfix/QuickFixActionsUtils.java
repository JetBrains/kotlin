/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.quickfix;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.highlighter.IdeErrorMessages;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class QuickFixActionsUtils {
    private QuickFixActionsUtils() {
    }

    public static void checkForUnexpectedErrors(JetFile file) {
        AnalyzeExhaust exhaust = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(file);

        Collection<Diagnostic> diagnostics = exhaust.getBindingContext().getDiagnostics();
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
                return IdeErrorMessages.RENDERER.render(diagnostic);
            }
        });

        List<String> expectedErrorStrings = InTextDirectivesUtils.findLinesWithPrefixRemoved(file.getText(), "// ERROR:");
        Collections.sort(expectedErrorStrings);

        UsefulTestCase.assertOrderedEquals(
                "All actual errors should be mentioned in test data with // ERROR: directive. But no unnecessary errors should be me mentioned",
                Ordering.natural().sortedCopy(actualErrorStrings), expectedErrorStrings);
    }

    public static void checkAvailableActionsAreExpected(JetFile file, Collection<IntentionAction> availableActions) {
        List<String> validActions = Ordering.natural().sortedCopy(
                Sets.newHashSet(InTextDirectivesUtils.findLinesWithPrefixRemoved(file.getText(), "// ACTION:")));

        Collection<String> actualActions = Ordering.natural().sortedCopy(
                Collections2.transform(availableActions, new Function<IntentionAction, String>() {
                    @Override
                    public String apply(@Nullable IntentionAction input) {
                        assert input != null;
                        return input.getText();
                    }
                }));

        UsefulTestCase.assertOrderedEquals("Some unexpected actions available at current position: %s. Use // ACTION: directive",
                                           actualActions, validActions);
    }
}
