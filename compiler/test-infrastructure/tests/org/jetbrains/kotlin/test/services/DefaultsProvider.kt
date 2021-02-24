/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.LanguageVersionSettingsBuilder
import org.jetbrains.kotlin.test.model.BinaryKind
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.TestArtifactKind

/*
 * TODO:
 *   - default target artifact
 *   - default libraries
 */
class DefaultsProvider(
    val defaultFrontend: FrontendKind<*>,
    val defaultLanguageSettings: LanguageVersionSettings,
    private val defaultLanguageSettingsBuilder: LanguageVersionSettingsBuilder,
    val defaultPlatform: TargetPlatform,
    val defaultArtifactKind: BinaryKind<*>?,
    val defaultTargetBackend: TargetBackend?,
    val defaultDependencyKind: DependencyKind
) : TestService {
    fun newLanguageSettingsBuilder(): LanguageVersionSettingsBuilder {
        return LanguageVersionSettingsBuilder.fromExistingSettings(defaultLanguageSettingsBuilder)
    }
}

val TestServices.defaultsProvider: DefaultsProvider by TestServices.testServiceAccessor()

@DslMarker
annotation class DefaultsDsl
