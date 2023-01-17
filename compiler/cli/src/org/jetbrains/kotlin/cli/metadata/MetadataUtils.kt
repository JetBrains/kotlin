/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.metadata

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.buildKotlinLibrary
import org.jetbrains.kotlin.library.metadata.KlibMetadataVersion
import java.io.File

fun buildKotlinMetadataLibrary(configuration: CompilerConfiguration, serializedMetadata: SerializedMetadata, destDir: File) {
    val versions = KotlinLibraryVersioning(
        abiVersion = KotlinAbiVersion.CURRENT,
        libraryVersion = null,
        compilerVersion = KotlinCompilerVersion.getVersion(),
        metadataVersion = KlibMetadataVersion.INSTANCE.toString(),
        irVersion = null
    )

    buildKotlinLibrary(
        emptyList(),
        serializedMetadata,
        null,
        versions,
        destDir.absolutePath,
        configuration[CommonConfigurationKeys.MODULE_NAME]!!,
        nopack = true,
        perFile = false,
        manifestProperties = null,
        dataFlowGraph = null,
        builtInsPlatform = BuiltInsPlatform.COMMON
    )
}