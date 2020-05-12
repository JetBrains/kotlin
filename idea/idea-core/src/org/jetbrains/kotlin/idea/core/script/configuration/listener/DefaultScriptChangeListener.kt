/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.core.script.configuration.listener

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.scripting.definitions.isNonScript

open class DefaultScriptChangeListener(project: Project) : ScriptChangeListener(project) {
    override fun editorActivated(vFile: VirtualFile, updater: ScriptConfigurationUpdater) {
        val file = getAnalyzableKtFileForScript(vFile) ?: return
        updater.ensureUpToDatedConfigurationSuggested(file)
    }

    override fun documentChanged(vFile: VirtualFile, updater: ScriptConfigurationUpdater) {
        val file = getAnalyzableKtFileForScript(vFile) ?: return
        updater.ensureUpToDatedConfigurationSuggested(file)
    }

    override fun isApplicable(vFile: VirtualFile): Boolean {
        return vFile.isValid && !vFile.isNonScript()
    }
}