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
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.codeInsight.TipsManager;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespaceHeader;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.plugin.references.JetSimpleNameReference;

/**
 * Performs completion in package directive. Should suggest only packages and avoid showing fake package produced by
 * DUMMY_IDENTIFIER.
 */
public class JetPackagesContributor extends CompletionContributor {

    private static final String DUMMY_IDENTIFIER = "___package___";

    public JetPackagesContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(),
               new CompletionProvider<CompletionParameters>() {
                   @Override
                   protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context,
                                                 @NotNull CompletionResultSet result) {

                       PsiElement position = parameters.getPosition();
                       if (!(position.getContainingFile() instanceof JetFile)) {
                           return;
                       }

                       JetNamespaceHeader namespaceHeader = PsiTreeUtil.getParentOfType(position, JetNamespaceHeader.class);
                       if (namespaceHeader == null) {
                           return;
                       }

                       PsiReference ref = parameters.getPosition().getContainingFile().findReferenceAt(parameters.getOffset());

                       if (ref instanceof JetSimpleNameReference) {
                           JetSimpleNameReference simpleNameReference = (JetSimpleNameReference)ref;

                           String name = simpleNameReference.getExpression().getText();
                           if (name == null) {
                               return;
                           }

                           int prefixLength = parameters.getOffset() - simpleNameReference.getExpression().getTextOffset();
                           result = result.withPrefixMatcher(new PlainPrefixMatcher(name.substring(0, prefixLength)));

                           ResolveSession resolveSession = WholeProjectAnalyzerFacade.getLazyResolveSessionForFile(
                                   (JetFile) simpleNameReference.getExpression().getContainingFile());
                           BindingContext bindingContext = ResolveSessionUtils.resolveToExpression(
                                   resolveSession, simpleNameReference.getExpression());

                           for (LookupElement variant : DescriptorLookupConverter.collectLookupElements(
                                   resolveSession, bindingContext, TipsManager.getPackageReferenceVariants(simpleNameReference.getExpression(), bindingContext))) {
                               if (!variant.getLookupString().contains(DUMMY_IDENTIFIER)) {
                                   result.addElement(variant);
                               }
                           }

                           result.stopHere();
                       }
                   }
               });
    }

    @Override
    public void beforeCompletion(@NotNull CompletionInitializationContext context) {
        // Will need to filter this dummy identifier to avoid showing it in completion
        context.setDummyIdentifier(DUMMY_IDENTIFIER);
        // context.setDummyIdentifier(CompletionInitializationContext.DUMMY_IDENTIFIER);
    }
}
