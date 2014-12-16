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

package org.jetbrains.jet.plugin.stubindex;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.caches.resolve.JsProjectDetector;
import org.jetbrains.jet.plugin.util.ProjectRootsUtil;

public class JetSourceFilterScope extends DelegatingGlobalSearchScope {
    @NotNull
    public static GlobalSearchScope kotlinSourcesAndLibraries(@NotNull GlobalSearchScope delegate, @NotNull Project project) {
        if (delegate instanceof JetSourceFilterScope) {
            return delegate;
        }
        return new JetSourceFilterScope(delegate, true, true, project);
    }

    @NotNull
    public static GlobalSearchScope kotlinSourceAndClassFiles(@NotNull GlobalSearchScope delegate, @NotNull Project project) {
        if (delegate instanceof JetSourceFilterScope) {
            delegate = ((JetSourceFilterScope) delegate).myBaseScope;
        }
        return new JetSourceFilterScope(delegate, false, true, project);
    }

    @NotNull
    public static GlobalSearchScope kotlinSources(@NotNull GlobalSearchScope delegate, @NotNull Project project) {
        if (delegate instanceof JetSourceFilterScope) {
            delegate = ((JetSourceFilterScope) delegate).myBaseScope;
        }
        return new JetSourceFilterScope(delegate, false, false, project);
    }

    private final ProjectFileIndex index;
    private final Project project;
    private final boolean includeLibrarySourceFiles;
    private final boolean includeClassFiles;
    private final boolean isJsProject;

    private JetSourceFilterScope(
            @NotNull GlobalSearchScope delegate,
            boolean includeLibrarySourceFiles,
            boolean includeClassFiles,
            @NotNull Project project
    ) {
        super(delegate);
        this.project = project;
        this.includeLibrarySourceFiles = includeLibrarySourceFiles;
        this.includeClassFiles = includeClassFiles;
        //NOTE: avoid recomputing in potentially bottleneck 'contains' method
        this.index = ProjectRootManager.getInstance(project).getFileIndex();
        this.isJsProject = JsProjectDetector.isJsProject(project);
    }

    @Override
    public boolean contains(@NotNull VirtualFile file) {
        if (!super.contains(file)) {
            return false;
        }

        return ProjectRootsUtil.isInContent(project, file, true, includeLibrarySourceFiles, includeClassFiles, index, isJsProject);
    }
}
