/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion

class K2MetadataCompilerArgumentsConfigurator : CommonCompilerArgumentsConfigurator() {
    override fun configureAnalysisFlags(
        arguments: CommonCompilerArguments,
        reporter: Reporter,
        languageVersion: LanguageVersion,
    ): MutableMap<AnalysisFlag<*>, Any> = with(arguments) {
        require(this is K2MetadataCompilerArguments)
        super.configureAnalysisFlags(arguments, reporter, languageVersion).also {
            it[AnalysisFlags.metadataCompilation] = true
        }
    }

    override fun configureExtraLanguageFeatures(
        arguments: CommonCompilerArguments,
        map: HashMap<LanguageFeature, LanguageFeature.State>,
        reporter: Reporter,
    ) {
        map[LanguageFeature.MultiPlatformProjects] = LanguageFeature.State.ENABLED
    }
}
