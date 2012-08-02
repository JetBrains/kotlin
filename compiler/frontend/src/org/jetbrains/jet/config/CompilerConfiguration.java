/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.config;

import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Evgeny Gerashchenko
 * @since 7/3/12
 */
public class CompilerConfiguration {
    private final Map<Key, Object> map = new HashMap<Key, Object>();
    private boolean readOnly = false;

    @Nullable
    public <T> T get(@NotNull CompilerConfigurationKey<T> key) {
        T data = (T) map.get(key.ideaKey);
        return data == null ? null : unmodifiable(data);
    }

    @NotNull
    public <T> T get(@NotNull CompilerConfigurationKey<T> key, @NotNull T defaultValue) {
        T data = (T) map.get(key.ideaKey);
        return data == null ? defaultValue : unmodifiable(data);
    }

    @NotNull
    public <T> List<T> getList(@NotNull CompilerConfigurationKey<List<T>> key) {
        List<T> data = (List<T>) map.get(key.ideaKey);
        if (data == null) {
            return Collections.emptyList();
        }
        else {
            return Collections.unmodifiableList(data);
        }
    }

    public <T> void put(@NotNull CompilerConfigurationKey<T> key, @Nullable T value) {
        checkReadOnly();
        map.put(key.ideaKey, value);
    }

    public <T> void add(@NotNull CompilerConfigurationKey<List<T>> key, @NotNull T value) {
        checkReadOnly();
        Key<List<T>> ideaKey = key.ideaKey;
        if (map.get(ideaKey) == null) {
            map.put(ideaKey, new ArrayList<T>());
        }
        List<T> list = (List<T>) map.get(ideaKey);
        list.add(value);
    }

    public <T> void addAll(@NotNull CompilerConfigurationKey<List<T>> key, @NotNull Collection<T> values) {
        checkReadOnly();
        Key<List<T>> ideaKey = key.ideaKey;
        if (map.get(ideaKey) == null) {
            map.put(ideaKey, new ArrayList<T>());
        }
        List<T> list = (List<T>) map.get(ideaKey);
        list.addAll(values);
    }

    public CompilerConfiguration copy() {
        CompilerConfiguration copy = new CompilerConfiguration();
        copy.map.putAll(map);
        return copy;
    }

    private void checkReadOnly() {
        if (readOnly) {
            throw new IllegalStateException("CompilerConfiguration is read-only");
        }
    }

    public void setReadOnly(boolean readOnly) {
        if (readOnly != this.readOnly) {
            checkReadOnly();
            this.readOnly = readOnly;
        }
    }

    @NotNull
    private static <T> T unmodifiable(@NotNull T object) {
        if (object instanceof List) {
            return (T) Collections.unmodifiableList((List) object);
        }
        else if (object instanceof Map) {
            return (T) Collections.unmodifiableMap((Map) object);
        }
        else if (object instanceof Collection) {
            return (T) Collections.unmodifiableCollection((Collection) object);
        }
        else {
            return object;
        }
    }
}
