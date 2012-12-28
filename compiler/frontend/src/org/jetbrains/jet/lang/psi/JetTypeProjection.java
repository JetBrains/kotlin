/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

public class JetTypeProjection extends JetElementImpl implements JetModifierListOwner {
    public JetTypeProjection(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public JetModifierList getModifierList() {
        return (JetModifierList) findChildByType(JetNodeTypes.MODIFIER_LIST);
    }

    @Override
    public boolean hasModifier(JetToken modifier) {
        JetModifierList modifierList = getModifierList();
        return modifierList != null && modifierList.hasModifier(modifier);
    }

    @NotNull
    public JetProjectionKind getProjectionKind() {
        ASTNode projectionNode = getProjectionNode();
        if (projectionNode == null) return JetProjectionKind.NONE;

        if (projectionNode.getElementType() == JetTokens.IN_KEYWORD) return JetProjectionKind.IN;
        if (projectionNode.getElementType() == JetTokens.OUT_KEYWORD) return JetProjectionKind.OUT;
        if (projectionNode.getElementType() == JetTokens.MUL) return JetProjectionKind.STAR;

        throw new IllegalStateException(projectionNode.getText());
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitTypeProjection(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitTypeProjection(this, data);
    }

    @Nullable
    public JetTypeReference getTypeReference() {
        return (JetTypeReference) findChildByType(JetNodeTypes.TYPE_REFERENCE);
    }

    @Nullable
    public ASTNode getProjectionNode() {
        JetModifierList modifierList = getModifierList();
        if (modifierList != null) {
            ASTNode node = modifierList.getModifierNode(JetTokens.IN_KEYWORD);
            if (node != null) {
                return node;
            }
            node = modifierList.getModifierNode(JetTokens.OUT_KEYWORD);
            if (node != null) {
                return node;
            }
        }
        PsiElement star = findChildByType(JetTokens.MUL);
        if (star != null) {
            return star.getNode();
        }

        return null;
    }
}
