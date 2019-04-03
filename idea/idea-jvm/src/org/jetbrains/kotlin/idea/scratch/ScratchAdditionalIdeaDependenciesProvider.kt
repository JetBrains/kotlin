/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch

import com.intellij.ide.scratch.ScratchFileService
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.dependencies.ScriptAdditionalIdeaDependenciesProvider
import org.jetbrains.kotlin.idea.core.script.scriptRelatedModuleName
import org.jetbrains.kotlin.utils.addIfNotNull

class ScratchAdditionalIdeaDependenciesProvider : ScriptAdditionalIdeaDependenciesProvider() {

    override fun getRelatedModules(file: VirtualFile, project: Project): List<Module> {
        if (!ScratchFileService.isInScratchRoot(file)) return emptyList()

        val scratchModule = file.scriptRelatedModuleName?.let {
            ModuleManager.getInstance(project).findModuleByName(it)
        } ?: return emptyList()

        val modules = linkedSetOf(scratchModule)
        moduleDependencyEnumerator(scratchModule).withoutLibraries().forEach { orderEntry ->
            when (orderEntry) {
                is ModuleSourceOrderEntry -> modules.add(orderEntry.getOwnerModule())
                is ModuleOrderEntry -> modules.addIfNotNull(orderEntry.module)
            }
            true
        }
        return modules.toList()
    }

    override fun getRelatedLibraries(file: VirtualFile, project: Project): List<Library> {
        if (!ScratchFileService.isInScratchRoot(file)) return emptyList()

        val result = linkedSetOf<Library>()
        getRelatedModules(file, project).forEach {
            moduleDependencyEnumerator(it).withoutDepModules().forEach { orderEntry ->
                if (orderEntry is LibraryOrderEntry) {
                    result.addIfNotNull(orderEntry.library)
                }
                true
            }
        }
        return result.toList()
    }

    private fun moduleDependencyEnumerator(it: Module): OrderEnumerator {
        return ModuleRootManager.getInstance(it).orderEntries()
            .compileOnly().withoutSdk().recursively().exportedOnly()
    }
}