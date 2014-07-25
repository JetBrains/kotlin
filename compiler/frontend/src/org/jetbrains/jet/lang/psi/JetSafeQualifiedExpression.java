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
import org.jetbrains.jet.lexer.JetToken;

public class JetSafeQualifiedExpression extends JetExpressionImpl implements JetQualifiedExpression {
    public JetSafeQualifiedExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitSafeQualifiedExpression(this, data);
    }

    @NotNull
    @Override
    public JetExpression getReceiverExpression() {
        return JetQualifiedExpressionImpl.INSTANCE$.getReceiverExpression(this);
    }

    @Nullable
    @Override
    public JetExpression getSelectorExpression() {
        return JetQualifiedExpressionImpl.INSTANCE$.getSelectorExpression(this);
    }

    @NotNull
    @Override
    public ASTNode getOperationTokenNode() {
        return JetQualifiedExpressionImpl.INSTANCE$.getOperationTokenNode(this);
    }

    @NotNull
    @Override
    public JetToken getOperationSign() {
        return JetQualifiedExpressionImpl.INSTANCE$.getOperationSign(this);
    }
}
