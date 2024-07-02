/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.sessions

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinResolutionScopeProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.allDirectDependencies
import org.jetbrains.kotlin.analysis.api.projectStructure.analysisExtensionFileContextModule

@KaImplementationDetail
class KaGlobalSearchScope(
    val shadowedScope: GlobalSearchScope,
    private val useSiteModule: KaModule,
) : GlobalSearchScope() {
    val baseScope: GlobalSearchScope =
        KotlinResolutionScopeProvider.getInstance(useSiteModule.project).getResolutionScope(useSiteModule)

    override fun getProject(): Project? {
        return baseScope.project
    }

    override fun isSearchInModuleContent(aModule: Module): Boolean {
        return baseScope.isSearchInModuleContent(aModule)
    }

    override fun isSearchInLibraries(): Boolean {
        return baseScope.isSearchInLibraries
    }

    override fun contains(file: VirtualFile): Boolean {
        return (baseScope.contains(file) && !shadowedScope.contains(file)) || isFromGeneratedModule(file, useSiteModule)
    }

    override fun toString(): String {
        return "Analysis scope for $useSiteModule (base: $baseScope, shadowed: $shadowedScope)"
    }

    fun isFromGeneratedModule(file: VirtualFile, useSiteModule: KaModule): Boolean {
        val analysisContextModule = file.analysisExtensionFileContextModule ?: return false
        if (analysisContextModule == useSiteModule) return true
        return analysisContextModule in useSiteModule.allDirectDependencies()
    }
}
