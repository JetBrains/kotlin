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
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPackageDirective;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.codeInsight.TipsManager;
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies;
import org.jetbrains.jet.plugin.references.JetSimpleNameReference;

import java.util.Collection;

/**
 * Performs completion in package directive. Should suggest only packages and avoid showing fake package produced by
 * DUMMY_IDENTIFIER.
 */
public class JetPackagesContributor extends CompletionContributor {

    static final String DUMMY_IDENTIFIER = "___package___";

    static final ElementPattern<? extends PsiElement> ACTIVATION_PATTERN =
            PlatformPatterns.psiElement().inside(JetPackageDirective.class);


    public JetPackagesContributor() {
        extend(CompletionType.BASIC, ACTIVATION_PATTERN,
               new CompletionProvider<CompletionParameters>() {
                   @Override
                   protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context,
                                                 @NotNull CompletionResultSet result) {

                       PsiElement position = parameters.getPosition();
                       if (!(position.getContainingFile() instanceof JetFile)) {
                           return;
                       }

                       PsiReference ref = parameters.getPosition().getContainingFile().findReferenceAt(parameters.getOffset());

                       if (ref instanceof JetSimpleNameReference) {
                           JetSimpleNameReference simpleNameReference = (JetSimpleNameReference)ref;

                           String name = simpleNameReference.getExpression().getText();
                           if (name == null) {
                               return;
                           }

                           try {
                               int prefixLength = parameters.getOffset() - simpleNameReference.getExpression().getTextOffset();
                               result = result.withPrefixMatcher(new PlainPrefixMatcher(name.substring(0, prefixLength)));

                               ResolveSessionForBodies resolveSession =
                                       ResolvePackage.getLazyResolveSession(simpleNameReference.getExpression());
                               BindingContext bindingContext = resolveSession.resolveToElement(simpleNameReference.getExpression());

                               Collection<DeclarationDescriptor> variants =
                                       TipsManager.getPackageReferenceVariants(simpleNameReference.getExpression(), bindingContext);
                               for (LookupElement variant : DescriptorLookupConverter.collectLookupElements(resolveSession, variants)) {
                                   if (!variant.getLookupString().contains(DUMMY_IDENTIFIER)) {
                                       result.addElement(variant);
                                   }
                               }

                               result.stopHere();
                           }
                           catch (ProcessCanceledException e) {
                               throw CompletionProgressIndicatorUtil.rethrowWithCancelIndicator(e);
                           }
                       }
                   }
               });
    }
}
