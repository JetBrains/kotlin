/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.projectStructure

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaResolutionScope
import org.jetbrains.kotlin.analysis.api.projectStructure.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.contains

/**
 * [KaBaseResolutionScope] encapsulates special handling required for
 * generated and shadowed scopes provided by [org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider].
 * After KT-74541 is fixed, these scopes will be contained directly in [org.jetbrains.kotlin.analysis.api.projectStructure.KaModule.contentScope].
 *
 * [KaBaseResolutionScope] is not intended to be created manually,
 * it's a responsibility of [org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaResolutionScopeProvider]
 * Please, use [Companion.forModule]
 */
internal class KaBaseResolutionScope(
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

    override fun contains(element: PsiElement): Boolean {
        return resolutionScope.contains(element) || isFromGeneratedModule(element)
    }

    private fun isFromGeneratedModule(element: PsiElement): Boolean {
        val ktFile = element.containingFile as? KtFile ?: return false
        if (ktFile.isDangling) {
            val module = KaModuleProvider.getModule(useSiteModule.project, ktFile, useSiteModule)
            return module.isAccessibleFromUseSiteModule()
        }

        val virtualFile = ktFile.virtualFile ?: return false
        return isFromGeneratedModule(virtualFile)
    }

    /**
     * To support files from [org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider]
     * which are not dangling files
     */
    private fun isFromGeneratedModule(virtualFile: VirtualFile): Boolean {
        val analysisContextModule = virtualFile.analysisContextModule ?: return false
        return analysisContextModule.isAccessibleFromUseSiteModule()
    }

    private fun KaModule.isAccessibleFromUseSiteModule(): Boolean {
        return this == useSiteModule || this in useSiteModule.allDirectDependencies()
    }

    override fun toString(): String {
        return "Analysis scope for $useSiteModule. Resolution scope: $resolutionScope"
    }
}
