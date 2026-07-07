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

import org.jetbrains.annotations.Nullable;

/**
 * Represents a declaration that can be assigned an initializer with {@code =}, such as a property, a local variable, or
 * an enum entry.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val greeting = "Hello"
 * //             ^_____^
 * // The initializer
 * }</pre>
 */
public interface KtDeclarationWithInitializer extends KtDeclaration {
    /**
     * Returns the initializer expression, or {@code null} if this declaration has none.
     */
    @Nullable
    KtExpression getInitializer();

    /**
     * Returns {@code true} if this declaration has an initializer expression.
     */
    boolean hasInitializer();
}
