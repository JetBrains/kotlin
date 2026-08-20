/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;

/**
 * Represents a condition-driven loop: a {@code while} or {@code do}-{@code while} loop.
 *
 * <p>This is the common base for the concrete node types {@link KtWhileExpression} and {@link KtDoWhileExpression}, adding access to the
 * loop condition on top of {@link KtLoopExpression}.
 */
public abstract class KtWhileExpressionBase extends KtLoopExpression {
    @KtImplementationDetail
    public KtWhileExpressionBase(@NotNull ASTNode node) {
        super(node);
    }

    /** Returns the loop condition, or {@code null} if it is absent in incomplete code. */
    @Nullable
    @IfNotParsed
    public KtExpression getCondition() {
        return findExpressionUnder(KtNodeTypes.CONDITION);
    }
}
