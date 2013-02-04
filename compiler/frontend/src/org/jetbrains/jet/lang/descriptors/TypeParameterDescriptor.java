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

package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.Variance;

import java.util.Set;

public interface TypeParameterDescriptor extends ClassifierDescriptor {
    boolean isReified();

    Variance getVariance();

    @NotNull
    Set<JetType> getUpperBounds();

    @NotNull
    JetType getUpperBoundsAsType();

    @NotNull
    Set<JetType> getLowerBounds();

    @NotNull
    JetType getLowerBoundsAsType();

    @NotNull
    @Override
    TypeConstructor getTypeConstructor();

    @NotNull
    @Override
    @Deprecated // Use the static method TypeParameterDescriptor.substitute()
    TypeParameterDescriptor substitute(@NotNull TypeSubstitutor substitutor);

    int getIndex();
}
