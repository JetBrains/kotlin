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
import com.intellij.psi.MultiRangeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.bindingContextUtil.BindingContextUtilPackage;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class JetInvokeFunctionReference extends JetSimpleReference<JetCallExpression> implements MultiRangeReference {

    public JetInvokeFunctionReference(@NotNull JetCallExpression expression) {
        super(expression);
    }

    @Override
    public TextRange getRangeInElement() {
        return getElement().getTextRange().shiftRight(-getElement().getTextOffset());
    }

    @Override
    @NotNull
    protected Collection<DeclarationDescriptor> getTargetDescriptors(@NotNull BindingContext context) {
        Call call = BindingContextUtilPackage.getCall(getElement(), context);
        ResolvedCall<?> resolvedCall = BindingContextUtilPackage.getResolvedCall(call, context);
        if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
            return Collections.<DeclarationDescriptor>singleton(
                    ((VariableAsFunctionResolvedCall) resolvedCall).getFunctionCall().getCandidateDescriptor());
        }
        if (call != null && resolvedCall != null && call.getCallType() == Call.CallType.INVOKE) {
            return Collections.<DeclarationDescriptor>singleton(resolvedCall.getCandidateDescriptor());
        }        
        return Collections.emptyList();
    }

    @Override
    public List<TextRange> getRanges() {
        List<TextRange> list = new ArrayList<TextRange>();
        JetValueArgumentList valueArgumentList = getExpression().getValueArgumentList();
        if (valueArgumentList != null) {
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
        }

        List<JetExpression> functionLiteralArguments = getExpression().getFunctionLiteralArguments();
        for (JetExpression functionLiteralArgument : functionLiteralArguments) {
            while (functionLiteralArgument instanceof JetLabeledExpression) {
                functionLiteralArgument = ((JetLabeledExpression) functionLiteralArgument).getBaseExpression();
            }

            if (functionLiteralArgument instanceof JetFunctionLiteralExpression) {
                JetFunctionLiteralExpression functionLiteralExpression = (JetFunctionLiteralExpression) functionLiteralArgument;
                list.add(getRange(functionLiteralExpression.getLeftCurlyBrace()));
                ASTNode rightCurlyBrace = functionLiteralExpression.getRightCurlyBrace();
                if (rightCurlyBrace != null) {
                    list.add(getRange(rightCurlyBrace));
                }
            }
        }

        return list;
    }

    private TextRange getRange(ASTNode node) {
        TextRange textRange = node.getTextRange();
        return textRange.shiftRight(-getExpression().getTextOffset());
    }
}
