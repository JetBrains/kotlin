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

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.ResolveResult;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;

import java.util.ArrayList;
import java.util.Collection;

import static org.jetbrains.jet.lang.resolve.BindingContext.AMBIGUOUS_REFERENCE_TARGET;
import static org.jetbrains.jet.lang.resolve.BindingContext.DESCRIPTOR_TO_DECLARATION;

public abstract class JetPsiReference implements PsiPolyVariantReference {

    @NotNull
    protected final JetReferenceExpression myExpression;

    protected JetPsiReference(@NotNull JetReferenceExpression expression) {
        this.myExpression = expression;
    }

    @NotNull
    @Override
    public PsiElement getElement() {
        return myExpression;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        return doMultiResolve();
    }

    @Override
    public PsiElement resolve() {
        return doResolve();
    }

    @NotNull
    @Override
    public String getCanonicalText() {
        return "<TBD>";
    }

    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @NotNull
    @Override
    public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @Override
    public boolean isReferenceTo(PsiElement element) {
        PsiElement target = resolve();
        return target == element || target != null && target.getNavigationElement() == element;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return EMPTY_ARRAY;
    }

    @Override
    public boolean isSoft() {
        return false;
    }

    @Nullable
    protected PsiElement doResolve() {
        JetFile file = (JetFile) getElement().getContainingFile();
        BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(file)
                .getBindingContext();
        PsiElement psiElement = BindingContextUtils.resolveToDeclarationPsiElement(bindingContext, myExpression);
        if (psiElement != null) {
            return psiElement;
        }
        Collection<? extends DeclarationDescriptor> declarationDescriptors = bindingContext.get(AMBIGUOUS_REFERENCE_TARGET, myExpression);
        if (declarationDescriptors != null) return null;

        // TODO: Need a better resolution for Intrinsic function (KT-975)
        return file;
    }

    protected ResolveResult[] doMultiResolve() {
        JetFile file = (JetFile) getElement().getContainingFile();
        BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(file)
                .getBindingContext();
        Collection<? extends DeclarationDescriptor> declarationDescriptors = bindingContext.get(AMBIGUOUS_REFERENCE_TARGET, myExpression);
        if (declarationDescriptors == null) return ResolveResult.EMPTY_ARRAY;

        ArrayList<ResolveResult> results = new ArrayList<ResolveResult>(declarationDescriptors.size());
        
        for (DeclarationDescriptor descriptor : declarationDescriptors) {
            PsiElement element = bindingContext.get(DESCRIPTOR_TO_DECLARATION, descriptor);
            if (element == null) {
                // TODO: Need a better resolution for Intrinsic function (KT-975)
                element = file;
            }

            results.add(new PsiElementResolveResult(element, true));
        }

        return results.toArray(new ResolveResult[results.size()]);
    }
}
