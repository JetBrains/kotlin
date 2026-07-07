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
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents the actual type syntax inside a {@link KtTypeReference}, stripped of any leading annotations and modifiers.
 *
 * <p>This is the common base type for the concrete type-syntax nodes, such as {@link KtUserType} ({@code Foo<Bar>}),
 * {@link KtNullableType} ({@code Foo?}), {@link KtFunctionType} ({@code (Int) -> String}), and {@link KtDynamicType}
 * ({@code dynamic}). A {@link KtTypeReference} wraps exactly one {@link KtTypeElement}.
 *
 * @see KtTypeReference
 */
public interface KtTypeElement extends KtElement {
    /**
     * A shared empty array, useful as a zero-length return value.
     */
    KtTypeElement[] EMPTY_ARRAY = new KtTypeElement[0];

    /**
     * A factory for creating arrays of {@link KtTypeElement}, used by the PSI child-access machinery.
     */
    ArrayFactory<KtTypeElement> ARRAY_FACTORY = count -> count == 0 ? EMPTY_ARRAY : new KtTypeElement[count];

    /**
     * Returns the type arguments of this type element as type references (for example, {@code Int} and {@code String}
     * for {@code Map<Int, String>}), or an empty list if there are none.
     *
     * <p>The list may contain {@code null} elements for malformed type arguments in incomplete code.
     */
    @NotNull
    List<KtTypeReference> getTypeArgumentsAsTypes();
}
