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

import com.intellij.compiler.impl.javaCompiler.OutputItemImpl;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.compiler.runner.AbstractOutputItemCollector;
import org.jetbrains.jet.compiler.runner.CompilerEnvironment;

import java.io.File;

/**
 * @author Pavel Talanov
 */
public final class CompilerUtils {
    private CompilerUtils() {
    }

    @NotNull
    public static CompilerEnvironment getEnvironmentFor(@NotNull CompileContext compileContext, @NotNull Module module, boolean tests) {
        VirtualFile mainOutput = compileContext.getModuleOutputDirectory(module);
        VirtualFile outputDirectoryForTests = compileContext.getModuleOutputDirectoryForTests(module);
        return CompilerEnvironment.getEnvironmentFor(tests, toIoFile(mainOutput), toIoFile(outputDirectoryForTests));
    }

    @Nullable
    public static File toIoFile(@Nullable VirtualFile file) {
        if (file == null) return null;
        return new File(file.getPath());
    }

    public static class OutputItemsCollectorImpl extends AbstractOutputItemCollector<VirtualFile, TranslatingCompiler.OutputItem> {

        public OutputItemsCollectorImpl(@NotNull String outputPath) {
            super(outputPath);
        }

        @Override
        protected TranslatingCompiler.OutputItem convertResult(String resultPath, VirtualFile correspondingSource) {
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(resultPath));
            return new OutputItemImpl(resultPath, correspondingSource);
        }

        @Override
        protected VirtualFile convertSource(String sourcePath) {
            return LocalFileSystem.getInstance().findFileByPath(sourcePath);
        }
    }
}

