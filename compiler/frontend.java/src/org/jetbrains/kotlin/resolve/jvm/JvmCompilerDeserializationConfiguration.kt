/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.utils.toMetadataVersion

class JvmCompilerDeserializationConfiguration(
    languageVersionSettings: LanguageVersionSettings
) : CompilerDeserializationConfiguration(languageVersionSettings) {
    override val binaryVersion: JvmMetadataVersion
        get() = languageVersionSettings.languageVersion.toMetadataVersion()
}