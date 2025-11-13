/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolve.extensions

import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaExtensibleApi
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * Provides [resolve extensions][KaResolveExtension] for [KaModule]s. Resolve extensions provide additional Kotlin files containing
 * generated declarations, which will be included in the resolution as if they were regular source files in the module.
 */
@KaExtensibleApi
@KaExperimentalApi
public abstract class KaResolveExtensionProvider {
    /**
     * Provides a list of [resolve extensions][KaResolveExtension] for the given [module].
     *
     * The function should not generate the actual file text or perform any heavy analysis. Any file text should only be generated *lazily*
     * in [KaResolveExtensionFile.buildFileText].
     *
     * Additionally, all implementations should:
     *
     * - Consider caching the results for subsequent invocations.
     * - Avoid using Kotlin resolution, as this function is called during session initialization, so Analysis API access is forbidden.
     * - Avoid using [KaModule.contentScope] of [module], as [KaResolveExtensionProvider.provideExtensionsFor] is used to
     *   lazily calculate content scopes.
     *   - Also avoid using [KaModuleProvider][org.jetbrains.kotlin.analysis.api.projectStructure.KaModuleProvider] or the project structure
     *     provider, as content scope calculation may be triggered during `getModule`.
     */
    public abstract fun provideExtensionsFor(module: KaModule): List<KaResolveExtension>

    @KaExperimentalApi
    public companion object {
        public val EP_NAME: ExtensionPointName<KaResolveExtensionProvider> =
            ExtensionPointName<KaResolveExtensionProvider>("org.jetbrains.kotlin.kaResolveExtensionProvider")

        /**
         * Creates [resolve extensions][KaResolveExtension] for the provided [KaModule].
         * The [Disposable] provided by the factory will be used as a parent for all returned extensions.
         *
         * @param module The [KaModule] for which to create extensions.
         * @param parentDisposableFactory A factory method to retrieve a parent [Disposable] for the returned
         *   extensions. This factory will only be invoked if at least one extension is created.
         */
        public fun provideExtensionsFor(
            module: KaModule,
            parentDisposableFactory: () -> Disposable,
        ): List<KaResolveExtension> {
            val extensions = EP_NAME.getExtensionList(module.project).flatMap { it.provideExtensionsFor(module) }
            if (extensions.isEmpty()) return emptyList()

            val parentDisposable = parentDisposableFactory()
            extensions.forEach { Disposer.register(parentDisposable, it) }
            return extensions
        }
    }
}
