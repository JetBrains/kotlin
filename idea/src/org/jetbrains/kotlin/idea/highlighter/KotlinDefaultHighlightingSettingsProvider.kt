/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.daemon.impl.analysis.DefaultHighlightingSettingProvider
import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil

class KotlinDefaultHighlightingSettingsProvider : DefaultHighlightingSettingProvider() {
    override fun getDefaultSetting(project: Project, file: VirtualFile): FileHighlightingSetting? {
        return when {
            ProjectRootsUtil.isLibraryFile(project, file) -> FileHighlightingSetting.SKIP_INSPECTION
            else -> null
        }
    }
}