/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.codeInspection.ex.Tools
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil

class WholeProjectJavaInspectionTest : WholeProjectInspectionTest() {

    override fun provideFiles(project: Project): Collection<VirtualFile> {
        val scope = object : DelegatingGlobalSearchScope(ProjectScope.getContentScope(project)) {
            val index = ProjectFileIndex.getInstance(myProject)
            override fun contains(file: VirtualFile): Boolean {
                if (!super.contains(file)) return false
                return ProjectRootsUtil.isInContent(
                    myProject,
                    file,
                    includeProjectSource = true,
                    includeLibrarySource = false,
                    includeLibraryClasses = false,
                    includeScriptDependencies = false,
                    fileIndex = index
                )
            }
        }

        return FileTypeIndex.getFiles(JavaFileType.INSTANCE, scope)
    }

    override fun isEnabledInspection(tools: Tools) = tools.tool.language in setOf(null, "java", "UAST", JavaLanguage.INSTANCE.id)
}