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
import com.intellij.openapi.util.UserDataHolderBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Evgeny Gerashchenko
 * @since 7/3/12
 */
public class CompilerConfiguration {
    private final UserDataHolderBase holder = new UserDataHolderBase();
    private boolean readOnly = false;

    @Nullable
    public <T> T get(@NotNull Key<T> key) {
        T data = holder.getUserData(key);
        return data == null ? null : unmodifiable(data);
    }

    @NotNull
    public <T> T get(@NotNull Key<T> key, @NotNull T defaultValue) {
        T data = holder.getUserData(key);
        return data == null ? defaultValue : unmodifiable(data);
    }

    @NotNull
    public <T> List<T> getList(@NotNull Key<List<T>> key) {
        List<T> data = holder.getUserData(key);
        if (data == null) {
            return Collections.emptyList();
        }
        else {
            return Collections.unmodifiableList(data);
        }
    }

    public <T> void put(@NotNull Key<T> key, @Nullable T value) {
        checkReadOnly();
        holder.putUserData(key, value);
    }

    public <T> void add(@NotNull Key<List<T>> key, @NotNull T value) {
        checkReadOnly();
        List<T> list = holder.putUserDataIfAbsent(key, new CopyOnWriteArrayList<T>());
        list.add(value);
    }

    public <T> void addAll(@NotNull Key<List<T>> key, @NotNull Collection<T> value) {
        checkReadOnly();
        List<T> list = holder.putUserDataIfAbsent(key, new CopyOnWriteArrayList<T>());
        list.addAll(value);
    }

    public CompilerConfiguration copy() {
        CompilerConfiguration copy = new CompilerConfiguration();
        holder.copyUserDataTo(copy.holder);
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
