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

package org.jetbrains.kotlin.config;

import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class CompilerConfigurationKey<T> {
    Key<T> ideaKey;

    public CompilerConfigurationKey(@NotNull @NonNls String name) {
        ideaKey = Key.create(name);
    }

    @NotNull
    public static <T> CompilerConfigurationKey<T> create(@NotNull @NonNls String name) {
        return new CompilerConfigurationKey<>(name);
    }

    @Override
    public String toString() {
        return ideaKey.toString();
    }
}
