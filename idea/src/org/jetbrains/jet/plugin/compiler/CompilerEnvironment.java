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

package org.jetbrains.jet.plugin.compiler;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.utils.PathUtil;

import java.io.*;

import static com.intellij.openapi.compiler.CompilerMessageCategory.ERROR;

/**
 * @author Pavel Talanov
 */
public final class CompilerEnvironment {

    @NotNull
    public static CompilerEnvironment getEnvironmentFor(@NotNull CompileContext compileContext, @NotNull Module module, boolean tests) {
        VirtualFile mainOutput = compileContext.getModuleOutputDirectory(module);
        final VirtualFile outputDir = tests ? compileContext.getModuleOutputDirectoryForTests(module) : mainOutput;
        File kotlinHome = PathUtil.getDefaultCompilerPath();
        return new CompilerEnvironment(kotlinHome, outputDir);
    }

    @Nullable
    private final File kotlinHome;
    @Nullable
    private final VirtualFile output;

    public CompilerEnvironment(@Nullable File home, @Nullable VirtualFile output) {
        this.kotlinHome = home;
        this.output = output;
    }

    public boolean success() {
        return kotlinHome != null && output != null;
    }

    @NotNull
    public File getKotlinHome() {
        assert kotlinHome != null;
        return kotlinHome;
    }

    @NotNull
    public VirtualFile getOutput() {
        assert output != null;
        return output;
    }

    public void reportErrorsTo(@NotNull CompileContext compileContext) {
        if (output == null) {
            compileContext.addMessage(ERROR, "[Internal Error] No output directory", "", -1, -1);
        }
        if (kotlinHome == null) {
            compileContext.addMessage(ERROR, "Cannot find kotlinc home. Make sure plugin is properly installed", "", -1, -1);
        }
    }

}
