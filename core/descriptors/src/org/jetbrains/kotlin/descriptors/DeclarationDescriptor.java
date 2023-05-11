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

package org.jetbrains.kotlin.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.annotations.Annotated;
import org.jetbrains.kotlin.mpp.DeclarationSymbolMarker;

public interface DeclarationDescriptor extends Annotated, Named, ValidateableDescriptor, DeclarationSymbolMarker {
    /**
     * @return The descriptor that corresponds to the original declaration of this element.
     *         A descriptor can be obtained from its original by substituting type arguments (of the declaring class
     *         or of the element itself).
     *         returns <code>this</code> object if the current descriptor is original itself
     */
    @NotNull
    DeclarationDescriptor getOriginal();

    @Nullable
    DeclarationDescriptor getContainingDeclaration();

    <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data);

    void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor);
}
