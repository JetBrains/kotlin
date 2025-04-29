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

/**
 * [KaBaseResolutionScope] is not intended to be created manually. It's the responsibility of [KaResolutionScopeProvider][org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaResolutionScopeProvider].
 * Please use [Companion.forModule] instead.
 *
 * @param analyzableModules The set of modules whose declarations can be analyzed from the [useSiteModule], including the use-site module
 *  itself.
 */
internal class KaBaseResolutionScope(
    private val useSiteModule: KaModule,
    private val searchScope: GlobalSearchScope,
    private val analyzableModules: Set<KaModule>,
) : KaResolutionScope() {
    override fun getProject(): Project? = searchScope.project

    override fun isSearchInModuleContent(aModule: Module): Boolean = searchScope.isSearchInModuleContent(aModule)

    override fun isSearchInLibraries(): Boolean = searchScope.isSearchInLibraries

    override fun contains(file: VirtualFile): Boolean = searchScope.contains(file) || isAccessibleDanglingFile(file)

    override fun contains(element: PsiElement): Boolean {
        /**
         * We check the *virtual file* here instead of calling [org.jetbrains.kotlin.psi.psiUtil.contains] on the search scope directly.
         * This is because `psiUtil.contains` queries the search scope with the element's *original file*, so search scope membership of any
         * dangling file element is checked based on the dangling file's original file. But this is incorrect for resolution scope checks:
         * The Analysis API separates dangling files and original files into separate modules. A dangling file element should not be
         * analyzable in its context module's session.
         */
        val virtualFile = element.containingFile.virtualFile
        return virtualFile != null && searchScope.contains(virtualFile) || isAccessibleDanglingFile(element)
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

    private fun KaModule.isAccessibleFromUseSiteModule(): Boolean = this in analyzableModules

    override val underlyingSearchScope: GlobalSearchScope
        get() = searchScope

    override fun toString(): String = "Resolution scope for '$useSiteModule'. Underlying search scope: '$searchScope'"
}
