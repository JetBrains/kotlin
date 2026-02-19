/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KlibAbiCompatibilityLevel
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.metadataVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion

private val KLIB_LEGACY_METADATA_VERSION = MetadataVersion(1, 4, 1)

fun LanguageVersion.toKlibMetadataVersion(): MetadataVersion =
    if (this < LanguageVersion.KOTLIN_2_3) KLIB_LEGACY_METADATA_VERSION else toJvmMetadataVersion()

fun CompilerConfiguration.klibMetadataVersionOrDefault(
    languageVersion: LanguageVersion = languageVersionSettings.languageVersion
): MetadataVersion {
    return this.metadataVersion as? MetadataVersion ?: languageVersion.toKlibMetadataVersion()
}

fun KlibAbiCompatibilityLevel.toCInteropKlibMetadataVersion(): MetadataVersion =
    LanguageVersion.fromVersionString("$major.$minor")?.toKlibMetadataVersion() ?: error("Cannot convert $this to MetadataVersion")
