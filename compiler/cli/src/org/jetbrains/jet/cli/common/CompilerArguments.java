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

package org.jetbrains.jet.cli.common;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class CompilerArguments {
    @NotNull
    private List<CompilerPlugin> compilerPlugins = Lists.newArrayList();

    @NotNull
    public List<CompilerPlugin> getCompilerPlugins() {
        return compilerPlugins;
    }

    /**
     * Sets the compiler plugins to be used when working with the {@link org.jetbrains.jet.cli.CLICompiler}
     */
    public void setCompilerPlugins(@NotNull List<CompilerPlugin> compilerPlugins) {
        this.compilerPlugins = compilerPlugins;
    }

    public List<String> freeArgs = Lists.newArrayList();

    public abstract boolean isHelp();
    public abstract boolean isTags();
    public abstract boolean isVersion();
    public abstract boolean isVerbose();

    public abstract String getSrc();
}
