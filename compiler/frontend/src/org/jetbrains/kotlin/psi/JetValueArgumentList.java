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
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.JetNodeTypes;
import org.jetbrains.kotlin.lexer.JetTokens;

import java.util.List;

public class JetValueArgumentList extends JetElementImpl {
    public JetValueArgumentList(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitValueArgumentList(this, data);
    }

    @NotNull
    public List<JetValueArgument> getArguments() {
        return findChildrenByType(JetNodeTypes.VALUE_ARGUMENT);
    }

    @Nullable
    public PsiElement getRightParenthesis() {
        return findChildByType(JetTokens.RPAR);
    }

    @Nullable
    public PsiElement getLeftParenthesis() {
        return findChildByType(JetTokens.LPAR);
    }

    @NotNull
    public JetValueArgument addArgument(@NotNull JetValueArgument argument) {
        return EditCommaSeparatedListHelper.INSTANCE$.addItem(this, getArguments(), argument);
    }

    @NotNull
    public JetValueArgument addArgumentAfter(@NotNull JetValueArgument argument, @Nullable JetValueArgument anchor) {
        return EditCommaSeparatedListHelper.INSTANCE$.addItemAfter(this, getArguments(), argument, anchor);
    }

    @NotNull
    public JetValueArgument addArgumentBefore(@NotNull JetValueArgument argument, @Nullable JetValueArgument anchor) {
        return EditCommaSeparatedListHelper.INSTANCE$.addItemBefore(this, getArguments(), argument, anchor);
    }

    public void removeArgument(@NotNull JetValueArgument argument) {
        EditCommaSeparatedListHelper.INSTANCE$.removeItem(argument);
    }
}
