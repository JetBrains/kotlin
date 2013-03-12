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

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collections;
import java.util.List;

public class JetArrayAccessExpression extends JetReferenceExpression {
    public JetArrayAccessExpression(@NotNull ASTNode node) {
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
        return ReferenceProvidersRegistry.getReferencesFromProviders(this, PsiReferenceService.Hints.NO_HINTS);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitArrayAccessExpression(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitArrayAccessExpression(this, data);
    }

    @NotNull
    public JetExpression getArrayExpression() {
        JetExpression baseExpression = findChildByClass(JetExpression.class);
        assert baseExpression != null;
        return baseExpression;
    }

    @NotNull
    public List<JetExpression> getIndexExpressions() {
        PsiElement container = getIndicesNode();
        if (container == null) return Collections.emptyList();
        return PsiTreeUtil.getChildrenOfTypeAsList(container, JetExpression.class);
    }

    @NotNull
    public JetContainerNode getIndicesNode() {
        return (JetContainerNode) findChildByType(JetNodeTypes.INDICES);
    }
    
    public List<TextRange> getBracketRanges() {
        PsiElement lBracket = getIndicesNode().findChildByType(JetTokens.LBRACKET);
        PsiElement rBracket = getIndicesNode().findChildByType(JetTokens.RBRACKET);
        if (lBracket == null || rBracket == null) {
            return Collections.emptyList();
        }
        return Lists.newArrayList(lBracket.getTextRange(), rBracket.getTextRange());
    }
}
