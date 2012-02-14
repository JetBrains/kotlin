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

package org.jetbrains.jet.plugin.references;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.compiler.TipsManager;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.compiler.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.plugin.completion.DescriptorLookupConverter;

import java.util.List;

/**
* @author yole
*/
public class JetSimpleNameReference extends JetPsiReference {

    private final JetSimpleNameExpression myExpression;

    public JetSimpleNameReference(JetSimpleNameExpression jetSimpleNameExpression) {
        super(jetSimpleNameExpression);
        myExpression = jetSimpleNameExpression;
    }

    @Override
    public PsiElement getElement() {
        return myExpression.getReferencedNameElement();
    }

    @Override
    public TextRange getRangeInElement() {
        PsiElement element = getElement();
        if (element == null) return null;
        return new TextRange(0, element.getTextLength());
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(
                (JetFile) myExpression.getContainingFile());

        return collectLookupElements(bindingContext, TipsManager.getReferenceVariants(myExpression, bindingContext));
    }

    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        PsiElement element = JetPsiFactory.createNameIdentifier(myExpression.getProject(), newElementName);
        return myExpression.getReferencedNameElement().replace(element);
    }

    private static Object[] collectLookupElements(BindingContext bindingContext, Iterable<DeclarationDescriptor> descriptors) {
        List<LookupElement> result = Lists.newArrayList();

        for (final DeclarationDescriptor descriptor : descriptors) {
            result.add(DescriptorLookupConverter.createLookupElement(bindingContext, descriptor));
        }

        return result.toArray();
    }
}
