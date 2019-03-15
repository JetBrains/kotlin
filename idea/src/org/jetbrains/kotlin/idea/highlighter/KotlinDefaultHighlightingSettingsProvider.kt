/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.daemon.impl.analysis.DefaultHighlightingSettingProvider
import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.internal.isKotlinDecompiledFile
import org.jetbrains.kotlin.idea.refactoring.toPsiFile
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.KtFile

class KotlinDefaultHighlightingSettingsProvider : DefaultHighlightingSettingProvider() {
    override fun getDefaultSetting(project: Project, file: VirtualFile): FileHighlightingSetting? {
        if (!file.isValid) {
            return null
        }

        return when {
            file.toPsiFile(project) !is KtFile -> null
            ProjectRootsUtil.isLibraryFile(project, file) -> FileHighlightingSetting.SKIP_INSPECTION
            file.isKotlinDecompiledFile -> FileHighlightingSetting.SKIP_HIGHLIGHTING
            else -> null
        }
    }
}