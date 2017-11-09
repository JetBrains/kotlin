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

package org.jetbrains.kotlin.cli.common.modules;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.modules.Module;

import java.util.Collections;
import java.util.List;

public class ModuleScriptData {
    public static final ModuleScriptData EMPTY = new ModuleScriptData(Collections.emptyList());

    private final List<Module> modules;

    public ModuleScriptData(@NotNull List<Module> modules) {
        this.modules = modules;
    }

    @NotNull
    public List<Module> getModules() {
        return modules;
    }
}
