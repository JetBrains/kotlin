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
import com.intellij.psi.MultiRangeReference;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetArrayAccessExpression;
import org.jetbrains.jet.lang.psi.JetContainerNode;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.BindingContext.INDEXED_LVALUE_GET;
import static org.jetbrains.jet.lang.resolve.BindingContext.INDEXED_LVALUE_SET;

public class JetArrayAccessReference extends JetSimpleReference<JetArrayAccessExpression> implements MultiRangeReference {

    public JetArrayAccessReference(@NotNull JetArrayAccessExpression expression) {
        super(expression);
    }

    @Override
    public TextRange getRangeInElement() {
        return getElement().getTextRange().shiftRight(-getElement().getTextOffset());
    }

    @Override
    @NotNull
    protected Collection<DeclarationDescriptor> getTargetDescriptors(@NotNull BindingContext context) {
        List<DeclarationDescriptor> result = Lists.newArrayList();

        ResolvedCall<FunctionDescriptor> getFunction = context.get(INDEXED_LVALUE_GET, getExpression());
        if (getFunction != null) {
            result.add(getFunction.getCandidateDescriptor());
        }

        ResolvedCall<FunctionDescriptor> setFunction = context.get(INDEXED_LVALUE_SET, getExpression());
        if (setFunction != null) {
            result.add(setFunction.getCandidateDescriptor());
        }

        return result;
    }

    @Override
    public List<TextRange> getRanges() {
        List<TextRange> list = new ArrayList<TextRange>();

        JetContainerNode indices = getExpression().getIndicesNode();
        TextRange textRange = indices.getNode().findChildByType(JetTokens.LBRACKET).getTextRange();
        TextRange lBracketRange = textRange.shiftRight(-getExpression().getTextOffset());

        list.add(lBracketRange);

        ASTNode rBracket = indices.getNode().findChildByType(JetTokens.RBRACKET);
        if (rBracket != null) {
            textRange = rBracket.getTextRange();
            TextRange rBracketRange = textRange.shiftRight(-getExpression().getTextOffset());
            list.add(rBracketRange);
        }

        return list;
    }

    @Override
    public boolean canRename() {
        return true;
    }

    @SuppressWarnings("ConstantConditions")
    @Nullable
    @Override
    public PsiElement handleElementRename(@Nullable String newElementName) {
        return ReferencesPackage.renameImplicitConventionalCall(this, newElementName);
    }
}
