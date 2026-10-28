/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.cri

import org.jetbrains.kotlin.buildtools.api.cri.CompilerRefIndexConfiguration
import org.jetbrains.kotlin.buildtools.internal.DeepCopyable
import org.jetbrains.kotlin.buildtools.internal.Options
import org.jetbrains.kotlin.buildtools.internal.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.internal.checkOptionIsAvailableForVersion
import java.nio.file.Path

// The API return type keeps the verifier from loading `CompilerRefIndexConfiguration.Builder`, which an older consumer API lacks
internal fun createCompilerRefIndexConfigurationBuilder(outputDirectory: Path): CompilerRefIndexConfiguration.Builder =
    CompilerRefIndexConfigurationImpl(outputDirectory)

internal class CompilerRefIndexConfigurationImpl(
    override val outputDirectory: Path,
    private val options: Options = Options(CompilerRefIndexConfiguration::class),
) : CompilerRefIndexConfiguration, CompilerRefIndexConfiguration.Builder, DeepCopyable<CompilerRefIndexConfigurationImpl> {

    override fun build(): CompilerRefIndexConfiguration = deepCopy()

    override fun toBuilder(): CompilerRefIndexConfiguration.Builder = deepCopy()

    override fun deepCopy(): CompilerRefIndexConfigurationImpl = CompilerRefIndexConfigurationImpl(outputDirectory, options.deepCopy())

    @UseFromImplModuleRestricted
    override fun <V> get(key: CompilerRefIndexConfiguration.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: CompilerRefIndexConfiguration.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        options[key] = value
    }
}
