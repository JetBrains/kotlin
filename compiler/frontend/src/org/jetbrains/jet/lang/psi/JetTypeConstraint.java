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
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

public class JetTypeConstraint extends JetElementImpl {
    public JetTypeConstraint(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitTypeConstraint(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitTypeConstraint(this, data);
    }

    public boolean isClassObjectContraint() {
        return findChildByType(JetTokens.CLASS_KEYWORD) != null &&
                findChildByType(JetTokens.OBJECT_KEYWORD) != null;
    }

    @Nullable @IfNotParsed
    public JetSimpleNameExpression getSubjectTypeParameterName() {
        return (JetSimpleNameExpression) findChildByType(JetNodeTypes.REFERENCE_EXPRESSION);
    }

    @Nullable @IfNotParsed
    public JetTypeReference getBoundTypeReference() {
        boolean passedColon = false;
        ASTNode node = getNode().getFirstChildNode();
        while (node != null) {
            IElementType tt = node.getElementType();
            if (tt == JetTokens.COLON) passedColon = true;
            if (passedColon && tt == JetNodeTypes.TYPE_REFERENCE) return (JetTypeReference) node.getPsi();
            node = node.getTreeNext();
        }

        return null;
    }
}
