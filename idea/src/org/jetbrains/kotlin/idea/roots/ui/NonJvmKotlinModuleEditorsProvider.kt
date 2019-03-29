/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.roots.ui

import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.ModuleConfigurationEditor
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ui.configuration.*
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.resolve.isJvm

class NonJvmKotlinModuleEditorsProvider : ModuleConfigurationEditorProviderEx {
    override fun isCompleteEditorSet() = true

    override fun createEditors(state: ModuleConfigurationState): Array<ModuleConfigurationEditor> {
        val rootModel = state.rootModel
        val module = rootModel.module
        if (ModuleType.get(module) !is JavaModuleType) return ModuleConfigurationEditor.EMPTY
        val targetPlatform = TargetPlatformDetector.getPlatform(module)
        if (targetPlatform.isJvm()) return ModuleConfigurationEditor.EMPTY

        val moduleName = module.name
        return arrayOf(
                KotlinContentEntriesEditor(moduleName, state),
                object : OutputEditor(state) {}, // Work around protected constructor
                ClasspathEditor(state)
        )
    }
}