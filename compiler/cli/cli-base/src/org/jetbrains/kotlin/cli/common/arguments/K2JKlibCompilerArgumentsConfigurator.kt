/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*

class K2JKlibCompilerArgumentsConfigurator : CommonCompilerArgumentsConfigurator() {
    override fun configureAnalysisFlags(
        arguments: CommonCompilerArguments,
        collector: MessageCollector,
        languageVersion: LanguageVersion,
    ): MutableMap<AnalysisFlag<*>, Any> = with(arguments) {
        require(this is K2JKlibCompilerArguments)
        val result = super.configureAnalysisFlags(arguments, collector, languageVersion)
        result[JvmAnalysisFlags.javaTypeEnhancementState] = JavaTypeEnhancementStateParser(collector, languageVersion.toKotlinVersion())
            .parse(jsr305, supportCompatqualCheckerFrameworkAnnotations, jspecifyAnnotations, nullabilityAnnotations)

        result[JvmAnalysisFlags.inheritMultifileParts] = inheritMultifileParts
        result[JvmAnalysisFlags.outputBuiltinsMetadata] = outputBuiltinsMetadata
        return result
    }

    override fun configureLanguageFeatures(
        arguments: CommonCompilerArguments,
        collector: MessageCollector,
    ): MutableMap<LanguageFeature, LanguageFeature.State> = with(arguments) {
        require(this is K2JKlibCompilerArguments)
        val result = super.configureLanguageFeatures(arguments, collector)
        if (typeEnhancementImprovementsInStrictMode) {
            result[LanguageFeature.TypeEnhancementImprovementsInStrictMode] = LanguageFeature.State.ENABLED
        }
        if (enhanceTypeParameterTypesToDefNotNull) {
            result[LanguageFeature.ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated] = LanguageFeature.State.ENABLED
        }
        if (valueClasses) {
            result[LanguageFeature.ValueClasses] = LanguageFeature.State.ENABLED
        }
        return result
    }
}
