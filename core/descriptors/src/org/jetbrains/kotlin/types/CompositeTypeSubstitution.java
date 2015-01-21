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

package org.jetbrains.kotlin.types;

import org.jetbrains.annotations.NotNull;

public class CompositeTypeSubstitution extends TypeSubstitution {
    private final TypeSubstitution[] inner;

    public CompositeTypeSubstitution(@NotNull TypeSubstitution... inner) {
        this.inner = inner;
    }

    @Override
    public TypeProjection get(TypeConstructor key) {
        for (TypeSubstitution substitution : inner) {
            TypeProjection value = substitution.get(key);
            if (value != null) return value;
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        for (TypeSubstitution substitution : inner) {
            if (!substitution.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public boolean approximateCapturedTypes() {
        for (TypeSubstitution substitution : inner) {
            if (substitution.approximateCapturedTypes()) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (TypeSubstitution substitution : inner) {
            builder.append(substitution).append(" * ");
        }
        return builder.toString();
    }
}
