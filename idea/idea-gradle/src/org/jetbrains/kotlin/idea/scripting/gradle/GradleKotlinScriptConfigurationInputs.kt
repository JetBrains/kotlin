/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.cache.CachedConfigurationInputs
import org.jetbrains.kotlin.psi.KtFile

data class GradleKotlinScriptConfigurationInputs(
    val buildScriptAndPluginsSections: String,
    val timeStamp: Long
) : CachedConfigurationInputs {
    override fun isUpToDate(project: Project, file: VirtualFile, ktFile: KtFile?): Boolean {
        val actualStamp = getGradleScriptInputsStamp(project, file, ktFile) ?: return false
        return actualStamp.buildScriptAndPluginsSections == this.buildScriptAndPluginsSections
                && project.service<GradleScriptInputsWatcher>().areAffectedFilesUpToDate(file, timeStamp)
    }
}