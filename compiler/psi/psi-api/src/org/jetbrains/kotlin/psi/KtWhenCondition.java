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

/**
 * Represents a single condition of a {@link KtWhenEntry} in a {@code when} expression.
 *
 * <p>This is the common base for the concrete node types {@link KtWhenConditionWithExpression} (a plain or equality
 * check), {@link KtWhenConditionInRange} (an {@code in}/{@code !in} check), and {@link KtWhenConditionIsPattern}
 * (an {@code is}/{@code !is} check). A single entry may list several comma-separated conditions.
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
    public KtWhenCondition(@NotNull ASTNode node) {
        super(node);
    }
}
