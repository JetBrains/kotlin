/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope

interface WholeProjectKotlinFileProvider : WholeProjectFileProvider {
    override fun provideFiles(project: Project): Collection<VirtualFile> {
        val scope = KotlinSourceFilterScope.projectSources(ProjectScope.getContentScope(project), project)
        return FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope)
    }
}