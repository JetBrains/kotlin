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

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class InstantiateType implements Expression {

    private final DiType type;

    public InstantiateType(@NotNull DiType type) {
        this.type = type;
    }

    public InstantiateType(@NotNull Class<?> theClass) {
        this(new DiType(theClass));
    }

    @Override
    @NotNull
    public DiType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "[Instantiate type: " + getType() + "]";
    }

    @NotNull
    @Override
    public String renderAsCode() {
        throw new UnsupportedOperationException("This should be replaced by some concrete expression by the time this method is called");
    }

    @NotNull
    @Override
    public Collection<DiType> getTypesToImport() {
        throw new UnsupportedOperationException("This should be replaced by some concrete expression by the time this method is called");
    }
}
