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

import com.intellij.util.ArrayFactory;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;

/**
 * Represents a Kotlin declaration: a construct that introduces a named or structural entity, such as a class, function,
 * property, type alias, parameter, or type parameter.
 *
 * <p>This is the common base type for all declaration nodes in the Kotlin PSI. Because local declarations may appear
 * wherever a statement is allowed, a declaration is also a {@link KtExpression}. Every declaration owns a modifier list
 * ({@link KtModifierListOwner}) and may carry a preceding KDoc comment.
 *
 * @see KtNamedDeclaration a declaration that introduces a name
 * @see KtClassOrObject
 * @see KtFunction
 * @see KtProperty
 */
public interface KtDeclaration extends KtExpression, KtModifierListOwner {
    /**
     * A shared empty array, useful as a zero-length return value.
     */
    KtDeclaration[] EMPTY_ARRAY = new KtDeclaration[0];

    /**
     * A factory for creating arrays of {@link KtDeclaration}, used by the PSI child-access machinery.
     */
    ArrayFactory<KtDeclaration> ARRAY_FACTORY = count -> count == 0 ? EMPTY_ARRAY : new KtDeclaration[count];

    /**
     * Returns the KDoc comment attached to this declaration, or {@code null} if it has none.
     */
    @Nullable
    KDoc getDocComment();
}
