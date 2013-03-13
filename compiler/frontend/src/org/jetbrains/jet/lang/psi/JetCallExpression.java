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

package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collections;
import java.util.List;

public class JetCallExpression extends JetReferenceExpression implements JetCallElement {
    public JetCallExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    @Override
    public PsiReference getReference() {
        PsiReference[] references = getReferences();
        if (references.length == 1) return references[0];
        else return null;
    }

    @NotNull
    @Override
    public PsiReference[] getReferences() {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitCallExpression(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitCallExpression(this, data);
    }

    @Override
    @Nullable
    public JetExpression getCalleeExpression() {
        return findChildByClass(JetExpression.class);
    }

    @Override
    @Nullable
    public JetValueArgumentList getValueArgumentList() {
        return (JetValueArgumentList) findChildByType(JetNodeTypes.VALUE_ARGUMENT_LIST);
    }

    @Nullable
    public JetTypeArgumentList getTypeArgumentList() {
        return (JetTypeArgumentList) findChildByType(JetNodeTypes.TYPE_ARGUMENT_LIST);
    }

    @Override
    @NotNull
    public List<JetExpression> getFunctionLiteralArguments() {
        JetExpression calleeExpression = getCalleeExpression();
        ASTNode node;
        if (calleeExpression instanceof JetFunctionLiteralExpression) {
            node = calleeExpression.getNode().getTreeNext();
        }
        else {
            node = getNode().getFirstChildNode();
        }
        List<JetExpression> result = new SmartList<JetExpression>();
        while (node != null) {
            PsiElement psi = node.getPsi();
            if (psi instanceof JetFunctionLiteralExpression) {
                result.add((JetFunctionLiteralExpression) psi);
            }
            else if (psi instanceof JetPrefixExpression) {
                JetPrefixExpression prefixExpression = (JetPrefixExpression) psi;
                if (JetTokens.LABELS.contains(prefixExpression.getOperationReference().getReferencedNameElementType())) {
                    JetExpression labeledExpression = prefixExpression.getBaseExpression();
                    if (labeledExpression instanceof JetFunctionLiteralExpression) {
                        result.add(labeledExpression);
                    }
                }
            }
            node = node.getTreeNext();
        }
        return result;
    }

    @Override
    @NotNull
    public List<? extends ValueArgument> getValueArguments() {
        JetValueArgumentList list = getValueArgumentList();
        return list != null ? list.getArguments() : Collections.<JetValueArgument>emptyList();
    }

    @NotNull
    public List<JetTypeProjection> getTypeArguments() {
        JetTypeArgumentList list = getTypeArgumentList();
        return list != null ? list.getArguments() : Collections.<JetTypeProjection>emptyList();
    }
}
