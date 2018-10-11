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
import org.jetbrains.kotlin.lexer.KtTokens;

public class KtPropertyDelegate extends KtElementImpl {
    public KtPropertyDelegate(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    public KtExpression getExpression() {
        return findChildByClass(KtExpression.class);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitPropertyDelegate(this, data);
    }

    @NotNull
    public ASTNode getByKeywordNode() {
        //noinspection ConstantConditions
        return getNode().findChildByType(KtTokens.BY_KEYWORD);
    }
}
