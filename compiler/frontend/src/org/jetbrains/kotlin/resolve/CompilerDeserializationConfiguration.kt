/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration

open class CompilerDeserializationConfiguration(
    protected val languageVersionSettings: LanguageVersionSettings
) : DeserializationConfiguration {

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
