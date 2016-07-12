/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;

import java.util.List;

public abstract class DelegatingType implements KotlinType {
    protected abstract KotlinType getDelegate();

    @NotNull
    @Override
    public TypeConstructor getConstructor() {
        return getDelegate().getConstructor();
    }

    @NotNull
    @Override
    public List<TypeProjection> getArguments() {
        return getDelegate().getArguments();
    }

    @Override
    public boolean isMarkedNullable() {
        return getDelegate().isMarkedNullable();
    }

    @NotNull
    @Override
    public MemberScope getMemberScope() {
        return getDelegate().getMemberScope();
    }

    @Override
    public boolean isError() {
        return getDelegate().isError();
    }

    @NotNull
    @Override
    public Annotations getAnnotations() {
        return getDelegate().getAnnotations();
    }

    @Override
    @Nullable
    public <T extends TypeCapability> T getCapability(@NotNull Class<T> capabilityClass) {
        return getDelegate().getCapability(capabilityClass);
    }

    @NotNull
    @Override
    public TypeCapabilities getCapabilities() {
        return getDelegate().getCapabilities();
    }

    @Override
    public int hashCode() {
        return getDelegate().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof KotlinType)) return false;

        KotlinType type = (KotlinType) obj;
        return KotlinTypeChecker.FLEXIBLE_UNEQUAL_TO_INFLEXIBLE.equalTypes(this, type);
    }

    @Override
    public String toString() {
        return getDelegate().toString();
    }
}
