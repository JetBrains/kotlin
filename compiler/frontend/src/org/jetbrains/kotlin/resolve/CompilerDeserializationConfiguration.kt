/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.util.toMetadataVersion

class CommonCompilerDeserializationConfiguration(
    val languageVersionSettings: LanguageVersionSettings,
) : DeserializationConfiguration {
    override val metadataVersion: MetadataVersion = languageVersionSettings.languageVersion.toMetadataVersion()

    override val skipMetadataVersionCheck = languageVersionSettings.getFlag(AnalysisFlags.skipMetadataVersionCheck)

    override val skipPrereleaseCheck = languageVersionSettings.getFlag(AnalysisFlags.skipPrereleaseCheck)

    override val reportErrorsOnPreReleaseDependencies =
        !skipPrereleaseCheck && !languageVersionSettings.isPreRelease() && !KotlinCompilerVersion.isPreRelease()

    override val allowUnstableDependencies = languageVersionSettings.getFlag(AnalysisFlags.allowUnstableDependencies)

    override val typeAliasesAllowed = languageVersionSettings.supportsFeature(LanguageFeature.TypeAliases)

    override val isJvmPackageNameSupported = languageVersionSettings.supportsFeature(LanguageFeature.JvmPackageName)

    override val readDeserializedContracts: Boolean =
        languageVersionSettings.supportsFeature(LanguageFeature.ReadDeserializedContracts)
}

// TODO(KT-85611): Remove after updating to CommonCompilerDeserializationConfiguration in IntelliJ
typealias KlibCompilerDeserializationConfiguration = CommonCompilerDeserializationConfiguration
