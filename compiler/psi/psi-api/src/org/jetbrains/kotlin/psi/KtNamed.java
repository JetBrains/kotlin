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
import org.jetbrains.kotlin.name.Name;

/**
 * Represents an element that carries a Kotlin {@link Name}, such as a named declaration or a label.
 *
 * <p>Unlike {@link com.intellij.psi.PsiNamedElement#getName()}, which returns a raw {@link String}, this interface
 * exposes the name as a structured {@link Name} that already accounts for backtick-quoted identifiers.
 */
public interface KtNamed {
    /**
     * Returns the name of this element as a {@link Name}, or {@code null} if the element is anonymous or its name is
     * missing (for example, in incomplete or erroneous code).
     */
    @Nullable
    Name getNameAsName();
}
