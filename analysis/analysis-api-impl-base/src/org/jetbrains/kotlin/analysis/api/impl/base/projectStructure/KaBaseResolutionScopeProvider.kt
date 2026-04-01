/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.projectStructure

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.caches.getOrPut
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaGlobalSearchScopeMerger
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaResolutionScope
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaResolutionScopeProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaBuiltinsModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import java.time.Duration

class KaBaseResolutionScopeProvider : KaResolutionScopeProvider {
    override fun getResolutionScope(module: KaModule): KaResolutionScope {
        return resolutionScopeCache.getOrPut(module) { module ->
            val analyzableModules = getAnalyzableModules(module)
            val searchScope = buildSearchScope(module, analyzableModules)
            KaBaseResolutionScope(module, searchScope)
        }
    }

    private fun getAnalyzableModules(module: KaModule): Set<KaModule> =
        buildSet {
            add(module)
            addAll(module.directRegularDependencies)
            addAll(module.directFriendDependencies)
            addAll(module.transitiveDependsOnDependencies)
            if (module is KaLibrarySourceModule) {
                add(module.binaryLibrary)
            }
        }

    private fun buildSearchScope(module: KaModule, analyzableModules: Set<KaModule>): GlobalSearchScope {
        val scopes = buildList {
            analyzableModules.mapTo(this) { it.contentScope }
            if (analyzableModules.none { it is KaBuiltinsModule }) {
                // `KaBuiltinsModule` is a module containing builtins declarations for the target platform.
                // It is never a dependency of any `KaModule`,
                // builtins are registered on a symbol provider level during session creation.
                // That's why its scope has to be added manually here.
                add(createBuiltinsScope(module.project))
            }
        }
        return KaGlobalSearchScopeMerger.getInstance(module.project).union(scopes)
    }

    private fun createBuiltinsScope(project: Project): GlobalSearchScope {
        return BuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(project)
    }

    private val resolutionScopeCache: Cache<KaModule, KaResolutionScope> =
        Caffeine.newBuilder().weakKeys().softValues().expireAfterAccess(Duration.ofSeconds(10)).build()
}
