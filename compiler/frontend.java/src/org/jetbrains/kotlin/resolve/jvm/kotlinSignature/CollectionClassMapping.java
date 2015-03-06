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

package org.jetbrains.kotlin.resolve.jvm.kotlinSignature;

import com.google.common.collect.ImmutableBiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.platform.JavaToKotlinClassMapBuilder;

public class CollectionClassMapping extends JavaToKotlinClassMapBuilder {
    private static CollectionClassMapping instance = null;

    @NotNull
    public static CollectionClassMapping getInstance() {
        if (instance == null) {
            instance = new CollectionClassMapping();
        }
        return instance;
    }

    private ImmutableBiMap.Builder<ClassDescriptor, ClassDescriptor> mapBuilder = ImmutableBiMap.builder();
    private final ImmutableBiMap<ClassDescriptor, ClassDescriptor> mutableToReadOnlyMap;

    private CollectionClassMapping() {
        init();
        mutableToReadOnlyMap = mapBuilder.build();
        mapBuilder = null;
    }

    @Override
    protected void register(@NotNull Class<?> javaClass, @NotNull ClassDescriptor kotlinDescriptor, @NotNull Direction direction) {
        // do nothing
    }

    @Override
    protected void register(
            @NotNull Class<?> javaClass,
            @NotNull ClassDescriptor kotlinDescriptor,
            @NotNull ClassDescriptor kotlinMutableDescriptor
    ) {
        mapBuilder.put(kotlinMutableDescriptor, kotlinDescriptor);
    }

    public boolean isMutableCollection(@NotNull ClassDescriptor mutable) {
        return mutableToReadOnlyMap.containsKey(mutable);
    }

    public boolean isReadOnlyCollection(@NotNull ClassDescriptor immutable) {
        return mutableToReadOnlyMap.containsValue(immutable);
    }

    @NotNull
    public ClassDescriptor convertMutableToReadOnly(@NotNull ClassDescriptor mutable) {
        ClassDescriptor readOnly = mutableToReadOnlyMap.get(mutable);
        if (readOnly == null) {
            throw new IllegalArgumentException("Given class " + mutable + " is not a mutable collection");
        }
        return readOnly;
    }

    @NotNull
    public ClassDescriptor convertReadOnlyToMutable(@NotNull ClassDescriptor readOnly) {
        ClassDescriptor mutable = mutableToReadOnlyMap.inverse().get(readOnly);
        if (mutable == null) {
            throw new IllegalArgumentException("Given class " + readOnly + " is not a read-only collection");
        }
        return mutable;
    }
}
