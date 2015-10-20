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
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.Collection;

public interface ValueParameterDescriptor extends VariableDescriptor, ParameterDescriptor {
    @NotNull
    @Override
    CallableDescriptor getContainingDeclaration();

    /**
     * Returns the 0-based index of the value parameter in the parameter list of its containing function.
     *
     * @return the parameter index
     */
    int getIndex();

    /**
     * @return true iff this parameter belongs to a declared function (not a fake override) and declares the default value,
     * i.e. explicitly specifies it in the function signature. Also see 'hasDefaultValue' extension in DescriptorUtils.kt
     */
    boolean declaresDefaultValue();

    @Nullable
    KotlinType getVarargElementType();

    @NotNull
    @Override
    ValueParameterDescriptor getOriginal();

    @NotNull
    ValueParameterDescriptor copy(@NotNull CallableDescriptor newOwner, @NotNull Name newName);

    /**
     * Parameter p1 overrides p2 iff
     * a) their respective owners (function declarations) f1 override f2
     * b) p1 and p2 have the same indices in the owners' parameter lists
     */
    @NotNull
    @Override
    Collection<? extends ValueParameterDescriptor> getOverriddenDescriptors();

    boolean isCrossinline();

    boolean isNoinline();
}
