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
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespaceHeader;
import org.jetbrains.jet.plugin.references.JetPackageReference;

/**
 * @author Nikolay Krasko
 */
public class JetPackagesContributor extends CompletionContributor {

    private static final String DUMMY_IDENTIFIER = "___package___";

    public JetPackagesContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(),
               new CompletionProvider<CompletionParameters>() {
                   @Override
                   protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context,
                                                 @NotNull CompletionResultSet result) {

                       final PsiElement position = parameters.getPosition();
                       if (!(position.getContainingFile() instanceof JetFile)) {
                           return;
                       }

                       JetNamespaceHeader namespaceHeader = PsiTreeUtil.getParentOfType(position, JetNamespaceHeader.class);
                       if (namespaceHeader == null) {
                           return;
                       }

                       final PsiReference ref = parameters.getPosition().getContainingFile().findReferenceAt(parameters.getOffset());

                       if (ref != null) {

                           // For package there will be a wrong prefix matcher with the whole package directive as prefix
                           if (ref instanceof JetPackageReference) {
                               JetPackageReference packageRef = (JetPackageReference) ref;
                               PsiElement nameIdentifier = packageRef.getExpression().getNameIdentifier();
                               if (nameIdentifier == null) {
                                   return;
                               }

                               if (!(nameIdentifier.getTextOffset() <= parameters.getOffset())) {
                                   return;
                               }

                               int prefixLength = parameters.getOffset() - nameIdentifier.getTextOffset();
                               result = result.withPrefixMatcher(new PlainPrefixMatcher(nameIdentifier.getText().substring(0, prefixLength)));
                           }

                           Object[] variants = ref.getVariants();
                           for (Object variant : variants) {
                               if (variant instanceof LookupElement) {
                                   LookupElement lookupElement = (LookupElement) variant;
                                   if (!lookupElement.getLookupString().contains(DUMMY_IDENTIFIER)) {
                                       result.addElement((LookupElement) variant);
                                   }
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
    }
}
