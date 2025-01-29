/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.modification.*
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.createPackageProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import java.time.Duration

internal class SymbolKotlinProviderCache(project: Project) {
    /**
     * [packageProviderCache] caches [KotlinPackageProvider]s for [GlobalSearchScope]s to avoid recreation of the package provider for every
     * usage in [SymbolKotlinAsJavaSupport]. The major advantage here is that [KotlinPackageProvider] usually caches its own results, which
     * can only be beneficial if it's not recreated on every request.
     *
     * While we should generally avoid using scopes as keys, it's valid to use them in a weak key cache, as this (1) won't hinder their
     * garbage collection and (2) keys will be compared by identity, avoiding issues with missing or costly scope equality.
     *
     * Package providers are expired after not being used for a while to avoid a leak when the scope is kept around for too long.
     */
    private val packageProviderCache =
        Caffeine.newBuilder()
            .weakKeys()
            .expireAfterAccess(Duration.ofSeconds(30))
            .build<GlobalSearchScope, KotlinPackageProvider>(project::createPackageProvider)

    fun getPackageProvider(scope: GlobalSearchScope): KotlinPackageProvider =
        packageProviderCache[scope]
            ?: errorWithAttachment("Couldn't create a package provider for the given scope.") {
                withEntry("scope", scope.toString())
            }

    private fun invalidateAll() {
        // All modification events are published in write actions, so the invalidation will not run into concurrency issues.
        packageProviderCache.invalidateAll()
    }

    internal class ModuleStateModificationListener(val project: Project) : KotlinModuleStateModificationListener {
        override fun onModification(module: KaModule, modificationKind: KotlinModuleStateModificationKind) {
            getInstance(project).invalidateAll()
        }
    }

    internal class ModuleOutOfBlockModificationListener(val project: Project) : KotlinModuleOutOfBlockModificationListener {
        override fun onModification(module: KaModule) {
            getInstance(project).invalidateAll()
        }
    }

    internal class GlobalModuleStateModificationListener(val project: Project) : KotlinGlobalModuleStateModificationListener {
        override fun onModification() {
            getInstance(project).invalidateAll()
        }
    }

    internal class GlobalSourceModuleStateModificationListener(val project: Project) : KotlinGlobalSourceModuleStateModificationListener {
        override fun onModification() {
            getInstance(project).invalidateAll()
        }
    }

    internal class GlobalScriptModuleStateModificationListener(val project: Project) : KotlinGlobalScriptModuleStateModificationListener {
        override fun onModification() {
            getInstance(project).invalidateAll()
        }
    }

    internal class GlobalSourceOutOfBlockModificationListener(val project: Project) : KotlinGlobalSourceOutOfBlockModificationListener {
        override fun onModification() {
            getInstance(project).invalidateAll()
        }
    }

    companion object {
        fun getInstance(project: Project): SymbolKotlinProviderCache = project.service()
    }
}
