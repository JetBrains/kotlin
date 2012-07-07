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

/**
 * @author Evgeny Gerashchenko
 * @since 7/3/12
 */
public class CompilerConfiguration {
    private final UserDataHolderBase holder = new UserDataHolderBase();

    public <T> T get(@NotNull Key<T> key) {
        return holder.getUserData(key);
    }

    public <T> void put(@NotNull Key<T> key, @Nullable T value) {
        holder.putUserData(key, value);
    }
}
