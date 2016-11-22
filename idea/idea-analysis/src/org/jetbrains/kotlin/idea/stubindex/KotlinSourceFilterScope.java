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

package org.jetbrains.kotlin.idea.stubindex;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil;

public class KotlinSourceFilterScope extends DelegatingGlobalSearchScope {
    @NotNull
    public static GlobalSearchScope sourcesAndLibraries(@NotNull GlobalSearchScope delegate, @NotNull Project project) {
        return create(delegate, true, true, true, project);
    }

    @NotNull
    public static GlobalSearchScope sourceAndClassFiles(@NotNull GlobalSearchScope delegate, @NotNull Project project) {
        return create(delegate, true, false, true, project);
    }

    @NotNull
    public static GlobalSearchScope sources(@NotNull GlobalSearchScope delegate, @NotNull Project project) {
        return create(delegate, true, false, false, project);
    }

    @NotNull
    public static GlobalSearchScope librarySources(@NotNull GlobalSearchScope delegate, @NotNull Project project) {
        return create(delegate, false, true, false, project);
    }

    @NotNull
    public static GlobalSearchScope libraryClassFiles(@NotNull GlobalSearchScope delegate, @NotNull Project project) {
        return create(delegate, false, false, true, project);
    }

    @NotNull
    private static GlobalSearchScope create(
            @NotNull GlobalSearchScope delegate,
            boolean includeProjectSourceFiles,
            boolean includeLibrarySourceFiles,
            boolean includeClassFiles,
            @NotNull Project project
    ) {
        if (delegate == GlobalSearchScope.EMPTY_SCOPE) return delegate;

        if (delegate instanceof KotlinSourceFilterScope) {
            KotlinSourceFilterScope wrappedDelegate = (KotlinSourceFilterScope) delegate;

            boolean doIncludeProjectSourceFiles = wrappedDelegate.includeProjectSourceFiles && includeProjectSourceFiles;
            boolean doIncludeLibrarySourceFiles = wrappedDelegate.includeLibrarySourceFiles && includeLibrarySourceFiles;
            boolean doIncludeClassFiles = wrappedDelegate.includeClassFiles && includeClassFiles;

            return new KotlinSourceFilterScope(wrappedDelegate.myBaseScope,
                                               doIncludeProjectSourceFiles,
                                               doIncludeLibrarySourceFiles,
                                               doIncludeClassFiles,
                                               project);
        }

        return new KotlinSourceFilterScope(delegate, includeProjectSourceFiles, includeLibrarySourceFiles, includeClassFiles, project);
    }

    private final ProjectFileIndex index;
    private final Project project;
    private final boolean includeProjectSourceFiles;
    private final boolean includeLibrarySourceFiles;
    private final boolean includeClassFiles;

    //NOTE: avoid recomputing in potentially bottleneck 'contains' method
    private final Ref<Boolean> isJsProjectRef = new Ref<Boolean>();

    private KotlinSourceFilterScope(
            @NotNull GlobalSearchScope delegate,
            boolean includeProjectSourceFiles,
            boolean includeLibrarySourceFiles,
            boolean includeClassFiles,
            @NotNull Project project
    ) {
        super(delegate);
        this.project = project;
        this.includeProjectSourceFiles = includeProjectSourceFiles;
        this.includeLibrarySourceFiles = includeLibrarySourceFiles;
        this.includeClassFiles = includeClassFiles;
        this.index = ProjectRootManager.getInstance(project).getFileIndex();
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public boolean contains(@NotNull VirtualFile file) {
        if (!super.contains(file)) return false;
        return ProjectRootsUtil.isInContent(
                project, file, includeProjectSourceFiles, includeLibrarySourceFiles, includeClassFiles, index, isJsProjectRef
        );
    }

    @Override
    public String toString() {
        return "KotlinSourceFilterScope(delegate=" + myBaseScope + " src=" + includeProjectSourceFiles + " libSrc=" + includeLibrarySourceFiles +
               "cls=" + includeClassFiles;
    }
}
