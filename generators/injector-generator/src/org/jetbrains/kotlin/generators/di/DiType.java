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

package org.jetbrains.kotlin.generators.di;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class DiType {
    @NotNull
    private final Class<?> clazz;
    @NotNull
    private final List<DiType> typeParameters;

    public DiType(@NotNull Class<?> clazz, @NotNull List<DiType> typeParameters) {
        this.clazz = clazz;
        this.typeParameters = typeParameters;

        if (clazz.getTypeParameters().length != typeParameters.size()) {
            throw new IllegalStateException("type parameter count mismatch: " + clazz + ", " + typeParameters);
        }
    }

    public DiType(@NotNull Class<?> clazz) {
        this(clazz, Collections.<DiType>emptyList());
    }

    @NotNull
    public Class<?> getClazz() {
        return clazz;
    }

    @NotNull
    public List<DiType> getTypeParameters() {
        return typeParameters;
    }

    public boolean isAssignableFrom(@NotNull DiType that) {
        if (!this.clazz.isAssignableFrom(that.clazz)) {
            return false;
        }
        // TODO: following code incorrectly assumes that each type parameter
        // is projected into type parameter in the same position
        return this.typeParameters.equals(that.typeParameters);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DiType type = (DiType) o;

        if (!clazz.equals(type.clazz)) return false;
        if (!typeParameters.equals(type.typeParameters)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = clazz.hashCode();
        result = 31 * result + typeParameters.hashCode();
        return result;
    }

    @NotNull
    public static DiType fromReflectionType(@NotNull Type type) {
        if (type instanceof Class<?>) {
            return new DiType((Class) type);
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Class<?> owner = (Class<?>) parameterizedType.getRawType();
            List<DiType> diTypeParameters = Lists.newArrayList();
            for (Type typeParameter : parameterizedType.getActualTypeArguments()) {
                diTypeParameters.add(fromReflectionType(typeParameter));
            }
            return new DiType(owner, diTypeParameters);
        }
        throw new IllegalArgumentException("unsupported type: " + type);
    }

    @NotNull
    public static DiType collectionOf(@NotNull Class<?> type) {
        return collectionOf(new DiType(type));
    }

    @NotNull
    public static DiType collectionOf(@NotNull DiType type) {
        return new DiType(List.class, Lists.newArrayList(type));
    }

    @NotNull
    public static DiType listOf(@NotNull Class<?> type) {
        return collectionOf(new DiType(type));
    }

    @NotNull
    public static DiType listOf(@NotNull DiType type) {
        return new DiType(List.class, Lists.newArrayList(type));
    }

    @Override
    public String toString() {
        if (typeParameters.size() > 0) {
            return clazz + "<...>";
        }
        else {
            return clazz.toString();
        }
    }
}
