/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.Consumer;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.completion.handlers.JetJavaClassInsertHandler;
import org.jetbrains.jet.plugin.references.JetSimpleNameReference;

/**
 * @author Nikolay Krasko
 */
public class JetClassCompletionContributor extends CompletionContributor {
    public JetClassCompletionContributor() {
        extend(CompletionType.CLASS_NAME, PlatformPatterns.psiElement(),
               new CompletionProvider<CompletionParameters>() {
                   @Override
                   protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context,
                                                 final @NotNull CompletionResultSet result) {

                       final PsiElement position = parameters.getPosition();
                       if (!(position.getContainingFile() instanceof JetFile)) {
                           return;
                       }

                       final PsiReference ref = parameters.getPosition().getContainingFile().findReferenceAt(parameters.getOffset());
                       if (ref instanceof JetSimpleNameReference) {
                           addClasses(parameters, result, new Consumer<LookupElement>() {
                               @Override
                               public void consume(LookupElement lookupElement) {
                                   result.addElement(lookupElement);
                               }
                           });
                           result.stopHere();
                       }
                   }
               });
    }

    /**
     * Jet classes will be added as java completions for unification
     */
    static void addClasses(
            @NotNull final CompletionParameters parameters,
            @NotNull final CompletionResultSet result,
            @NotNull final Consumer<LookupElement> consumer) {

        CompletionResultSet tempResult = result.withPrefixMatcher(CompletionUtil.findReferenceOrAlphanumericPrefix(parameters));
        JavaClassNameCompletionContributor.addAllClasses(
                parameters,
                parameters.getInvocationCount() <= 2,
                JavaCompletionSorting.addJavaSorting(parameters, tempResult).getPrefixMatcher(),
                new Consumer<LookupElement>() {
                    @Override
                    public void consume(LookupElement lookupElement) {
                        // Redefine standard java insert handler which is going to insert fqn
                        if (lookupElement instanceof JavaPsiClassReferenceElement) {
                            JavaPsiClassReferenceElement javaPsiReferenceElement = (JavaPsiClassReferenceElement) lookupElement;
                            javaPsiReferenceElement.setInsertHandler(JetJavaClassInsertHandler.JAVA_CLASS_INSERT_HANDLER);
                        }

                        consumer.consume(lookupElement);
                    }
                });
    }
}
