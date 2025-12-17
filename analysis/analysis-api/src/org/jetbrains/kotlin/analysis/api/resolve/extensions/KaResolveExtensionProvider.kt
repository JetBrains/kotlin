/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolve.extensions

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaExtensibleApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded

/**
 * Provides [resolve extensions][KaResolveExtension] for [KaModule]s. Resolve extensions provide additional Kotlin files containing
 * generated declarations, which will be included in the resolution as if they were regular source files in the module.
 *
 * The extension point name is `org.jetbrains.kotlin.kaResolveExtensionProvider`.
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

    @KaImplementationDetail
    public companion object {
        private val EP_NAME: ExtensionPointName<KaResolveExtensionProvider> = ExtensionPointName(
            "org.jetbrains.kotlin.kaResolveExtensionProvider",
        )

        @KaImplementationDetail
        public fun provideExtensionsFor(module: KaModule): List<KaResolveExtension> = buildList {
            EP_NAME.getExtensionList(module.project).forEach { provider ->
                runCatching {
                    provider.provideExtensionsFor(module)
                }.onSuccess { resolveExtensions ->
                    addAll(resolveExtensions)
                }.onFailure { error ->
                    rethrowIntellijPlatformExceptionIfNeeded(error)

                    logger<KaResolveExtensionProvider>().error(error)
                }
            }
        }
    }
}
