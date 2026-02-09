/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a {@code when} condition that matches against an expression value.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * when (x) {
 *     1 -> "one"
 * //  ^
 * }
 * }</pre>
 */
public class KtWhenConditionWithExpression extends KtWhenCondition {
    public KtWhenConditionWithExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    @IfNotParsed
    public KtExpression getExpression() {
        return findChildByClass(KtExpression.class);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitWhenConditionWithExpression(this, data);
    }
}
