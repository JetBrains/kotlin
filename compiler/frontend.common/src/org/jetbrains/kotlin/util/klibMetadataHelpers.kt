/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.metadataVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion

private val KLIB_LEGACY_METADATA_VERSION = MetadataVersion(1, 4, 1)

fun CompilerConfiguration.klibMetadataVersionOrDefault(): MetadataVersion {
    return this.metadataVersion as? MetadataVersion ?: KLIB_LEGACY_METADATA_VERSION
}
