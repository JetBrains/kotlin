/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.Consumer;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * Special contributor for getting completion of type for extensions receiver.
 *
 * @author Nikolay Krasko
 */
public class JetExtensionReceiverTypeContributor extends CompletionContributor {
    private static class ReceiverTypeCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters,
                                      ProcessingContext context,
                                      final @NotNull CompletionResultSet result
        ) {
            if (parameters.getInvocationCount() > 0 || parameters.getCompletionType() == CompletionType.CLASS_NAME) {
                JetClassCompletionContributor.addClasses(parameters, result, new Consumer<LookupElement>() {
                    @Override
                    public void consume(@NotNull LookupElement element) {
                        result.addElement(element);
                    }
                });
            }

            result.stopHere();
        }
    }

    private static final ElementPattern<? extends PsiElement> ACTIVATION_PATTERN =
        PlatformPatterns.psiElement().afterLeaf(JetTokens.FUN_KEYWORD.toString(), JetTokens.VAL_KEYWORD.toString(),
                                                JetTokens.VAR_KEYWORD.toString());

    public JetExtensionReceiverTypeContributor() {
        extend(CompletionType.BASIC, ACTIVATION_PATTERN, new ReceiverTypeCompletionProvider());
        extend(CompletionType.CLASS_NAME, ACTIVATION_PATTERN, new ReceiverTypeCompletionProvider());
    }
}
