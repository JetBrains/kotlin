/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.native

import org.jetbrains.kotlin.cli.common.arguments.K2NativeKlibCompilerArguments
import org.jetbrains.kotlin.cli.pipeline.AbstractConfigurationPhase
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.ConfigurationUpdater
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion

internal object NativeKlibConfigurationPhase : AbstractConfigurationPhase<K2NativeKlibCompilerArguments>(
    name = "JsConfigurationPhase",
    postActions = setOf(CheckCompilationErrors.CheckMessageCollector),
    configurationUpdaters = listOf()
) {
    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion {
        return MetadataVersion(*versionArray)
    }
}

private object NativeKlibConfigurationUpdater : ConfigurationUpdater<K2NativeKlibCompilerArguments>() {
    override fun fillConfiguration(
        input: ArgumentsPipelineArtifact<K2NativeKlibCompilerArguments>,
        configuration: CompilerConfiguration,
    ) {
        val (arguments, services, rootDisposable, _, _) = input

    }
}