/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch

import com.intellij.ide.scratch.ScratchFileService
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.dependencies.ScriptRelatedModulesProvider
import org.jetbrains.kotlin.idea.core.script.scriptRelatedModuleName

class ScratchModuleDependencyProvider : ScriptRelatedModulesProvider() {
    override fun getRelatedModules(file: VirtualFile, project: Project): List<Module> {
        if (ScratchFileService.isInScratchRoot(file)) {
            val scratchModule = file.scriptRelatedModuleName?.let { ModuleManager.getInstance(project).findModuleByName(it) }
            if (scratchModule != null) {
                return listOf(scratchModule)
            }
        }
        return emptyList()
    }
}