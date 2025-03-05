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
        return resolutionScope.contains(file) || isAccessibleDanglingFile(file)
    }

    override fun contains(element: PsiElement): Boolean {
        return resolutionScope.contains(element) || isAccessibleDanglingFile(element)
    }

    private fun isAccessibleDanglingFile(element: PsiElement): Boolean {
        val ktFile = element.containingFile as? KtFile ?: return false
        if (!ktFile.isDangling) {
            return false
        }
        val module = ktFile.contextModule ?: KaModuleProvider.getModule(useSiteModule.project, ktFile, useSiteModule)
        return module.isAccessibleFromUseSiteModule()
    }

    private fun isAccessibleDanglingFile(virtualFile: VirtualFile): Boolean {
        return virtualFile.analysisContextModule?.isAccessibleFromUseSiteModule() == true
    }

    private fun KaModule.isAccessibleFromUseSiteModule(): Boolean {
        return this in buildSet {
            add(useSiteModule)
            addAll(useSiteModule.directRegularDependencies)
            addAll(useSiteModule.directFriendDependencies)
            addAll(useSiteModule.transitiveDependsOnDependencies)
            if (useSiteModule is KaLibrarySourceModule) {
                add(useSiteModule.binaryLibrary)
            }
        }
    }

    override fun toString(): String {
        return "Analysis scope for $useSiteModule. Resolution scope: $resolutionScope"
    }
}
