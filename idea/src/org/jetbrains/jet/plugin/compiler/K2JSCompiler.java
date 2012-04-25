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
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Chunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.project.JsModuleDetector;

/**
 * @author Pavel Talanov
 */
public final class K2JSCompiler implements TranslatingCompiler {

    @Override
    public boolean isCompilableFile(VirtualFile file, CompileContext context) {
        if (!(file.getFileType() instanceof JetFileType)) {
            return false;
        }
        Project project = context.getProject();
        if (project == null) {
            return false;
        }
        return JsModuleDetector.isJsProject(project);
    }

    @Override
    public void compile(final CompileContext context, Chunk<Module> moduleChunk, final VirtualFile[] files, OutputSink sink) {
        context.addMessage(CompilerMessageCategory.INFORMATION, "Some useful code will be there", null, -1, -1);
        //TODO:
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Kotlin to JavaScript compiler";
    }

    @Override
    public boolean validateConfiguration(CompileScope scope) {
        return true;
    }
}
