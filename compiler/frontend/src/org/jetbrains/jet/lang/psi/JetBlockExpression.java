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
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifiableCodeBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Arrays;
import java.util.List;

/**
 * @author max
 */
public class JetBlockExpression extends JetExpressionImpl implements JetStatementExpression, PsiModifiableCodeBlock {
    public JetBlockExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public boolean shouldChangeModificationCount(PsiElement place) {
        return JetModifiableBlockHelper.shouldChangeModificationCount(place);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitBlockExpression(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitBlockExpression(this, data);
    }

    @NotNull
    public List<JetElement> getStatements() {
        return Arrays.asList(findChildrenByClass(JetElement.class));
    }

    @Nullable
    public TextRange getLastBracketRange() {
        PsiElement rBrace = findChildByType(JetTokens.RBRACE);
        return rBrace != null ? rBrace.getTextRange() : null;
    }
}
