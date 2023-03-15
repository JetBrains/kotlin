/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import java.util.*

private val LANGUAGE_TO_METADATA_VERSION = EnumMap<LanguageVersion, JvmMetadataVersion>(LanguageVersion::class.java).apply {
    val oldMetadataVersion = JvmMetadataVersion(1, 1, 18)
    this[LanguageVersion.KOTLIN_1_0] = oldMetadataVersion
    this[LanguageVersion.KOTLIN_1_1] = oldMetadataVersion
    this[LanguageVersion.KOTLIN_1_2] = oldMetadataVersion
    this[LanguageVersion.KOTLIN_1_3] = oldMetadataVersion
    this[LanguageVersion.KOTLIN_1_4] = JvmMetadataVersion(1, 4, 3)
    this[LanguageVersion.KOTLIN_1_5] = JvmMetadataVersion(1, 5, 1)
    this[LanguageVersion.KOTLIN_1_6] = JvmMetadataVersion(1, 6, 0)
    this[LanguageVersion.KOTLIN_1_7] = JvmMetadataVersion(1, 7, 0)
    this[LanguageVersion.KOTLIN_1_8] = JvmMetadataVersion(1, 8, 0)
    this[LanguageVersion.KOTLIN_1_9] = JvmMetadataVersion.INSTANCE
    this[LanguageVersion.KOTLIN_2_0] = JvmMetadataVersion(2, 0, 0)
    this[LanguageVersion.KOTLIN_2_1] = JvmMetadataVersion(2, 1, 0)

    check(size == LanguageVersion.values().size) {
        "Please add mappings from the missing LanguageVersion instances to the corresponding JvmMetadataVersion " +
                "in `LANGUAGE_TO_METADATA_VERSION`"
    }
}

fun LanguageVersion.toMetadataVersion(): JvmMetadataVersion = LANGUAGE_TO_METADATA_VERSION.getValue(this)

fun CompilerConfiguration.metadataVersion(
    languageVersion: LanguageVersion = languageVersionSettings.languageVersion
): BinaryVersion {
    return get(CommonConfigurationKeys.METADATA_VERSION) ?: languageVersion.toMetadataVersion()
}
