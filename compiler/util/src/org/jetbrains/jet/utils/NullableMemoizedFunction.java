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

package org.jetbrains.jet.utils;

import com.intellij.util.Function;
import com.intellij.util.NullableFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public abstract class NullableMemoizedFunction<K, V> implements NullableFunction<K, V> {

    public static <K, V> NullableFunction<K, V> create(@NotNull final Function<K, V> compute) {
        return new NullableMemoizedFunction<K, V>() {
            @Nullable
            @Override
            protected V compute(@NotNull K input) {
                return compute.fun(input);
            }
        };
    }

    private final Map<K, Object> cache;

    public NullableMemoizedFunction(@NotNull Map<K, Object> map) {
        this.cache = map;
    }

    public NullableMemoizedFunction() {
        this(new HashMap<K, Object>());
    }

    @Override
    @Nullable
    public V fun(@NotNull K input) {
        Object value = cache.get(input);
        if (value != null) return Nulls.unescape(value);

        V typedValue = compute(input);

        Object oldValue = cache.put(input, Nulls.escape(typedValue));
        assert oldValue == null : "Race condition detected";

        return typedValue;
    }

    @Nullable
    protected abstract V compute(@NotNull K input);
}
