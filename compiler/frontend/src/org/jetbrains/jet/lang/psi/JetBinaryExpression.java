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
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author max
 */
public class JetBinaryExpression extends JetExpressionImpl implements JetOperationExpression {
    public JetBinaryExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitBinaryExpression(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitBinaryExpression(this, data);
    }

    @NotNull
    public JetExpression getLeft() {
        JetExpression left = findChildByClass(JetExpression.class);
        assert left != null;
        return left;
    }

    @Nullable @IfNotParsed
    public JetExpression getRight() {
        ASTNode node = getOperationReference().getNode().getTreeNext();
        while (node != null) {
            PsiElement psi = node.getPsi();
            if (psi instanceof JetExpression) {
                return (JetExpression) psi;
            }
            node = node.getTreeNext();
        }

        return null;
    }

    @Override
    @NotNull
    public JetSimpleNameExpression getOperationReference() {
        return (JetSimpleNameExpression) findChildByType(JetNodeTypes.OPERATION_REFERENCE);
    }

    public IElementType getOperationToken() {
        return getOperationReference().getReferencedNameElementType();
    }
}
