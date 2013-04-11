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

import com.intellij.util.NotNullFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public abstract class NotNullMemoizedFunction<K, V> extends NullableMemoizedFunction<K, V>  implements NotNullFunction<K, V> {

    public static <K, V> NotNullFunction<K, V> create(@NotNull final NotNullFunction<K, V> compute) {
        return new NotNullMemoizedFunction<K, V>() {
            @NotNull
            @Override
            protected V compute(@NotNull K input) {
                return compute.fun(input);
            }
        };
    }

    public NotNullMemoizedFunction(@NotNull Map<K, Object> map) {
        super(map);
    }

    public NotNullMemoizedFunction() {
        super();
    }

    @NotNull
    @Override
    public V fun(@NotNull K input) {
        //noinspection ConstantConditions
        return super.fun(input);
    }

    @NotNull
    @Override
    protected abstract V compute(@NotNull K input);
}
