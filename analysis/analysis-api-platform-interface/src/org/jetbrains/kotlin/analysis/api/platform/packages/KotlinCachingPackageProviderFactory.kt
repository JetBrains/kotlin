/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.packages

import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus
import org.jetbrains.kotlin.analysis.api.platform.modification.*
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import java.time.Duration

/**
 * A [KotlinPackageProviderFactory] which caches [KotlinPackageProvider]s based on their [GlobalSearchScope]s. The major advantage here is
 * that [KotlinPackageProvider] usually caches its own results, which can only be beneficial if it's not recreated with every call to
 * [createPackageProvider].
 */
public abstract class KotlinCachingPackageProviderFactory(project: Project) : KotlinPackageProviderFactory, Disposable {
    /**
     * While we should generally avoid using scopes as keys, it's valid to use them in a weak key cache, as this (1) won't hinder their
     * garbage collection and (2) keys will be compared by identity, avoiding issues with missing or costly scope equality.
     *
     * Package providers are expired after not being used for a while to avoid a leak when the scope is kept around for too long.
     */
    private val packageProviderCache =
        Caffeine.newBuilder()
            .weakKeys()
            .expireAfterAccess(Duration.ofSeconds(30))
            .build<GlobalSearchScope, KotlinPackageProvider>(::createNewPackageProvider)

    init {
        // It's generally best practice to register listeners in plugin XMLs. However, we don't know whether the platform intends to
        // implement a caching package provider factory or not, and thus can't register these listeners in the XML.
        val connection = project.analysisMessageBus.connect(this)

        connection.subscribe(
            KotlinModificationTopics.MODULE_STATE_MODIFICATION,
            KotlinModuleStateModificationListener { _, _ -> invalidateAll() },
        )

        connection.subscribe(
            KotlinModificationTopics.MODULE_OUT_OF_BLOCK_MODIFICATION,
            KotlinModuleOutOfBlockModificationListener { _ -> invalidateAll() },
        )

        connection.subscribe(
            KotlinModificationTopics.GLOBAL_MODULE_STATE_MODIFICATION,
            KotlinGlobalModuleStateModificationListener { invalidateAll() },
        )

        connection.subscribe(
            KotlinModificationTopics.GLOBAL_SOURCE_MODULE_STATE_MODIFICATION,
            KotlinGlobalSourceModuleStateModificationListener { invalidateAll() },
        )

        connection.subscribe(
            KotlinModificationTopics.GLOBAL_SCRIPT_MODULE_STATE_MODIFICATION,
            KotlinGlobalScriptModuleStateModificationListener { invalidateAll() },
        )

        connection.subscribe(
            KotlinModificationTopics.GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION,
            KotlinGlobalSourceOutOfBlockModificationListener { invalidateAll() },
        )
    }

    protected abstract fun createNewPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider

    override fun createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider =
        packageProviderCache[searchScope]
            ?: errorWithAttachment("Couldn't create a package provider for the given scope.") {
                withEntry("scope", searchScope.toString())
            }

    private fun invalidateAll() {
        // All modification events are published in write actions, so the invalidation will not run into concurrency issues.
        packageProviderCache.invalidateAll()
    }

    override fun dispose() {
    }
}
