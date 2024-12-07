/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.SourceMapNamesPolicy
import org.jetbrains.kotlin.js.config.SourceMapSourceEmbedding
import java.io.File

data class SourceMapsInfo(
    val sourceMapPrefix: String,
    val sourceRoots: List<String>,
    val outputDir: File?,
    val sourceMapContentEmbedding: SourceMapSourceEmbedding,
    val namesPolicy: SourceMapNamesPolicy,
    val includeUnavailableSourcesIntoSourceMap: Boolean = false
) {
    companion object {
        fun from(configuration: CompilerConfiguration): SourceMapsInfo? =
            if (configuration.getBoolean(JSConfigurationKeys.SOURCE_MAP)) {
                SourceMapsInfo(
                    configuration.get(JSConfigurationKeys.SOURCE_MAP_PREFIX, ""),
                    configuration.get(JSConfigurationKeys.SOURCE_MAP_SOURCE_ROOTS, emptyList()),
                    configuration.get(JSConfigurationKeys.OUTPUT_DIR),
                    configuration.get(JSConfigurationKeys.SOURCE_MAP_EMBED_SOURCES, SourceMapSourceEmbedding.INLINING),
                    configuration.get(JSConfigurationKeys.SOURCEMAP_NAMES_POLICY, SourceMapNamesPolicy.SIMPLE_NAMES),
                    configuration.getBoolean(JSConfigurationKeys.SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_FILES),
                )
            } else {
                null
            }
    }
}

val JsCommonBackendContext.sourceMapsInfo: SourceMapsInfo?
    get() = SourceMapsInfo.from(configuration)
