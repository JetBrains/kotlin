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
//        result[JvmAnalysisFlags.sanitizeParentheses] = sanitizeParentheses
//        result[JvmAnalysisFlags.suppressMissingBuiltinsError] = suppressMissingBuiltinsError
//        result[JvmAnalysisFlags.enableJvmPreview] = enableJvmPreview
//        result[AnalysisFlags.allowUnstableDependencies] = allowUnstableDependencies
        result[JvmAnalysisFlags.outputBuiltinsMetadata] = outputBuiltinsMetadata
        if (expectBuiltinsAsPartOfStdlib && !stdlibCompilation) {
            collector.report(
                CompilerMessageSeverity.ERROR,
                "-Xcompile-builtins-as-part-of-stdlib must not be used without -Xstdlib-compilation"
            )
        }
        result[JvmAnalysisFlags.expectBuiltinsAsPartOfStdlib] = expectBuiltinsAsPartOfStdlib
        return result
    }

    private fun K2JVMCompilerArguments.configureJvmDefaultMode(collector: MessageCollector?): JvmDefaultMode? = when {
        jvmDefaultStable != null -> JvmDefaultMode.fromStringOrNull(jvmDefaultStable).also {
            if (it == null) {
                collector?.report(
                    CompilerMessageSeverity.ERROR,
                    "Unknown -jvm-default mode: $jvmDefaultStable, supported modes: " +
                            "${JvmDefaultMode.entries.map(JvmDefaultMode::description)}"
                )
            }
        }
        jvmDefault != null -> JvmDefaultMode.fromStringOrNullOld(jvmDefault).also {
            if (it == null) {
                collector?.report(
                    CompilerMessageSeverity.ERROR,
                    "Unknown -Xjvm-default mode: $jvmDefault, supported modes: " +
                            "${JvmDefaultMode.entries.map(JvmDefaultMode::oldDescription)}"
                )
            }
        }
        else -> null
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
//        if (annotationsInMetadata) {
//            result[LanguageFeature.AnnotationsInMetadata] = LanguageFeature.State.ENABLED
//        }
//        if (!indyAllowAnnotatedLambdas) {
//            result[LanguageFeature.JvmIndyAllowLambdasWithAnnotations] = LanguageFeature.State.DISABLED
//        }

        return result
    }
}
