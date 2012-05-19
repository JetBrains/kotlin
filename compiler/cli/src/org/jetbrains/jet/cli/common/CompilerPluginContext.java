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

package org.jetbrains.jet.cli.common;

import com.intellij.openapi.project.Project;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;

import java.util.List;

/**
 * Represents the context of available state in which a {@link CompilerPlugin} runs such as
 * the {@link Project}, the {@link BindingContext} and the underlying {@link JetFile} files.
 */
public class CompilerPluginContext {
    private final Project project;
    private final BindingContext context;
    private final List<JetFile> files;

    public CompilerPluginContext(Project project, BindingContext context, List<JetFile> files) {
        this.project = project;
        this.context = context;
        this.files = files;
    }

    public BindingContext getContext() {
        return context;
    }

    public List<JetFile> getFiles() {
        return files;
    }

    public Project getProject() {
        return project;
    }
}
