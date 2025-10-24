/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.js.JsCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.*
import java.io.File

internal class JsCompilationConfigurationImpl(
    override var kotlinScriptFilenameExtensions: Set<String> = emptySet(),
    override var logger: KotlinLogger = DefaultKotlinLogger,
) : JsCompilationConfiguration {
    internal var aggregatedIcConfiguration: AggregatedIcConfiguration<*>? = null
        private set

    override fun useLogger(logger: KotlinLogger): JsCompilationConfiguration {
        this.logger = logger
        return this
    }

    override fun useKotlinScriptFilenameExtensions(kotlinScriptExtensions: Collection<String>): JsCompilationConfiguration {
        this.kotlinScriptFilenameExtensions = kotlinScriptExtensions.toSet()
        return this
    }
}