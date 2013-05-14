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

package org.jetbrains.jet.plugin.references;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.ResolveResult;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.codeInsight.DescriptorToDeclarationUtil;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.BindingContext.AMBIGUOUS_LABEL_TARGET;
import static org.jetbrains.jet.lang.resolve.BindingContext.AMBIGUOUS_REFERENCE_TARGET;

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
        AnalyzeExhaust analyzeExhaust = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(file);
        List<PsiElement> psiElements = DescriptorToDeclarationUtil.resolveToDeclarationPsiElements(analyzeExhaust, myExpression);
        if (psiElements.size() == 1) {
            return psiElements.iterator().next();
        }
        if (psiElements.size() > 1) {
            return null;
        }
        Collection<PsiElement> stdlibSymbols = resolveStandardLibrarySymbol(analyzeExhaust.getBindingContext());
        if (stdlibSymbols.size() == 1) {
            return stdlibSymbols.iterator().next();
        }
        return null;
    }

    protected ResolveResult[] doMultiResolve() {
        JetFile file = (JetFile) getElement().getContainingFile();
        AnalyzeExhaust analyzeExhaust = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(file);
        BindingContext bindingContext = analyzeExhaust.getBindingContext();
        Collection<? extends DeclarationDescriptor> declarationDescriptors = bindingContext.get(AMBIGUOUS_REFERENCE_TARGET, myExpression);
        if (declarationDescriptors == null) {
            List<PsiElement> psiElements = DescriptorToDeclarationUtil.resolveToDeclarationPsiElements(analyzeExhaust, myExpression);
            if (psiElements.size() > 1) {
                return PsiElementResolveResult.createResults(psiElements);
            }
            Collection<? extends PsiElement> labelTargets = bindingContext.get(AMBIGUOUS_LABEL_TARGET, myExpression);
            if (labelTargets != null && !labelTargets.isEmpty()) {
                return PsiElementResolveResult.createResults(labelTargets);
            }
            Collection<PsiElement> standardLibraryElements = resolveStandardLibrarySymbol(bindingContext);
            if (standardLibraryElements.size() > 1) {
                return PsiElementResolveResult.createResults(standardLibraryElements);
            }
            return ResolveResult.EMPTY_ARRAY;
        }

        List<ResolveResult> results = new ArrayList<ResolveResult>(declarationDescriptors.size());
        for (DeclarationDescriptor descriptor : declarationDescriptors) {
            List<PsiElement> elements = DescriptorToDeclarationUtil.descriptorToDeclarations(bindingContext, descriptor);
            for (PsiElement element : elements) {
                results.add(new PsiElementResolveResult(element, true));
            }
        }
        return results.toArray(new ResolveResult[results.size()]);
    }

    private Collection<PsiElement> resolveStandardLibrarySymbol(@NotNull BindingContext bindingContext) {
        return myExpression.getProject().getComponent(BuiltInsReferenceResolver.class)
                .resolveStandardLibrarySymbol(bindingContext, myExpression);
    }
}
