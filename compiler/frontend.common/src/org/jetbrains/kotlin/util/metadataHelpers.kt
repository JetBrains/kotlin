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

private val LANGUAGE_TO_METADATA_VERSION = EnumMap<LanguageVersion, MetadataVersion>(LanguageVersion::class.java).apply {
    val oldMetadataVersion = MetadataVersion(1, 1, 18)
    for (version in LanguageVersion.entries) {
        this[version] = when {
            version <= LanguageVersion.KOTLIN_1_3 -> oldMetadataVersion
            version == LanguageVersion.KOTLIN_1_4 -> MetadataVersion(1, 4, 3)
            version == LanguageVersion.KOTLIN_1_5 -> MetadataVersion(1, 5, 1)
            version == LanguageVersion.LATEST_STABLE -> MetadataVersion.INSTANCE
            else -> MetadataVersion(version.major, version.minor, 0)
        }
    }
}

fun LanguageVersion.toMetadataVersion(): MetadataVersion = LANGUAGE_TO_METADATA_VERSION.getValue(this)

fun CompilerConfiguration.metadataVersion(
    languageVersion: LanguageVersion = languageVersionSettings.languageVersion,
): MetadataVersion = this.metadataVersion as? MetadataVersion ?: languageVersion.toMetadataVersion()
