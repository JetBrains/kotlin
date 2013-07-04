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

package org.jetbrains.jet.plugin.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.references.JetSimpleNameReference;

public class JetCompletionContributor extends CompletionContributor {
    public JetCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(),
               new CompletionProvider<CompletionParameters>() {
                   @Override
                   protected void addCompletions(
                           @NotNull CompletionParameters parameters,
                           ProcessingContext context,
                           @NotNull CompletionResultSet result
                   ) {
                       doSimpleReferenceCompletion(parameters, result);
                   }
               });
    }

    public static void doSimpleReferenceCompletion(CompletionParameters parameters, CompletionResultSet result) {
        PsiElement position = parameters.getPosition();

        if (!(position.getContainingFile() instanceof JetFile)) {
            return;
        }

        JetSimpleNameReference jetReference = getJetReference(parameters);
        if (jetReference != null) {
            result.restartCompletionWhenNothingMatches();

            CompletionSession session = new CompletionSession(parameters, result, jetReference, position);
            session.completeForReference();

            if (!session.getJetResult().isSomethingAdded() && session.getParameters().getInvocationCount() < 2) {
                // Rerun completion if nothing was found
                session = new CompletionSession(parameters.withInvocationCount(2), result, jetReference, position);
                session.completeForReference();
            }

            // Prevent from adding reference variants from standard reference contributor
            result.stopHere();
        }
    }

    @Nullable
    private static JetSimpleNameReference getJetReference(@NotNull CompletionParameters parameters) {
        PsiElement element = parameters.getPosition();
        if (element.getParent() != null) {
            PsiElement parent = element.getParent();
            PsiReference[] references = parent.getReferences();

            if (references.length != 0) {
                for (PsiReference reference : references) {
                    if (reference instanceof JetSimpleNameReference) {
                        return (JetSimpleNameReference)reference;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void beforeCompletion(@NotNull CompletionInitializationContext context) {
        if (context.getCompletionType() == CompletionType.BASIC && context.getFile() instanceof JetFile) {
            PsiElement position = context.getFile().findElementAt(Math.max(0, context.getStartOffset() - 1));

            if (JetPackagesContributor.ACTIVATION_PATTERN.accepts(position)) {
                context.setDummyIdentifier(JetPackagesContributor.DUMMY_IDENTIFIER);
            }
            else if (JetExtensionReceiverTypeContributor.ACTIVATION_PATTERN.accepts(position)) {
                context.setDummyIdentifier(JetExtensionReceiverTypeContributor.DUMMY_IDENTIFIER);
            }
        }
    }
}
