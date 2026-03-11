/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.config.*

class K2JKlibCompilerArgumentsConfigurator : CommonCompilerArgumentsConfigurator() {
    override fun configureAnalysisFlags(
        arguments: CommonCompilerArguments,
        reporter: Reporter,
        languageVersion: LanguageVersion,
    ): MutableMap<AnalysisFlag<*>, Any> = with(arguments) {
        require(this is K2JKlibCompilerArguments)
        val result = super.configureAnalysisFlags(arguments, reporter, languageVersion)
        result[JvmAnalysisFlags.javaTypeEnhancementState] = JavaTypeEnhancementStateParser(reporter, languageVersion.toKotlinVersion())
            .parse(jsr305, supportCompatqualCheckerFrameworkAnnotations, jspecifyAnnotations, nullabilityAnnotations)

        configureJvmDefaultMode(reporter)?.let { result[JvmAnalysisFlags.jvmDefaultMode] = it }
        result[JvmAnalysisFlags.inheritMultifileParts] = inheritMultifileParts
        result[JvmAnalysisFlags.outputBuiltinsMetadata] = outputBuiltinsMetadata
        return result
    }

    override fun configureLanguageFeatures(
        arguments: CommonCompilerArguments,
        reporter: Reporter,
    ): MutableMap<LanguageFeature, LanguageFeature.State> = with(arguments) {
        require(this is K2JKlibCompilerArguments)
        val result = super.configureLanguageFeatures(arguments, reporter)
        if (typeEnhancementImprovementsInStrictMode) {
            result[LanguageFeature.TypeEnhancementImprovementsInStrictMode] = LanguageFeature.State.ENABLED
        }
        if (enhanceTypeParameterTypesToDefNotNull) {
            result[LanguageFeature.ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated] = LanguageFeature.State.ENABLED
        }
        if (valueClasses) {
            result[LanguageFeature.JvmInlineMultiFieldValueClasses] = LanguageFeature.State.ENABLED
        }
        if (configureJvmDefaultMode(null)?.isEnabled == true) {
            result[LanguageFeature.ForbidSuperDelegationToAbstractFakeOverride] =
                LanguageFeature.State.ENABLED
        }
        return result
    }

    private fun K2JKlibCompilerArguments.configureJvmDefaultMode(
        reporter: Reporter?,
    ): JvmDefaultMode? =
        when {
            jvmDefault != null ->
                JvmDefaultMode.fromStringOrNull(jvmDefault).also {
                    if (it == null) {
                        reporter?.reportError(
                            "Unknown -jvm-default mode: $jvmDefault, supported modes: " +
                                    "${JvmDefaultMode.entries.map(JvmDefaultMode::description)}",
                        )
                    }
                }

            else -> null
        }
}
