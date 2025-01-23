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
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaResolutionScopeProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.allDirectDependencies
import org.jetbrains.kotlin.analysis.api.projectStructure.analysisContextModule

/**
 * A scope used for resolving files inside [useSiteModule].
 * This scope is a union of [useSiteModule]'s content scope and content scopes of modules [useSiteModule] depends on.
 *
 * Additionally, [KaResolutionScope] encapsulates special handling required for
 * generated and shadowed scopes provided by [org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider].
 * After KT-74541 is fixed, these scopes will be contained directly in [KaModule.contentScope].
 */
@KaImplementationDetail
class KaResolutionScope(
    private val useSiteModule: KaModule,
    private val shadowedScopeByGeneratedFiles: GlobalSearchScope = EMPTY_SCOPE,
) : GlobalSearchScope() {
    val resolutionScope: GlobalSearchScope
        get() = KaResolutionScopeProvider.getInstance(useSiteModule.project).getResolutionScope(useSiteModule)
            .intersectWith(notScope(shadowedScopeByGeneratedFiles))

    override fun getProject(): Project? {
        return resolutionScope.project
    }

    override fun isSearchInModuleContent(aModule: Module): Boolean {
        return resolutionScope.isSearchInModuleContent(aModule)
    }

    override fun isSearchInLibraries(): Boolean {
        return resolutionScope.isSearchInLibraries
    }

    override fun contains(file: VirtualFile): Boolean {
        return resolutionScope.contains(file) || isFromGeneratedModule(file)
    }

    override fun toString(): String {
        return "Analysis scope for $useSiteModule (Resolution scope: $resolutionScope, Scope shadowed by generated files: $shadowedScopeByGeneratedFiles)"
    }

    /**
     * To support files from [org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider]
     * which are not dangling files
     */
    fun isFromGeneratedModule(file: VirtualFile): Boolean {
        val analysisContextModule = file.analysisContextModule ?: return false
        return isFromGeneratedModule(analysisContextModule)
    }

    fun isFromGeneratedModule(analysisContextModule: KaModule): Boolean {
        return analysisContextModule == useSiteModule || analysisContextModule in useSiteModule.allDirectDependencies()
    }
}
