/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope

private class ModuleLibrariesSearchScope(module: Module) : GlobalSearchScope(module.project) {
    private val projectFileIndex = ProjectRootManager.getInstance(module.project).fileIndex
    private val moduleFileIndex = ModuleRootManager.getInstance(module).fileIndex

    override fun contains(file: VirtualFile): Boolean {
        // We want this scope to work only on .class files, not on source files
        if (!projectFileIndex.isInLibraryClasses(file)) return false

        val orderEntry = moduleFileIndex.getOrderEntryForFile(file)
        return orderEntry is JdkOrderEntry || orderEntry is LibraryOrderEntry
    }

    override fun compare(file1: VirtualFile, file2: VirtualFile): Int {
        return Comparing.compare(moduleFileIndex.getOrderEntryForFile(file2), moduleFileIndex.getOrderEntryForFile(file1))
    }

    override fun isSearchInModuleContent(aModule: Module): Boolean = false

    override fun isSearchInLibraries(): Boolean = true
}

fun createScopeForModuleLibraries(module: Module): GlobalSearchScope = ModuleLibrariesSearchScope(module)