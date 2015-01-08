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

package org.jetbrains.kotlin.codegen.inline;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class InlineResult {

    private final Set<String> classesToRemove = new HashSet<String>();
    private final ReifiedTypeParametersUsages reifiedTypeParametersUsages = new ReifiedTypeParametersUsages();

    private InlineResult() {
    }

    @NotNull
    public static InlineResult create() {
        return new InlineResult();
    }

    @NotNull
    public InlineResult addAllClassesToRemove(@NotNull InlineResult child) {
        classesToRemove.addAll(child.classesToRemove);
        return this;
    }

    public void addClassToRemove(@NotNull String classInternalName) {
        classesToRemove.add(classInternalName);
    }

    @NotNull
    public Set<String> getClassesToRemove() {
        return classesToRemove;
    }

    @NotNull
    public ReifiedTypeParametersUsages getReifiedTypeParametersUsages() {
        return reifiedTypeParametersUsages;
    }
}
