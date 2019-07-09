/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Paths

object OutsidersPsiFileSupportUtils {
    fun getOutsiderFileOrigin(project: Project, virtualFile: VirtualFile): VirtualFile? {
        if (!OutsidersPsiFileSupportWrapper.isOutsiderFile(virtualFile)) return null

        val originalFilePath = OutsidersPsiFileSupportWrapper.getOriginalFilePath(virtualFile) ?: return null

        return generateSequence(VfsUtil.findFile(Paths.get(originalFilePath), true)) {
            if (it == project.baseDir) null else it.parent
        }.filter { it.exists() }.firstOrNull()
    }
}