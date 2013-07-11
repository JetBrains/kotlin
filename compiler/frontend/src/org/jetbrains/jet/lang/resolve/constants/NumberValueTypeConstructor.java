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

package org.jetbrains.jet.lang.resolve.constants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeProjection;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class NumberValueTypeConstructor implements TypeConstructor {
    private final Long value;

    public NumberValueTypeConstructor(Long value) {
        this.value = value;
    }

    public Long getValue() {
        return value;
    }

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getParameters() {
        throw new IllegalStateException();
    }

    @NotNull
    @Override
    public Collection<JetType> getSupertypes() {
        throw new IllegalStateException();
    }

    @Override
    public boolean isSealed() {
        throw new IllegalStateException();
    }

    @Override
    public boolean isDenotable() {
        return false;
    }

    @Nullable
    @Override
    public ClassifierDescriptor getDeclarationDescriptor() {
        throw new IllegalStateException();
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        throw new IllegalStateException();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NumberValueTypeConstructor type = (NumberValueTypeConstructor) o;

        if (!value.equals(type.value)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
