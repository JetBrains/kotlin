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

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.kotlin.lexer.JetTokens;

public class JetForExpression extends JetLoopExpression {
    public JetForExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitForExpression(this, data);
    }

    @Nullable
    public JetParameter getLoopParameter() {
        return (JetParameter) findChildByType(JetNodeTypes.VALUE_PARAMETER);
    }

    @Nullable
    public JetMultiDeclaration getMultiParameter() {
        return (JetMultiDeclaration) findChildByType(JetNodeTypes.MULTI_VARIABLE_DECLARATION);
    }

    @Nullable @IfNotParsed
    public JetExpression getLoopRange() {
        return findExpressionUnder(JetNodeTypes.LOOP_RANGE);
    }

    @Nullable @IfNotParsed
    public ASTNode getInKeywordNode() {
        return getNode().findChildByType(JetTokens.IN_KEYWORD);
    }
}
