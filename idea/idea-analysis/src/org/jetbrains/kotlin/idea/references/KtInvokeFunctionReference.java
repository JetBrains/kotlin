/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.references;

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.MultiRangeReference;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.kotlin.util.OperatorNameConventions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class KtInvokeFunctionReference extends KtSimpleReference<KtCallExpression> implements MultiRangeReference {

    public KtInvokeFunctionReference(@NotNull KtCallExpression expression) {
        super(expression);
    }

    @Override
    public TextRange getRangeInElement() {
        return getElement().getTextRange().shiftRight(-getElement().getTextOffset());
    }

    @Override
    @NotNull
    protected Collection<DeclarationDescriptor> getTargetDescriptors(@NotNull BindingContext context) {
        Call call = CallUtilKt.getCall(getElement(), context);
        ResolvedCall<?> resolvedCall = CallUtilKt.getResolvedCall(call, context);
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
        KtValueArgumentList valueArgumentList = getExpression().getValueArgumentList();
        if (valueArgumentList != null) {
            if (valueArgumentList.getArguments().size() > 0) {
                ASTNode valueArgumentListNode = valueArgumentList.getNode();
                ASTNode lPar = valueArgumentListNode.findChildByType(KtTokens.LPAR);
                if (lPar != null) {
                    list.add(getRange(lPar));
                }

                ASTNode rPar = valueArgumentListNode.findChildByType(KtTokens.RPAR);
                if (rPar != null) {
                    list.add(getRange(rPar));
                }
            }
            else {
                list.add(getRange(valueArgumentList.getNode()));
            }
        }

        List<KtLambdaArgument> functionLiteralArguments = getExpression().getLambdaArguments();
        for (KtLambdaArgument functionLiteralArgument : functionLiteralArguments) {
            KtLambdaExpression functionLiteralExpression = functionLiteralArgument.getLambdaExpression();
            list.add(getRange(functionLiteralExpression.getLeftCurlyBrace()));
            ASTNode rightCurlyBrace = functionLiteralExpression.getRightCurlyBrace();
            if (rightCurlyBrace != null) {
                list.add(getRange(rightCurlyBrace));
            }
        }

        return list;
    }

    private TextRange getRange(ASTNode node) {
        TextRange textRange = node.getTextRange();
        return textRange.shiftRight(-getExpression().getTextOffset());
    }

    @Override
    public boolean canRename() {
        return true;
    }

    @SuppressWarnings("ConstantConditions")
    @Nullable
    @Override
    public PsiElement handleElementRename(@Nullable String newElementName) {
        return ReferenceUtilKt.renameImplicitConventionalCall(this, newElementName);
    }

    private static final List<Name> NAMES = Lists.newArrayList(OperatorNameConventions.INVOKE);

    @NotNull
    @Override
    public Collection<Name> getResolvesByNames() {
        return NAMES;
    }
}
