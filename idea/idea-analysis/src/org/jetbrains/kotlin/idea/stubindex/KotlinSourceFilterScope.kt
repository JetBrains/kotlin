/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.stubindex

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil

class KotlinSourceFilterScope private constructor(
    delegate: GlobalSearchScope,
    private val includeProjectSourceFiles: Boolean,
    private val includeLibrarySourceFiles: Boolean,
    private val includeClassFiles: Boolean,
    private val includeScriptDependencies: Boolean,
    private val includeScriptsOutsideSourceRoots: Boolean,
    private val project: Project
) : DelegatingGlobalSearchScope(delegate) {

    private val index = ProjectRootManager.getInstance(project).fileIndex

    override fun getProject() = project

    override fun contains(file: VirtualFile): Boolean {
        if (!super.contains(file)) return false

        return ProjectRootsUtil.isInContent(
            project,
            file,
            includeProjectSourceFiles,
            includeLibrarySourceFiles,
            includeClassFiles,
            includeScriptDependencies,
            includeScriptsOutsideSourceRoots,
            index
        )
    }

    override fun toString(): String {
        return "KotlinSourceFilterScope(delegate=$myBaseScope src=$includeProjectSourceFiles libSrc=$includeLibrarySourceFiles " +
               "cls=$includeClassFiles script=$includeScriptDependencies)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        if (!super.equals(other)) return false

        other as KotlinSourceFilterScope

        if (includeProjectSourceFiles != other.includeProjectSourceFiles) return false
        if (includeLibrarySourceFiles != other.includeLibrarySourceFiles) return false
        if (includeClassFiles != other.includeClassFiles) return false
        if (includeScriptDependencies != other.includeScriptDependencies) return false
        if (project != other.project) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + includeProjectSourceFiles.hashCode()
        result = 31 * result + includeLibrarySourceFiles.hashCode()
        result = 31 * result + includeClassFiles.hashCode()
        result = 31 * result + includeScriptDependencies.hashCode()
        result = 31 * result + project.hashCode()
        return result
    }

    companion object {
        @JvmStatic
        fun sourcesAndLibraries(delegate: GlobalSearchScope, project: Project) = create(delegate, true, true, true, true, true, project)

        @JvmStatic
        fun sourceAndClassFiles(delegate: GlobalSearchScope, project: Project) = create(delegate, true, false, true, true, true, project)

        @JvmStatic
        fun projectSourceAndClassFiles(delegate: GlobalSearchScope, project: Project) = create(delegate, true, false, true, false, true, project)

        @JvmStatic
        fun projectSources(delegate: GlobalSearchScope, project: Project) = create(delegate, true, false, false, false, true, project)

        @JvmStatic
        fun librarySources(delegate: GlobalSearchScope, project: Project) = create(delegate, false, true, false, true, false, project)

        @JvmStatic
        fun libraryClassFiles(delegate: GlobalSearchScope, project: Project) = create(delegate, false, false, true, true, false, project)

        @JvmStatic
        fun projectAndLibrariesSources(delegate: GlobalSearchScope, project: Project) = create(delegate, true, true, false, false, true, project)

        private fun create(
                delegate: GlobalSearchScope,
                includeProjectSourceFiles: Boolean,
                includeLibrarySourceFiles: Boolean,
                includeClassFiles: Boolean,
                includeScriptDependencies: Boolean,
                includeScriptsOutsideSourceRoots: Boolean,
                project: Project
        ): GlobalSearchScope {
            if (delegate === GlobalSearchScope.EMPTY_SCOPE) return delegate

            if (delegate is KotlinSourceFilterScope) {
                return KotlinSourceFilterScope(
                        delegate.myBaseScope,
                        includeProjectSourceFiles = delegate.includeProjectSourceFiles && includeProjectSourceFiles,
                        includeLibrarySourceFiles = delegate.includeLibrarySourceFiles && includeLibrarySourceFiles,
                        includeClassFiles = delegate.includeClassFiles && includeClassFiles,
                        includeScriptDependencies = delegate.includeScriptDependencies && includeScriptDependencies,
                        includeScriptsOutsideSourceRoots = delegate.includeScriptsOutsideSourceRoots && includeScriptsOutsideSourceRoots,
                        project = project
                )
            }

            return KotlinSourceFilterScope(delegate, includeProjectSourceFiles, includeLibrarySourceFiles, includeClassFiles, includeScriptDependencies, includeScriptsOutsideSourceRoots, project)
        }
    }
}
