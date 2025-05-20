/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import java.util.*

private val LANGUAGE_TO_JVM_METADATA_VERSION = EnumMap<LanguageVersion, MetadataVersion>(LanguageVersion::class.java).apply {
    val oldMetadataVersion = MetadataVersion(1, 1, 18)
    this[LanguageVersion.KOTLIN_1_0] = oldMetadataVersion
    this[LanguageVersion.KOTLIN_1_1] = oldMetadataVersion
    this[LanguageVersion.KOTLIN_1_2] = oldMetadataVersion
    this[LanguageVersion.KOTLIN_1_3] = oldMetadataVersion
    this[LanguageVersion.KOTLIN_1_4] = MetadataVersion(1, 4, 3)
    this[LanguageVersion.KOTLIN_1_5] = MetadataVersion(1, 5, 1)
    this[LanguageVersion.KOTLIN_1_6] = MetadataVersion(1, 6, 0)
    this[LanguageVersion.KOTLIN_1_7] = MetadataVersion(1, 7, 0)
    this[LanguageVersion.KOTLIN_1_8] = MetadataVersion(1, 8, 0)
    this[LanguageVersion.KOTLIN_1_9] = MetadataVersion(1, 9, 0)
    this[LanguageVersion.KOTLIN_2_0] = MetadataVersion(2, 0, 0)
    this[LanguageVersion.KOTLIN_2_1] = MetadataVersion(2, 1, 0)
    this[LanguageVersion.KOTLIN_2_2] = MetadataVersion.INSTANCE
    this[LanguageVersion.KOTLIN_2_3] = MetadataVersion(2, 3, 0)
    this[LanguageVersion.KOTLIN_2_4] = MetadataVersion(2, 4, 0)

    check(size == LanguageVersion.entries.size) {
        "Please add mappings from the missing LanguageVersion instances to the corresponding MetadataVersion " +
                "in `LANGUAGE_TO_METADATA_VERSION`"
    }
}

// TODO KT-76195 Consider renaming to `toMetadataVersion` in version 2.4 here and combine with `toKlibMetadataVersion`
fun LanguageVersion.toJvmMetadataVersion(): MetadataVersion = LANGUAGE_TO_JVM_METADATA_VERSION.getValue(this)

// TODO KT-76195 Consider renaming to `metadataVersion` in version 2.4 here and combine with `klibMetadataVersionOrDefault`
fun CompilerConfiguration.jvmMetadataVersion(
    languageVersion: LanguageVersion = languageVersionSettings.languageVersion
): BinaryVersion {
    return get(CommonConfigurationKeys.METADATA_VERSION) ?: languageVersion.toJvmMetadataVersion()
}
