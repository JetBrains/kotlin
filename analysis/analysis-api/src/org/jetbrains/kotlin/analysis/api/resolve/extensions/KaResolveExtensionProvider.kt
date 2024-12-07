/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolve.extensions

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * Allows extending Kotlin resolution by generating additional declarations.
 *
 * Those declarations will be analyzed the same way as they were just regular source files inside the project.
 *
 * All member implementations should consider caching the results for subsequent invocations.
 */
@KaExperimentalApi
public abstract class KaResolveExtensionProvider {
    /**
     * Provides a list of [KaResolveExtension]s for a given [KaModule].
     *
     * Should not perform any heavy analysis and the generation of the actual files. All file generation should be performed only in [KaResolveExtensionFile.buildFileText].
     *
     * Implementations should consider caching the results, so the subsequent invocations should be performed instantly.
     *
     * Implementation cannot use the Kotlin resolve inside, as this function is called during session initialization, so Analysis API access is forbidden.
     */
    public abstract fun provideExtensionsFor(module: KaModule): List<KaResolveExtension>

    @KaExperimentalApi
    public companion object {
        public val EP_NAME: ExtensionPointName<KaResolveExtensionProvider> =
            ExtensionPointName<KaResolveExtensionProvider>("org.jetbrains.kotlin.kaResolveExtensionProvider")

        public fun provideExtensionsFor(module: KaModule): List<KaResolveExtension> {
            return EP_NAME.getExtensionList(module.project).flatMap { it.provideExtensionsFor(module) }
        }
    }
}
