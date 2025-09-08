/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*

class K2JVMCompilerArgumentsConfigurator : CommonCompilerArgumentsConfigurator() {
    override fun configureAnalysisFlags(
        arguments: CommonCompilerArguments,
        collector: MessageCollector,
        languageVersion: LanguageVersion,
    ): MutableMap<AnalysisFlag<*>, Any> = with(arguments) {
        require(this is K2JVMCompilerArguments)
        val result = super.configureAnalysisFlags(arguments, collector, languageVersion)
        result[JvmAnalysisFlags.strictMetadataVersionSemantics] = strictMetadataVersionSemantics
        result[JvmAnalysisFlags.javaTypeEnhancementState] = JavaTypeEnhancementStateParser(collector, languageVersion.toKotlinVersion())
            .parse(jsr305, supportCompatqualCheckerFrameworkAnnotations, jspecifyAnnotations, nullabilityAnnotations)
        result[AnalysisFlags.ignoreDataFlowInAssert] = JVMAssertionsMode.fromString(assertionsMode) != JVMAssertionsMode.LEGACY

        configureJvmDefaultMode(collector)?.let {
            result[JvmAnalysisFlags.jvmDefaultMode] = it
            @Suppress("DEPRECATION")
            if (jvmDefault != null) {
                collector.report(CompilerMessageSeverity.STRONG_WARNING, "-Xjvm-default is deprecated. Use -jvm-default instead.")
            }
        }

        result[JvmAnalysisFlags.inheritMultifileParts] = inheritMultifileParts
        result[JvmAnalysisFlags.sanitizeParentheses] = sanitizeParentheses
        result[JvmAnalysisFlags.suppressMissingBuiltinsError] = suppressMissingBuiltinsError
        result[JvmAnalysisFlags.enableJvmPreview] = enableJvmPreview
        result[AnalysisFlags.allowUnstableDependencies] = allowUnstableDependencies
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

    @Suppress("DEPRECATION")
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
        require(this is K2JVMCompilerArguments)
        val result = super.configureLanguageFeatures(arguments, collector)
        result.configureJvmLanguageFeatures(this)

        if (indyAllowAnnotatedLambdas == true) {
            result[LanguageFeature.JvmIndyAllowLambdasWithAnnotations] = LanguageFeature.State.ENABLED
        } else if (indyAllowAnnotatedLambdas == false) {
            result[LanguageFeature.JvmIndyAllowLambdasWithAnnotations] = LanguageFeature.State.DISABLED
        }

        // If a JVM default mode is enabled via `-jvm-default` or `-Xjvm-default`, also forcibly enable a few flags that fix incomplete
        // error reporting in some cases.
        // Note that this won't have effect if a JVM default mode is enabled by other means, specifically if:
        // * `JvmDefaultEnableByDefault` is either enabled manually or automatically (if LV is 2.2+).
        //   In this case, both flags will be enabled simply because their `sinceVersion` is <= 1.9.
        if (configureJvmDefaultMode(null)?.isEnabled == true) {
            result[LanguageFeature.ForbidSuperDelegationToAbstractFakeOverride] = LanguageFeature.State.ENABLED
        }

        return result
    }
}
