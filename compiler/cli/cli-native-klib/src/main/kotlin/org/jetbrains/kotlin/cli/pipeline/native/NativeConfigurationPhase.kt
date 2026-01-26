/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.native

import org.jetbrains.kotlin.backend.konan.setupCommonNativeArguments
import org.jetbrains.kotlin.cli.common.arguments.K2NativeKlibCompilerArguments
import org.jetbrains.kotlin.cli.common.setupCommonKlibArguments
import org.jetbrains.kotlin.cli.pipeline.AbstractConfigurationPhase
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.ConfigurationUpdater
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion

object NativeConfigurationPhase : AbstractConfigurationPhase<K2NativeKlibCompilerArguments>(
    name = "NativeKlibConfigurationPhase",
    postActions = setOf(CheckCompilationErrors.CheckMessageCollector),
    configurationUpdaters = listOf(NativeKlibConfigurationUpdater)
) {
    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion {
        return MetadataVersion(*versionArray)
    }
}

object NativeKlibConfigurationUpdater : ConfigurationUpdater<K2NativeKlibCompilerArguments>() {
    override fun fillConfiguration(
        input: ArgumentsPipelineArtifact<K2NativeKlibCompilerArguments>,
        configuration: CompilerConfiguration,
    ) {
        val (arguments, _, rootDisposable, _, _) = input

        configuration.setupCommonKlibArguments(arguments, canBeMetadataKlibCompilation = true, rootDisposable)

        // Set up arguments common to all native compilation modes
        configuration.setupCommonNativeArguments(
            arguments,
            nostdlib = arguments.nostdlib,
            nodefaultlibs = arguments.nodefaultlibs,
            noendorsedlibs = true, // No endorsed libs in klib-only mode
            kotlinHome = arguments.kotlinHome,
        )

        // Set up Native klib-specific configuration
        arguments.nativeStdlibPath?.let {
            configuration.put(NativeKlibConfigurationKeys.NATIVE_STDLIB_PATH, it)
        }
        arguments.nativePlatformLibrariesPath?.let {
            configuration.put(NativeKlibConfigurationKeys.NATIVE_PLATFORM_LIBRARIES_PATH, it)
        }
    }
}
