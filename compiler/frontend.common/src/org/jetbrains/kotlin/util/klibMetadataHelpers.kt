/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.metadataVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import java.util.*

private val KLIB_LEGACY_METADATA_VERSION = MetadataVersion(1, 4, 1)

private val LANGUAGE_TO_KLIB_METADATA_VERSION = EnumMap<LanguageVersion, MetadataVersion>(LanguageVersion::class.java).apply {
    LanguageVersion.entries.forEach { this[it] = KLIB_LEGACY_METADATA_VERSION }
    // TODO KT-74417 Uncomment in version 2.3 to bump metadata version
    this[LanguageVersion.KOTLIN_2_3] = KLIB_LEGACY_METADATA_VERSION // MetadataVersion(2, 3, 0)

    check(size == LanguageVersion.entries.size) {
        "Please add mappings from the missing LanguageVersion instances to the corresponding MetadataVersion " +
                "in `LANGUAGE_TO_KLIB_METADATA_VERSION`"
    }
}

private fun LanguageVersion.toKlibMetadataVersion(): MetadataVersion = LANGUAGE_TO_KLIB_METADATA_VERSION.getValue(this)

fun CompilerConfiguration.klibMetadataVersionOrDefault(
    languageVersion: LanguageVersion = languageVersionSettings.languageVersion
): MetadataVersion {
    return this.metadataVersion as? MetadataVersion ?: languageVersion.toKlibMetadataVersion()
}
