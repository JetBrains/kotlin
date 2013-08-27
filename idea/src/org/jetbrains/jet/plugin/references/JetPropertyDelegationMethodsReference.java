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

import com.beust.jcommander.internal.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.JetPropertyDelegate;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;

import java.util.List;

public class JetPropertyDelegationMethodsReference implements PsiPolyVariantReference {

    public static PsiReference[] create(@NotNull JetPropertyDelegate delegate) {
        return new PsiReference[] { new JetPropertyDelegationMethodsReference(delegate) };
    }

    private final JetPropertyDelegate element;

    private JetPropertyDelegationMethodsReference(@NotNull JetPropertyDelegate element) {
        this.element = element;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        JetProperty property = PsiTreeUtil.getParentOfType(element, JetProperty.class);
        if (property == null) {
            return ResolveResult.EMPTY_ARRAY;
        }

        BindingContext context = AnalyzerFacadeWithCache.getContextForElement(element);

        DeclarationDescriptor descriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, property);
        if (!(descriptor instanceof PropertyDescriptor)) {
            return ResolveResult.EMPTY_ARRAY;
        }

        List<ResolveResult> results = Lists.newArrayList();

        PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;
        addResultsForAccessor(context, results, propertyDescriptor.getGetter());
        addResultsForAccessor(context, results, propertyDescriptor.getSetter());

        return results.toArray(new ResolveResult[results.size()]);
    }

    private static void addResultsForAccessor(BindingContext context, List<ResolveResult> results, PropertyAccessorDescriptor accessor) {
        if (accessor != null) {
            ResolvedCall<FunctionDescriptor> resolvedCall = context.get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, accessor);
            if (resolvedCall != null) {
                FunctionDescriptor resultingDescriptor = resolvedCall.getResultingDescriptor();

                PsiElement declaration = BindingContextUtils.descriptorToDeclaration(context, resultingDescriptor);
                if (declaration != null) {
                    results.add(new PsiElementResolveResult(declaration, true));
                }
            }
        }
    }

    @Override
    public PsiElement getElement() {
        return element;
    }

    @Override
    public TextRange getRangeInElement() {
        ASTNode byKeywordNode = element.getByKeywordNode();
        int offset = byKeywordNode.getPsi().getStartOffsetInParent();
        return new TextRange(offset, offset + byKeywordNode.getTextLength());
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        ResolveResult[] results = multiResolve(false);
        if (results.length == 1) return results[0].getElement();
        return null;
    }

    @NotNull
    @Override
    public String getCanonicalText() {
        return "get";
    }

    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @Override
    public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @Override
    public boolean isReferenceTo(PsiElement element) {
        if (element == null) return false;

        ResolveResult[] results = multiResolve(false);
        for (ResolveResult result : results) {
            if (element.equals(result.getElement())) return true;
        }
        return false;
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
}
