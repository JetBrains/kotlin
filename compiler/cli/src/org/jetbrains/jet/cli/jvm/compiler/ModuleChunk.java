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

package org.jetbrains.jet.cli.jvm.compiler;

import com.google.common.collect.Maps;
import jet.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ModuleChunk {

    public static final ModuleChunk EMPTY = new ModuleChunk(Collections.<Module>emptyList());

    private final List<Module> modules;
    private final Map<File, Module> sourceFileToModule = Maps.newHashMap();

    public ModuleChunk(@NotNull List<Module> modules) {
        this.modules = modules;
        for (Module module : modules) {
            for (String file : module.getSourceFiles()) {
                sourceFileToModule.put(new File(file).getAbsoluteFile(), module);
            }
        }
    }

    @NotNull
    public List<Module> getModules() {
        return modules;
    }

    @Nullable
    public Module findModuleBySourceFile(@NotNull File sourceFile) {
        return sourceFileToModule.get(sourceFile.getAbsoluteFile());
    }
}
