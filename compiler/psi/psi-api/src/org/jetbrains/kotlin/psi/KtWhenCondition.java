/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a single condition of a {@link KtWhenEntry} in a {@code when} expression.
 *
 * <p>This is the common base for the concrete node types {@link KtWhenConditionWithExpression} (a plain or equality check),
 * {@link KtWhenConditionInRange} (an {@code in}/{@code !in} check), and {@link KtWhenConditionIsPattern} (an {@code is}/{@code !is} check).
 * A single entry may list several comma-separated conditions.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * when (x) {
 *     1, 2 -> "small"
 * //  ^  ^
 * // Two 'with expression' conditions
 *     in 3..10 -> "medium"
 * //  ^______^
 * // An 'in range' condition
 *     is String -> "text"
 * //  ^_______^
 * // An 'is pattern' condition
 * }
 * }</pre>
 */
public abstract class KtWhenCondition extends KtElementImpl {
    @KtImplementationDetail
    public KtWhenCondition(@NotNull ASTNode node) {
        super(node);
    }
}
