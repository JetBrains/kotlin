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

package org.jetbrains.kotlin.idea.util

import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiElement
import kotlin.platform.platformStatic
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.configuration.JetModuleTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.ide.highlighter.JavaClassFileType
import org.jetbrains.kotlin.idea.caches.resolve.JsProjectDetector

public object ProjectRootsUtil {
    platformStatic
    public fun isInContent(project: Project, file: VirtualFile, includeProjectSource: Boolean,
                           includeLibrarySource: Boolean, includeLibraryClasses: Boolean,
                           fileIndex: ProjectFileIndex = ProjectFileIndex.SERVICE.getInstance(project),
                           isJsProject: Boolean? = null): Boolean {
        if (includeProjectSource && fileIndex.isInSourceContent(file)) {
            return true
        }
        if (!includeLibraryClasses && !includeLibrarySource) return false

        //NOTE: avoid computing isJsProject if redundant
        if (isJsProject ?: JsProjectDetector.isJsProject(project)) {
            return (includeLibrarySource && fileIndex.isInLibrarySource(file))
                   || (includeLibraryClasses && fileIndex.isLibraryClassFile(file))
        }
        // NOTE: the following is a workaround for cases when class files are under library source roots and source files are under class roots
        val isClassFile = file.getFileType() == JavaClassFileType.INSTANCE
        return (includeLibraryClasses && isClassFile && fileIndex.isInLibraryClasses(file))
               || (includeLibrarySource && !isClassFile && fileIndex.isInLibrarySource(file))
    }

    platformStatic
    public fun isInContent(
            element: PsiElement,
            includeProjectSource: Boolean,
            includeLibrarySource: Boolean,
            includeLibraryClasses: Boolean
    ): Boolean {
        return runReadAction {(): Boolean ->
            val virtualFile = when (element) {
                                  is PsiDirectory -> element.getVirtualFile()
                                  else -> element.getContainingFile()?.getVirtualFile()
                              } ?: return@runReadAction false

            val project = element.getProject()
            return@runReadAction isInContent(project, virtualFile, includeProjectSource, includeLibrarySource, includeLibraryClasses)
        }
    }

    platformStatic
    public fun isInProjectSource(element: PsiElement): Boolean {
        return isInContent(element, includeProjectSource = true, includeLibrarySource = false, includeLibraryClasses = false)
    }

    platformStatic
    public fun isInProjectOrLibSource(element: PsiElement): Boolean {
        return isInContent(element, includeProjectSource = true, includeLibrarySource = true, includeLibraryClasses = false)
    }

    platformStatic
    public fun isInProjectOrLibraryContent(element: PsiElement): Boolean {
        return isInContent(element, includeProjectSource = true, includeLibrarySource = true, includeLibraryClasses = true)
    }

    platformStatic
    public fun isLibraryClassFile(project: Project, file: VirtualFile): Boolean {
        return isInContent(project, file, includeProjectSource = false, includeLibrarySource = false, includeLibraryClasses = true)
    }

    platformStatic
    public fun isLibraryFile(project: Project, file: VirtualFile): Boolean {
        return isInContent(project, file, includeProjectSource = false, includeLibrarySource = true, includeLibraryClasses = true)
    }
}
