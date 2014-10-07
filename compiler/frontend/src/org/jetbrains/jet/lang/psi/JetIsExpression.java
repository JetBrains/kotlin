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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

public class JetIsExpression extends JetExpressionImpl implements JetOperationExpression {
    public JetIsExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitIsExpression(this, data);
    }

    @NotNull
    public JetExpression getLeftHandSide() {
        return findChildByClass(JetExpression.class);
    }

    @Nullable @IfNotParsed
    public JetTypeReference getTypeReference() {
        return (JetTypeReference) findChildByType(JetNodeTypes.TYPE_REFERENCE);
    }

    @Override
    @NotNull
    public JetSimpleNameExpression getOperationReference() {
        return (JetSimpleNameExpression) findChildByType(JetNodeTypes.OPERATION_REFERENCE);
    }

    public boolean isNegated() {
        return getOperationReference().getReferencedNameElementType() == JetTokens.NOT_IS;
    }

}
