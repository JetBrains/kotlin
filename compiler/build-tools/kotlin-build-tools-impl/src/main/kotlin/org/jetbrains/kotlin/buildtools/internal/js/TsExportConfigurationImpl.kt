/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.js

import org.jetbrains.kotlin.buildtools.api.js.TsExportConfiguration
import org.jetbrains.kotlin.buildtools.internal.DeepCopyable
import org.jetbrains.kotlin.buildtools.internal.Options
import org.jetbrains.kotlin.buildtools.internal.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.internal.checkOptionIsAvailableForVersion
import org.jetbrains.kotlin.buildtools.internal.initializeOptions
import java.nio.file.Path

/**
 * Empty implementation of the design option D configuration.
 */
internal class TsExportConfigurationImpl private constructor(
    private val options: Options,
    override val outputDirectory: Path,
) : TsExportConfiguration, TsExportConfiguration.Builder, DeepCopyable<TsExportConfigurationImpl> {

    constructor(outputDirectory: Path) : this(Options(TsExportConfiguration::class), outputDirectory) {
        initializeOptions(this::class, options)
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: TsExportConfiguration.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: TsExportConfiguration.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        options[key] = value
    }

    override fun build(): TsExportConfiguration = deepCopy()

    override fun deepCopy(): TsExportConfigurationImpl = TsExportConfigurationImpl(options.deepCopy(), outputDirectory)
}
