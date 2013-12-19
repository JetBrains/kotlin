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
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.references.JetSimpleNameReference;

public class JetCompletionContributor extends CompletionContributor {
    public JetCompletionContributor() {
        final CompletionProvider<CompletionParameters> provider = new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(
                    @NotNull CompletionParameters parameters,
                    ProcessingContext context,
                    @NotNull CompletionResultSet result
            ) {
                doSimpleReferenceCompletion(parameters, result);
            }
        };
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), provider);
        extend(CompletionType.SMART, PlatformPatterns.psiElement(), provider);
    }

    public static void doSimpleReferenceCompletion(CompletionParameters parameters, CompletionResultSet result) {
        PsiElement position = parameters.getPosition();

        if (!(position.getContainingFile() instanceof JetFile)) {
            return;
        }

        JetSimpleNameReference jetReference = getJetReference(parameters);
        if (jetReference != null) {
            try {
                result.restartCompletionWhenNothingMatches();

                CompletionSession session = new CompletionSession(parameters, result, jetReference, position);
                if (parameters.getCompletionType() == CompletionType.BASIC) {
                    session.completeForReference();

                    if (!session.getJetResult().isSomethingAdded()
                        && session.getParameters().getInvocationCount() < 2) {
                        // Rerun completion if nothing was found
                        session = new CompletionSession(parameters.withInvocationCount(2), result, jetReference, position);
                        session.completeForReference();
                    }
                }
                else {
                    session.completeSmart();
                }
            }
            catch (ProcessCanceledException e) {
                throw CompletionProgressIndicatorUtil.rethrowWithCancelIndicator(e);
            }
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
        if (context.getFile() instanceof JetFile) {
            int offset = context.getStartOffset();
            PsiElement tokenBefore = context.getFile().findElementAt(Math.max(0, offset - 1));
            if (context.getCompletionType() == CompletionType.SMART) {
                context.setDummyIdentifier(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + "$"); // add '$' to ignore context after the caret
            }
            else {
                if (JetPackagesContributor.ACTIVATION_PATTERN.accepts(tokenBefore)) {
                    context.setDummyIdentifier(JetPackagesContributor.DUMMY_IDENTIFIER);
                }
                else if (JetExtensionReceiverTypeContributor.ACTIVATION_PATTERN.accepts(tokenBefore)) {
                    context.setDummyIdentifier(JetExtensionReceiverTypeContributor.DUMMY_IDENTIFIER);
                }
                else{
                    context.setDummyIdentifier(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED);
                }
            }

            // this code will make replacement offset "modified" and prevents altering it by the code in CompletionProgressIndicator
            context.setReplacementOffset(context.getReplacementOffset());

            if (context.getCompletionType() == CompletionType.SMART) {
                PsiElement tokenAt = context.getFile().findElementAt(Math.max(0, offset));
                if (tokenAt != null) {
                    PsiElement parent = tokenAt.getParent();
                    if (parent instanceof JetExpression) {
                        // search expression to be replaced - go up while we are the first child of parent expression
                        JetExpression expression = (JetExpression) parent;
                        parent = expression.getParent();
                        while (parent instanceof JetExpression && parent.getFirstChild() == expression) {
                            expression = (JetExpression) parent;
                            parent = expression.getParent();
                        }
                        context.setReplacementOffset(expression.getTextRange().getEndOffset());
                    }
                }
            }
        }
    }
}
