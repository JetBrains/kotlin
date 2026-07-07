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
import org.jetbrains.kotlin.KtNodeTypes;

/**
 * Represents a condition-driven loop: a {@code while} or {@code do}-{@code while} loop.
 *
 * <p>This is the common base for the concrete node types {@link KtWhileExpression} and {@link KtDoWhileExpression},
 * adding access to the loop condition on top of {@link KtLoopExpression}.
 */
public abstract class KtWhileExpressionBase extends KtLoopExpression {
    public KtWhileExpressionBase(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Returns the loop condition, or {@code null} if it is absent in incomplete code.
     */
    @Nullable
    @IfNotParsed
    public KtExpression getCondition() {
        return findExpressionUnder(KtNodeTypes.CONDITION);
    }
}
