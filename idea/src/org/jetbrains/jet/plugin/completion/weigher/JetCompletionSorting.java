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

package org.jetbrains.jet.plugin.completion.weigher;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionSorter;
import org.jetbrains.jet.lang.psi.JetFile;

public final class JetCompletionSorting {
    private JetCompletionSorting() {
    }

    public static CompletionResultSet addJetSorting(CompletionParameters parameters, CompletionResultSet result) {
        CompletionSorter sorter = CompletionSorter.defaultSorter(parameters, result.getPrefixMatcher());
        sorter = sorter.weighAfter("stats",
                                   new JetLocalPreferableWeigher(),
                                   new JetExplicitlyImportedWeigher((JetFile)parameters.getOriginalFile()),
                                   new JetAccessibleWeigher());
        return result.withRelevanceSorter(sorter);
    }
}
