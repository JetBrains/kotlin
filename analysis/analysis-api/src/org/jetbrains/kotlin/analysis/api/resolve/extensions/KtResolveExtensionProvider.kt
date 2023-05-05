/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolve.extensions

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.kotlin.analysis.project.structure.KtModule

/**
 * Allows extending Kotlin resolution by generating additional declarations.
 *
 * Those declarations will be analyzed the same way as they were just regular source files inside the project.
 *
 * All member implementations should consider caching the results for subsequent invocations.
 */
public abstract class KtResolveExtensionProvider {
    /**
     * Provides a list of [KtResolveExtension]s for a given [KtModule].
     *
     * Should not perform any heavy analysis and the generation of the actual files. All file generation should be performed only in [KtResolveExtensionFile.buildFileText].
     *
     * Implementations should consider caching the results, so the subsequent invocations should be performed instantly.
     *
     * Implementation cannot use the Kotlin resolve inside, as this function is called during session initialization, so Analysis API access is forbidden.
     */
    public abstract fun provideExtensionsFor(module: KtModule): List<KtResolveExtension>

    public companion object {
        public val EP_NAME: ExtensionPointName<KtResolveExtensionProvider> =
            ExtensionPointName<KtResolveExtensionProvider>("org.jetbrains.kotlin.ktResolveExtensionProvider")

        public fun provideExtensionsFor(module: KtModule): List<KtResolveExtension> {
            return EP_NAME.getExtensionList(module.project).flatMap { it.provideExtensionsFor(module) }
        }
    }
}