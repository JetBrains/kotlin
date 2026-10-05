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
        languageVersion: LanguageVersion,
    ): MutableMap<LanguageFeature, LanguageFeature.State> = with(arguments) {
        require(this is K2JKlibCompilerArguments)
        val result = super.configureLanguageFeatures(arguments, reporter, languageVersion)
        if (typeEnhancementImprovementsInStrictMode) {
            result[LanguageFeature.TypeEnhancementImprovementsInStrictMode] = LanguageFeature.State.ENABLED
        }
        if (enhanceTypeParameterTypesToDefNotNull) {
            result[LanguageFeature.ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated] = LanguageFeature.State.ENABLED
        }
        if (configureJvmDefaultMode(null)?.isEnabled == true) {
            result[LanguageFeature.ForbidSuperDelegationToAbstractFakeOverride] =
                LanguageFeature.State.ENABLED
        }
        return result
    }

    // TODO(KT-87172): Remove if we decide to extend CommonKlibBasedCompilerArguments.
    //  Copy of CommonKlibBasedCompilerArgumentsConfigurator.configureExtraLanguageFeatures.
    override fun configureExtraLanguageFeatures(
        arguments: CommonCompilerArguments,
        map: HashMap<LanguageFeature, LanguageFeature.State>,
        reporter: Reporter,
    ) {
        require(arguments is K2JKlibCompilerArguments)

        when (KlibIrInlinerMode.fromString(arguments.irInlinerBeforeKlibSerialization)) {
            KlibIrInlinerMode.DEFAULT -> {
                // Do nothing. Rely on the default language feature states.
            }
            KlibIrInlinerMode.INTRA_MODULE -> {
                map[LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization] = LanguageFeature.State.ENABLED
                map[LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization] = LanguageFeature.State.DISABLED
            }
            KlibIrInlinerMode.FULL -> {
                map[LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization] = LanguageFeature.State.ENABLED
                map[LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization] = LanguageFeature.State.ENABLED
            }
            KlibIrInlinerMode.DISABLED -> {
                map[LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization] = LanguageFeature.State.DISABLED
                map[LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization] = LanguageFeature.State.DISABLED
            }
            null -> {
                reporter.reportError(
                    "Unknown value for parameter -Xklib-ir-inliner: '${arguments.irInlinerBeforeKlibSerialization}'. " +
                            "Value should be one of ${KlibIrInlinerMode.availableValues()}"
                )
            }
        }
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
