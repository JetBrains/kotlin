/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration

class CompilerDeserializationConfiguration(languageVersionSettings: LanguageVersionSettings) : DeserializationConfiguration {
    override val skipMetadataVersionCheck = languageVersionSettings.getFlag(AnalysisFlag.skipMetadataVersionCheck)

    override val skipPreReleaseCheck = skipMetadataVersionCheck || !languageVersionSettings.languageVersion.isStable

    override val typeAliasesAllowed = languageVersionSettings.supportsFeature(LanguageFeature.TypeAliases)

    override val isJvmPackageNameSupported = languageVersionSettings.supportsFeature(LanguageFeature.JvmPackageName)

    override val returnsEffectAllowed: Boolean = languageVersionSettings.supportsFeature(LanguageFeature.ReturnsEffect)

    override val callsInPlaceEffectAllowed: Boolean = languageVersionSettings.supportsFeature(LanguageFeature.CallsInPlaceEffect)
}
