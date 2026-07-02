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

import kotlin.annotations.jvm.ReadOnly;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents an element that contains a list of declarations, such as a file, a class body, or a block.
 *
 * <p>This interface groups only the directly nested declarations; it does not recurse into them, and it does not
 * include declarations synthesized by the compiler.
 */
public interface KtDeclarationContainer {
    /**
     * Returns the declarations directly contained in this element, in their source order. Returns an empty list if
     * there are none.
     */
    @NotNull
    @ReadOnly
    List<KtDeclaration> getDeclarations();
}
