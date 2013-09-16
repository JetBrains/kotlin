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

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyAccessorDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.JetPropertyDelegate;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;

import java.util.List;

public class JetPropertyDelegationMethodsReference extends AbstractPolyVariantJetReference<JetPropertyDelegate> {

    public static PsiReference[] create(@NotNull JetPropertyDelegate delegate) {
        return new PsiReference[] { new JetPropertyDelegationMethodsReference(delegate) };
    }

    public JetPropertyDelegationMethodsReference(@NotNull JetPropertyDelegate element) {
        super(element);
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
        addResultsForAccessor(property.getProject(), context, results, propertyDescriptor.getGetter());
        addResultsForAccessor(property.getProject(), context, results, propertyDescriptor.getSetter());

        return results.toArray(new ResolveResult[results.size()]);
    }

    private static void addResultsForAccessor(
            Project project,
            BindingContext context,
            List<ResolveResult> results,
            @Nullable PropertyAccessorDescriptor accessor
    ) {
        if (accessor == null) return;

        ResolvedCall<FunctionDescriptor> resolvedCall = context.get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, accessor);
        if (resolvedCall == null) return;

        JetReferenceUtil.findPsiElements(project, context, results, resolvedCall.getResultingDescriptor());
    }

    @Override
    public TextRange getRangeInElement() {
        ASTNode byKeywordNode = element.getByKeywordNode();
        int offset = byKeywordNode.getPsi().getStartOffsetInParent();
        return new TextRange(offset, offset + byKeywordNode.getTextLength());
    }

    @NotNull
    @Override
    public String getCanonicalText() {
        return "<unknown>";
    }
}
