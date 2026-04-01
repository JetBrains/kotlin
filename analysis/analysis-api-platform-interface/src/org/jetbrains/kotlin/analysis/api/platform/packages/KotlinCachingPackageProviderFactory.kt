/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.packages

import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus
import org.jetbrains.kotlin.analysis.api.platform.modification.*
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import java.time.Duration

/**
 * A [KotlinPackageProviderFactory] which caches [KotlinPackageProvider]s based on their [GlobalSearchScope]s. The major advantage here is
 * that [KotlinPackageProvider] usually caches its own results, which can only be beneficial if it's not recreated with every call to
 * [createPackageProvider].
 */
@KaPlatformInterface
public abstract class KotlinCachingPackageProviderFactory(project: Project) : KotlinPackageProviderFactory, Disposable {
    /**
     * While we should generally avoid using scopes as keys, it's valid to use them in a weak/soft key cache. It won't hinder their garbage
     * collection, and keys will be compared by identity, avoiding issues with missing or costly scope equality.
     *
     * The [KotlinPackageProvider] usually retains the [GlobalSearchScope], so the value also needs to be non-strongly referenced, as
     * otherwise the cached [KotlinPackageProvider] keeps the [GlobalSearchScope] key alive.
     *
     * We're using soft values so that the cached package provider is slightly resistant to garbage collection. The [GlobalSearchScope] key
     * will usually be softly reachable via the value reference, so soft keys would be more appropriate, but Caffeine doesn't support them.
     *
     * Package providers are expired after not being used for a few seconds to clean them up deterministically, in case the soft references
     * are too rigid.
     */
    private val packageProviderCache =
        Caffeine.newBuilder()
            .weakKeys()
            .softValues()
            .expireAfterAccess(Duration.ofSeconds(5))
            .build<GlobalSearchScope, KotlinPackageProvider>(::createNewPackageProvider)

    init {
        // It's generally best practice to register listeners in plugin XMLs. However, we don't know whether the platform intends to
        // implement a caching package provider factory or not, and thus can't register these listeners in the XML.
        project.analysisMessageBus.connect(this).subscribe(
            KotlinModificationEvent.TOPIC,
            KotlinModificationEventListener { event ->
                when (event) {
                    is KotlinModuleStateModificationEvent,
                    is KotlinModuleOutOfBlockModificationEvent,
                    KotlinGlobalModuleStateModificationEvent,
                    KotlinGlobalSourceModuleStateModificationEvent,
                    KotlinGlobalScriptModuleStateModificationEvent,
                    KotlinGlobalSourceOutOfBlockModificationEvent,
                        -> invalidateAll()

                    is KotlinCodeFragmentContextModificationEvent -> {}
                }
            }
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
