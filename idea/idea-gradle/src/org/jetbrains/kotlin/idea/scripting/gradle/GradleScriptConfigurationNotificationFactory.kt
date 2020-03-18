/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationNotificationFactory

class GradleScriptConfigurationNotificationFactory : ScriptConfigurationNotificationFactory {
    override fun showNotification(file: VirtualFile, project: Project, onClick: () -> Unit): Boolean {
        if (isGradleKotlinScript(file) && isGradleImportCanBeUsed(project)) {
            showNotificationForProjectImport(project, onClick)
            return true
        }
        return false
    }

    override fun hideNotification(file: VirtualFile, project: Project): Boolean {
        return hideNotificationForProjectImport(project)
    }

    private fun isGradleImportCanBeUsed(project: Project): Boolean {
        val gradleVersion = getGradleVersion(project)
        if (gradleVersion != null && kotlinDslScriptsModelImportSupported(gradleVersion)) {
            return true
        }
        return false
    }
}