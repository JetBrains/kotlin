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

class CompilerDeserializationConfiguration(languageVersionSettings: LanguageVersionSettings) : DeserializationConfiguration {
    override val skipMetadataVersionCheck = languageVersionSettings.getFlag(AnalysisFlags.skipMetadataVersionCheck)

    override val reportErrorsOnPreReleaseDependencies =
        !skipMetadataVersionCheck && !languageVersionSettings.isPreRelease() && !KotlinCompilerVersion.isPreRelease()

    override val reportErrorsOnIrDependencies = languageVersionSettings.getFlag(AnalysisFlags.reportErrorsOnIrDependencies)

    override val typeAliasesAllowed = languageVersionSettings.supportsFeature(LanguageFeature.TypeAliases)

    override val isJvmPackageNameSupported = languageVersionSettings.supportsFeature(LanguageFeature.JvmPackageName)

    override val readDeserializedContracts: Boolean = languageVersionSettings.supportsFeature(LanguageFeature.ReadDeserializedContracts)

    override val releaseCoroutines: Boolean = languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines)
}
