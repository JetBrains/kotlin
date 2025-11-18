/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.projectStructure

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
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
    /**
     * Caches the [VirtualFile] IDs that were last seen by the resolution scope. The cache only stores virtual file IDs that are contained
     * in the resolution scope, as this applies to the vast majority of PSI elements checked via [contains] for [PsiElement]s. Negative
     * results are not cached.
     *
     * A negative ID means that the cache is empty at the specific index.
     *
     * Even though the cache may be accessed in a multithreaded environment, it is not synchronized nor volatile to keep its overhead to a
     * minimum. This is legal as we do not expect perfect data consistency between threads. In particular:
     *
     * 1. Any virtual file ID placed in the cache is a valid result for the rest of the resolution scope's lifetime. As such, late-published
     *    writes are not harmful (an old result is still a valid result), and invalidation is unnecessary.
     * 2. Integer assignment is atomic, even for arrays, so a lack of synchronization or explicit atomicity does not lead to data
     *    inconsistencies.
     * 3. Every slot of the array is independent of every other slot. There is no need to maintain consistency between different slots, and
     *    as such no need to synchronize for consistency.
     *
     * The cache has a major impact on Code Analysis through [canBeAnalysed][org.jetbrains.kotlin.analysis.api.components.KaAnalysisScopeProvider.canBeAnalysed],
     * with a hit rate of >98% in local experiments.
     *
     * The cache has little benefit in Completion due to a low hit rate (as index accesses rarely hit the same virtual file). As such, the
     * cache is only applied to [contains] for [PsiElement]s, to avoid calls from indices. And the cache itself is extremely fast, having a
     * low passthrough overhead even in pathological scenarios (see KT-77578).
     */
    private val virtualFileIdCache = IntArray(32) { -1 }

    override fun contains(file: VirtualFile): Boolean {
        // As noted above, we don't want to use the virtual file cache for index accesses.
        return searchScope.contains(file) || isAccessibleDanglingFile(file)
    }

    override fun contains(element: PsiElement): Boolean {
        if (element is PsiDirectory) {
            return cachedSearchScopeContains(element.virtualFile)
        }

        /**
         * We check the *virtual file* here instead of calling [org.jetbrains.kotlin.psi.psiUtil.contains] on the search scope directly.
         * This is because `psiUtil.contains` queries the search scope with the element's *original file*, so search scope membership of any
         * dangling file element is checked based on the dangling file's original file. But this is incorrect for resolution scope checks:
         * The Analysis API separates dangling files and original files into separate modules. A dangling file element should not be
         * analyzable in its context module's session.
         */
        val psiFile = element.containingFile
        val virtualFile = psiFile.viewProvider.virtualFile
        return cachedSearchScopeContains(virtualFile) || isAccessibleDanglingFile(psiFile)
    }

    private fun cachedSearchScopeContains(virtualFile: VirtualFile): Boolean {
        // The cache depends on virtual file IDs. It can also only store *positive* virtual file IDs. "Real" virtual files are guaranteed to
        // have positive IDs.
        val virtualFileWithId = virtualFile as? VirtualFileWithId
            ?: return searchScope.contains(virtualFile)

        // Don't inline `virtualFile as? VirtualFileWithId` here as that will cause `id` to be boxed.
        val id = virtualFileWithId.id
        if (id < 0) {
            return searchScope.contains(virtualFile)
        }

        // Based on the ID, each virtual file is cached in a predetermined slot. This can lead to collisions if we're unlucky, but it also
        // means that checking the cache and writing to it barely has any overhead. A smarter caching strategy would impose a larger
        // overhead as well as the need for synchronization.
        val cache = virtualFileIdCache
        val index = id % cache.size
        if (cache[index] == id) {
            return true
        } else {
            val isContained = searchScope.contains(virtualFile)
            if (isContained) {
                cache[index] = id
            }
            return isContained
        }
    }

    private fun isAccessibleDanglingFile(psiFile: PsiFile): Boolean {
        val ktFile = psiFile as? KtFile ?: return false
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

    override fun getProject(): Project? = searchScope.project

    override fun isSearchInModuleContent(aModule: Module): Boolean = searchScope.isSearchInModuleContent(aModule)

    override fun isSearchInLibraries(): Boolean = searchScope.isSearchInLibraries

    override fun toString(): String = "Resolution scope for '$useSiteModule'. Underlying search scope: '$searchScope'"
}
