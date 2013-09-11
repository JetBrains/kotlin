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
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetForExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.List;

public class JetForLoopInReference extends AbstractPolyVariantJetReference<JetForExpression> {

    public static PsiReference[] create(@NotNull JetForExpression delegate) {
        return new PsiReference[] { new JetForLoopInReference(delegate) };
    }

    private JetForLoopInReference(@NotNull JetForExpression element) {
        super(element);
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        BindingContext context = AnalyzerFacadeWithCache.getContextForElement(element);

        JetExpression loopRange = element.getLoopRange();
        if (loopRange == null) return ResolveResult.EMPTY_ARRAY;

        List<ResolveResult> results = Lists.newArrayList();

        addResultsForKey(context, results, loopRange, BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL);
        addResultsForKey(context, results, loopRange, BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL);
        addResultsForKey(context, results, loopRange, BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL);

        return results.toArray(new ResolveResult[results.size()]);
    }

    private void addResultsForKey(
            BindingContext context,
            List<ResolveResult> results,
            JetExpression loopRange,
            WritableSlice<JetExpression, ResolvedCall<FunctionDescriptor>> key
    ) {
        ResolvedCall<FunctionDescriptor> resolvedCall = context.get(key, loopRange);
        if (resolvedCall == null) return;

        JetReferenceUtil.findPsiElements(element.getProject(), context, results, resolvedCall.getResultingDescriptor());
    }

    @Override
    public TextRange getRangeInElement() {
        ASTNode inKeywordNode = element.getInKeywordNode();
        if (inKeywordNode == null) return TextRange.EMPTY_RANGE;

        int offset = inKeywordNode.getPsi().getStartOffsetInParent();
        return new TextRange(offset, offset + inKeywordNode.getTextLength());
    }

    @NotNull
    @Override
    public String getCanonicalText() {
        return "<unknown>";
    }
}
