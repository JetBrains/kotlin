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

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetValueArgumentList;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.BindingContext.RESOLVED_CALL;

class JetInvokeFunctionReference extends JetPsiReference implements MultiRangeReference {

    public static PsiReference[] create(@NotNull JetCallExpression expression) {
        return new PsiReference[] { new JetInvokeFunctionReference(expression) };
    }

    public JetInvokeFunctionReference(@NotNull JetCallExpression expression) {
        super(expression);
    }

    @Override
    public TextRange getRangeInElement() {
        return getElement().getTextRange().shiftRight(-getElement().getTextOffset());
    }

    @Override
    protected PsiElement doResolve() {
        BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(
                (JetFile) getElement().getContainingFile()).getBindingContext();
        ResolvedCall<? extends CallableDescriptor> invokeFunction =
                bindingContext.get(RESOLVED_CALL, ((JetCallExpression) myExpression).getCalleeExpression());
        if (invokeFunction != null && invokeFunction instanceof VariableAsFunctionResolvedCall) {
            FunctionDescriptor resultingDescriptor = ((VariableAsFunctionResolvedCall) invokeFunction).getResultingDescriptor();
            PsiElement invokeFunctionElement = BindingContextUtils.callableDescriptorToDeclaration(bindingContext, resultingDescriptor);
            return invokeFunctionElement;
        }
        return null;
    }

    @Override
    protected ResolveResult[] doMultiResolve() {
        // TODO If there are more than one invoke(), add all function ("ambiguity"). See JetPsiReference
        PsiElement element = doResolve();
        if (element == null) {
            return ResolveResult.EMPTY_ARRAY;
        }
        return new ResolveResult[] {new PsiElementResolveResult(element, true)};
    }

    @Override
    public List<TextRange> getRanges() {
        List<TextRange> list = new ArrayList<TextRange>();
        JetValueArgumentList valueArgumentList = ((JetCallExpression) myExpression).getValueArgumentList();
        if (valueArgumentList == null) {
            return list;
        }
        if (valueArgumentList.getArguments().size() > 0) {
            ASTNode valueArgumentListNode = valueArgumentList.getNode();
            ASTNode lPar = valueArgumentListNode.findChildByType(JetTokens.LPAR);
            if (lPar != null) {
                list.add(getRange(lPar));
            }

            ASTNode rPar = valueArgumentListNode.findChildByType(JetTokens.RPAR);
            if (rPar != null) {
                list.add(getRange(rPar));
            }
        }
        else {
            list.add(getRange(valueArgumentList.getNode()));
        }

        return list;
    }

    private TextRange getRange(ASTNode node) {
        TextRange textRange = node.getTextRange();
        return textRange.shiftRight(-myExpression.getTextOffset());
    }
}
