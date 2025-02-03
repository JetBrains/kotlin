/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.sessions

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.platform.sessions.KaResolutionScope
import org.jetbrains.kotlin.analysis.api.projectStructure.*
import org.jetbrains.kotlin.psi.KtFile

/**
 * [KaBaseResolutionScope] encapsulates special handling required for
 * generated and shadowed scopes provided by [org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider].
 * After KT-74541 is fixed, these scopes will be contained directly in [KaModule.contentScope].
 *
 * [KaBaseResolutionScope] is not intended to be created manually,
 * it's a responsibility of [org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaResolutionScopeProvider]
 * Please, use [KaResolutionScope.getInstance]
 */
@KaImplementationDetail
class KaBaseResolutionScope internal constructor(
    private val useSiteModule: KaModule,
    private val resolutionScope: GlobalSearchScope,
) : KaResolutionScope() {
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
        return "Analysis scope for $useSiteModule. Resolution scope: $resolutionScope"
    }

    /**
     * To support files from [org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider]
     * which are not dangling files
     */
    fun isFromGeneratedModule(virtualFile: VirtualFile): Boolean {
        virtualFile.analysisContextModule?.let {
            return isFromGeneratedModule(it)
        }

        val ktFile = virtualFile.findPsiFile(useSiteModule.project) as? KtFile ?: return false
        if (ktFile.isDangling) {
            val module = KaModuleProvider.getModule(useSiteModule.project, ktFile, useSiteModule)
            return isFromGeneratedModule(module)
        }

        return false
    }

    fun isFromGeneratedModule(analysisContextModule: KaModule): Boolean {
        return analysisContextModule == useSiteModule || analysisContextModule in useSiteModule.allDirectDependencies()
    }
}
