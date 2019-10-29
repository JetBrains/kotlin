/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.loader

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptConfigurationSnapshot
import org.jetbrains.kotlin.idea.highlighter.OutsidersPsiFileSupportUtils
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition

internal class ScriptOutsiderFileConfigurationLoader(val project: Project) :
    ScriptConfigurationLoader {
    override fun loadDependencies(
        isFirstLoad: Boolean,
        virtualFile: VirtualFile,
        scriptDefinition: ScriptDefinition,
        context: ScriptConfigurationLoadingContext
    ): Boolean {
        if (!isFirstLoad) return false

        val fileOrigin = OutsidersPsiFileSupportUtils.getOutsiderFileOrigin(
            project,
            virtualFile
        ) ?: return false

        val original = context.getCachedConfiguration(fileOrigin)
        if (original != null) {
            context.saveNewConfiguration(
                virtualFile,
                ScriptConfigurationSnapshot(
                    original.inputs,
                    listOf(),
                    original.configuration
                )
            )
        }

        // todo(KT-34615): initiate loading configuration for original file and subscribe to it's result?
        //                 should we show "new context" notification for both files?

        return true
    }
}