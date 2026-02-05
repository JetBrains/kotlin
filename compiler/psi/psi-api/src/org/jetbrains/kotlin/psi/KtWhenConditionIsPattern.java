/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;

/**
 * Represents a {@code when} condition that performs a type check using {@code is} or {@code !is}.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * when (x) {
 *     is String -> println(x.length)
 * //  ^_______^
 * }
 * }</pre>
 */
public class KtWhenConditionIsPattern extends KtWhenCondition {
    public KtWhenConditionIsPattern(@NotNull ASTNode node) {
        super(node);
    }

    public boolean isNegated() {
        return getNode().findChildByType(KtTokens.NOT_IS) != null;
    }

    @Nullable @IfNotParsed
    public KtTypeReference getTypeReference() {
        return (KtTypeReference) findChildByType(KtNodeTypes.TYPE_REFERENCE);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitWhenConditionIsPattern(this, data);
    }
}
