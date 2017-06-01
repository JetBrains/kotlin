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

package org.jetbrains.kotlin.codegen.serialization;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.util.slicedMap.*;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.Method;

public final class JvmSerializationBindings {
    public static final SerializationMappingSlice<FunctionDescriptor, Method> METHOD_FOR_FUNCTION =
            SerializationMappingSlice.create();
    public static final SerializationMappingSlice<PropertyDescriptor, Pair<Type, String>> FIELD_FOR_PROPERTY =
            SerializationMappingSlice.create();
    public static final SerializationMappingSlice<PropertyDescriptor, Method> SYNTHETIC_METHOD_FOR_PROPERTY =
            SerializationMappingSlice.create();

    private static final class SerializationMappingSlice<K, V> extends BasicWritableSlice<K, V> {
        public SerializationMappingSlice() {
            super(Slices.ONLY_REWRITE_TO_EQUAL, false);
        }

        @NotNull
        public static <K, V> SerializationMappingSlice<K, V> create() {
            return new SerializationMappingSlice<>();
        }
    }

    private static final class SerializationMappingSetSlice<K> extends SetSlice<K> {
        public SerializationMappingSetSlice() {
            super(Slices.ONLY_REWRITE_TO_EQUAL, false);
        }

        @NotNull
        public static <K> SerializationMappingSetSlice<K> create() {
            return new SerializationMappingSetSlice<>();
        }
    }

    static {
        BasicWritableSlice.initSliceDebugNames(JvmSerializationBindings.class);
    }

    private final MutableSlicedMap map = new SlicedMapImpl(false);

    public <K, V> void put(@NotNull SerializationMappingSlice<K, V> slice, @NotNull K key, @NotNull V value) {
        map.put(slice, key, value);
    }

    public <K> void put(@NotNull SerializationMappingSetSlice<K> slice, @NotNull K key) {
        map.put(slice, key, true);
    }

    @Nullable
    public <K, V> V get(@NotNull SerializationMappingSlice<K, V> slice, @NotNull K key) {
        return map.get(slice, key);
    }

    public <K> boolean get(@NotNull SerializationMappingSetSlice<K> slice, @NotNull K key) {
        return Boolean.TRUE.equals(map.get(slice, key));
    }
}
