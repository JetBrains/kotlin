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

package org.jetbrains.kotlin.idea.stubindex

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil

class KotlinSourceFilterScope private constructor(
        delegate: GlobalSearchScope,
        private val includeProjectSourceFiles: Boolean,
        private val includeLibrarySourceFiles: Boolean,
        private val includeClassFiles: Boolean,
        private val project: Project) : DelegatingGlobalSearchScope(delegate) {

    private val index = ProjectRootManager.getInstance(project).fileIndex

    //NOTE: avoid recomputing in potentially bottleneck 'contains' method
    private val isJsProjectRef = Ref<Boolean?>(null)

    override fun getProject(): Project? {
        return project
    }

    override fun contains(file: VirtualFile): Boolean {
        if (!super.contains(file)) return false
        return ProjectRootsUtil.isInContent(
                project, file, includeProjectSourceFiles, includeLibrarySourceFiles, includeClassFiles, index, isJsProjectRef)
    }

    companion object {
        fun sourcesAndLibraries(delegate: GlobalSearchScope, project: Project): GlobalSearchScope {
            return create(delegate, true, true, true, project)
        }

        fun sourceAndClassFiles(delegate: GlobalSearchScope, project: Project): GlobalSearchScope {
            return create(delegate, true, false, true, project)
        }

        fun sources(delegate: GlobalSearchScope, project: Project): GlobalSearchScope {
            return create(delegate, true, false, false, project)
        }

        fun librarySources(delegate: GlobalSearchScope, project: Project): GlobalSearchScope {
            return create(delegate, false, true, false, project)
        }

        fun libraryClassFiles(delegate: GlobalSearchScope, project: Project): GlobalSearchScope {
            return create(delegate, false, false, true, project)
        }

        private fun create(
                delegate: GlobalSearchScope,
                includeProjectSourceFiles: Boolean,
                includeLibrarySourceFiles: Boolean,
                includeClassFiles: Boolean,
                project: Project
        ): GlobalSearchScope {
            if (delegate === GlobalSearchScope.EMPTY_SCOPE) return delegate

            if (delegate is KotlinSourceFilterScope) {

                val doIncludeProjectSourceFiles = delegate.includeProjectSourceFiles && includeProjectSourceFiles
                val doIncludeLibrarySourceFiles = delegate.includeLibrarySourceFiles && includeLibrarySourceFiles
                val doIncludeClassFiles = delegate.includeClassFiles && includeClassFiles

                return KotlinSourceFilterScope(delegate.myBaseScope,
                                               doIncludeProjectSourceFiles,
                                               doIncludeLibrarySourceFiles,
                                               doIncludeClassFiles,
                                               project)
            }

            return KotlinSourceFilterScope(delegate, includeProjectSourceFiles, includeLibrarySourceFiles, includeClassFiles, project)
        }
    }
}
