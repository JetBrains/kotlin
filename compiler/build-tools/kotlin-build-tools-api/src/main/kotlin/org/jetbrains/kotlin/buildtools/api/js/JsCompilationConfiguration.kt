/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.js

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import java.io.File

/**
 * Allows users to customize the compilation process and to observe the configured settings (or the default ones).
 *
 * This interface defines a set of properties and methods that allow users to customize the compilation process.
 * It provides control over various aspects of compilation, such as incremental compilation, logging customization and other.
 *
 * This interface is not intended to be implemented by the API consumers.
 */
@ExperimentalBuildToolsApi
public interface JsCompilationConfiguration {
    /**
     * A logger used during the compilation.
     *
     * Managed by [useLogger]
     * Default logger is a logger just printing messages to stdin and stderr.
     */
    public val logger: KotlinLogger

    /**
     * @see [JsCompilationConfiguration.logger]
     */
    public fun useLogger(logger: KotlinLogger): JsCompilationConfiguration

    /**
     * A set of additional to the `.kt` and `.kts` Kotlin script extensions.
     *
     * Managed by [useKotlinScriptFilenameExtensions]
     * Default value is an empty set.
     */
    public val kotlinScriptFilenameExtensions: Set<String>

    /**
     * @see [JsCompilationConfiguration.kotlinScriptFilenameExtensions]
     */
    public fun useKotlinScriptFilenameExtensions(kotlinScriptExtensions: Collection<String>): JsCompilationConfiguration
}
