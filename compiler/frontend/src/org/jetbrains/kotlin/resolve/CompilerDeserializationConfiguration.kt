/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.util.toJvmMetadataVersion
import org.jetbrains.kotlin.util.toKlibMetadataVersion

abstract class CommonCompilerDeserializationConfiguration(
    protected val languageVersionSettings: LanguageVersionSettings,
    toMetadataVersion: LanguageVersion.() -> MetadataVersion
) : DeserializationConfiguration {
    override val metadataVersion: MetadataVersion = languageVersionSettings.languageVersion.toMetadataVersion()

    final override val skipMetadataVersionCheck = languageVersionSettings.getFlag(AnalysisFlags.skipMetadataVersionCheck)

    final override val skipPrereleaseCheck = languageVersionSettings.getFlag(AnalysisFlags.skipPrereleaseCheck)

    final override val reportErrorsOnPreReleaseDependencies =
        !skipPrereleaseCheck && !languageVersionSettings.isPreRelease() && !KotlinCompilerVersion.isPreRelease()

    final override val allowUnstableDependencies = languageVersionSettings.getFlag(AnalysisFlags.allowUnstableDependencies)

    final override val typeAliasesAllowed = languageVersionSettings.supportsFeature(LanguageFeature.TypeAliases)

    final override val isJvmPackageNameSupported = languageVersionSettings.supportsFeature(LanguageFeature.JvmPackageName)

    final override val readDeserializedContracts: Boolean =
        languageVersionSettings.supportsFeature(LanguageFeature.ReadDeserializedContracts)
}

// TODO KT-76195 Consider replacing the following classes with `CommonCompilerDeserializationConfiguration` in version 2.4
class JvmCompilerDeserializationConfiguration(languageVersionSettings: LanguageVersionSettings) :
    CommonCompilerDeserializationConfiguration(languageVersionSettings, LanguageVersion::toJvmMetadataVersion)

class KlibCompilerDeserializationConfiguration(languageVersionSettings: LanguageVersionSettings) :
    CommonCompilerDeserializationConfiguration(languageVersionSettings, LanguageVersion::toKlibMetadataVersion)
