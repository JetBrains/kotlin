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
import com.intellij.psi.MultiRangeReference;
import com.intellij.psi.PsiReference;
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

class JetArrayAccessReference extends JetPsiReference implements MultiRangeReference {
    private final JetArrayAccessExpression expression;

    public static PsiReference[] create(JetArrayAccessExpression expression) {
        JetContainerNode indicesNode = expression.getIndicesNode();
        return indicesNode == null ? PsiReference.EMPTY_ARRAY : new PsiReference[] { new JetArrayAccessReference(expression) };
    }

    public JetArrayAccessReference(@NotNull JetArrayAccessExpression expression) {
        super(expression);
        this.expression = expression;
    }

    @Override
    public TextRange getRangeInElement() {
        return getElement().getTextRange().shiftRight(-getElement().getTextOffset());
    }

    @Nullable
    @Override
    protected Collection<? extends DeclarationDescriptor> getTargetDescriptors(@NotNull BindingContext context) {
        List<DeclarationDescriptor> result = Lists.newArrayList();

        ResolvedCall<FunctionDescriptor> getFunction = context.get(INDEXED_LVALUE_GET, expression);
        if (getFunction != null) {
            result.add(getFunction.getResultingDescriptor());
        }

        ResolvedCall<FunctionDescriptor> setFunction = context.get(INDEXED_LVALUE_SET, expression);
        if (setFunction != null) {
            result.add(setFunction.getResultingDescriptor());
        }

        return result;
    }

    @Override
    public List<TextRange> getRanges() {
        List<TextRange> list = new ArrayList<TextRange>();

        JetContainerNode indices = expression.getIndicesNode();
        TextRange textRange = indices.getNode().findChildByType(JetTokens.LBRACKET).getTextRange();
        TextRange lBracketRange = textRange.shiftRight(-expression.getTextOffset());

        list.add(lBracketRange);

        ASTNode rBracket = indices.getNode().findChildByType(JetTokens.RBRACKET);
        if (rBracket != null) {
            textRange = rBracket.getTextRange();
            TextRange rBracketRange = textRange.shiftRight(-expression.getTextOffset());
            list.add(rBracketRange);
        }

        return list;
    }
}
