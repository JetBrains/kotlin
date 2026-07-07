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

/**
 * Represents a variable declaration introduced by {@code val} or {@code var}: a property, a local variable, or an entry
 * of a destructuring declaration.
 *
 * <p>This is the common base type for the concrete node types {@link KtProperty} and
 * {@link KtDestructuringDeclarationEntry}. A variable may have a declared type and an initializer.
 */
public interface KtVariableDeclaration extends KtCallableDeclaration, KtDeclarationWithInitializer, KtValVarKeywordOwner {
    /**
     * Returns {@code true} if this variable is mutable (declared with {@code var}), or {@code false} if it is read-only
     * (declared with {@code val}).
     */
    boolean isVar();
}
