/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.Annotated;

import java.util.Collection;
import java.util.List;

public interface TypeConstructor extends Annotated {
    @NotNull
    @ReadOnly
    List<TypeParameterDescriptor> getParameters();

    @NotNull
    @ReadOnly
    Collection<JetType> getSupertypes();

    /**
     * Cannot have subtypes.
     */
    boolean isFinal();

    /**
     * If the type is non-denotable, it can't be written in code directly, it only can appear internally inside a type checker.
     * Examples: intersection type or number value type.
     */
    boolean isDenotable();

    @Nullable
    ClassifierDescriptor getDeclarationDescriptor();
}
