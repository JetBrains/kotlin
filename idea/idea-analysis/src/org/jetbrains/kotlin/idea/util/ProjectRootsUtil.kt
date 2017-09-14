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

package org.jetbrains.kotlin.idea.util

import com.intellij.ide.highlighter.ArchiveFileType
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.FileIndex
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import org.jetbrains.kotlin.idea.KotlinModuleFileType
import org.jetbrains.kotlin.idea.caches.resolve.JsProjectDetector
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.decompiler.builtIns.KotlinBuiltInFileType
import org.jetbrains.kotlin.idea.util.application.runReadAction

private val kotlinBinaries = listOf(JavaClassFileType.INSTANCE, KotlinBuiltInFileType, KotlinModuleFileType.INSTANCE)

fun VirtualFile.isKotlinBinary(): Boolean = fileType in kotlinBinaries

fun FileIndex.isInSourceContentWithoutInjected(file: VirtualFile): Boolean {
    return file !is VirtualFileWindow && isInSourceContent(file)
}

fun VirtualFile.getSourceRoot(project: Project): VirtualFile? = ProjectRootManager.getInstance(project).fileIndex.getSourceRootForFile(this)

val PsiFileSystemItem.sourceRoot: VirtualFile?
    get() = virtualFile?.getSourceRoot(project)

object ProjectRootsUtil {
    @JvmStatic fun isInContent(project: Project, file: VirtualFile, includeProjectSource: Boolean,
                               includeLibrarySource: Boolean, includeLibraryClasses: Boolean,
                               includeScriptDependencies: Boolean,
                               fileIndex: ProjectFileIndex = ProjectFileIndex.SERVICE.getInstance(project),
                               isJsProjectRef: Ref<Boolean?>? = null): Boolean {

        if (includeProjectSource && fileIndex.isInSourceContentWithoutInjected(file)) return true

        if (!includeLibraryClasses && !includeLibrarySource) return false

        // NOTE: the following is a workaround for cases when class files are under library source roots and source files are under class roots
        val canContainClassFiles = file.fileType == ArchiveFileType.INSTANCE || file.isDirectory
        val isBinary = file.isKotlinBinary()

        val scriptConfigurationManager = if (includeScriptDependencies) ScriptDependenciesManager.getInstance(project) else null

        if (includeLibraryClasses && (isBinary || canContainClassFiles)) {
            if (fileIndex.isInLibraryClasses(file)) return true
            if (scriptConfigurationManager?.getAllScriptsClasspathScope()?.contains(file) == true) return true
        }
        if (includeLibrarySource && !isBinary) {
            if (fileIndex.isInLibrarySource(file)) return true
            if (scriptConfigurationManager?.getAllLibrarySourcesScope()?.contains(file) == true) return true
        }

        if ((includeLibraryClasses && fileIndex.isInLibraryClasses(file)) ||
            (includeLibrarySource && fileIndex.isInLibrarySource(file))) {

            //NOTE: avoid computing isJsProject if redundant
            val isJsProject = isJsProjectRef?.get() ?: JsProjectDetector.isJsProject(project)
            isJsProjectRef?.set(isJsProject)
            return isJsProject
        }

        return false
    }

    @JvmStatic fun isInContent(
            element: PsiElement,
            includeProjectSource: Boolean,
            includeLibrarySource: Boolean,
            includeLibraryClasses: Boolean,
            includeScriptDependencies: Boolean
    ): Boolean {
        return runReadAction {
            val virtualFile = when (element) {
                                  is PsiDirectory -> element.virtualFile
                                  else -> element.containingFile?.virtualFile
                              } ?: return@runReadAction false

            val project = element.project
            return@runReadAction isInContent(project, virtualFile, includeProjectSource, includeLibrarySource, includeLibraryClasses, includeScriptDependencies)
        }
    }

    @JvmStatic fun isInProjectSource(element: PsiElement): Boolean {
        return isInContent(element, includeProjectSource = true, includeLibrarySource = false, includeLibraryClasses = false, includeScriptDependencies = false)
    }

    @JvmStatic fun isProjectSourceFile(project: Project, file: VirtualFile): Boolean {
        return isInContent(project, file, includeProjectSource = true, includeLibrarySource = false, includeLibraryClasses = false, includeScriptDependencies = false)
    }

    @JvmStatic fun isInProjectOrLibSource(element: PsiElement): Boolean {
        return isInContent(element, includeProjectSource = true, includeLibrarySource = true, includeLibraryClasses = false, includeScriptDependencies = false)
    }

    @JvmStatic fun isInProjectOrLibraryContent(element: PsiElement): Boolean {
        return isInContent(element, includeProjectSource = true, includeLibrarySource = true, includeLibraryClasses = true, includeScriptDependencies = true)
    }

    @JvmStatic fun isInProjectOrLibraryClassFile(element: PsiElement): Boolean {
        return isInContent(element, includeProjectSource = true, includeLibrarySource = false, includeLibraryClasses = true, includeScriptDependencies = false)
    }

    @JvmStatic fun isLibraryClassFile(project: Project, file: VirtualFile): Boolean {
        return isInContent(project, file, includeProjectSource = false, includeLibrarySource = false, includeLibraryClasses = true, includeScriptDependencies = true)
    }

    @JvmStatic fun isLibrarySourceFile(project: Project, file: VirtualFile): Boolean {
        return isInContent(project, file, includeProjectSource = false, includeLibrarySource = true, includeLibraryClasses = false, includeScriptDependencies = true)
    }

    @JvmStatic fun isLibraryFile(project: Project, file: VirtualFile): Boolean {
        return isInContent(project, file, includeProjectSource = false, includeLibrarySource = true, includeLibraryClasses = true, includeScriptDependencies = true)
    }
}

val Module.rootManager: ModuleRootManager
    get() = ModuleRootManager.getInstance(this)

val PsiElement.module: Module?
    get() = ModuleUtilCore.findModuleForPsiElement(this)
