/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.config.*

abstract class CommonKlibBasedCompilerArgumentsConfigurator : CommonCompilerArgumentsConfigurator() {
    abstract fun isSecondStage(arguments: CommonCompilerArguments): Boolean

    override fun configureAnalysisFlags(
        arguments: CommonCompilerArguments,
        reporter: Reporter,
        languageVersion: LanguageVersion,
    ): MutableMap<AnalysisFlag<*>, Any> {
        require(arguments is CommonKlibBasedCompilerArguments)

        return super.configureAnalysisFlags(arguments, reporter, languageVersion).apply {
            if (isSecondStage(arguments)) {
                addPartialLinkageToWarningLevelMap(arguments, reporter)
            }
        }
    }

    private fun MutableMap<AnalysisFlag<*>, Any>.addPartialLinkageToWarningLevelMap(
        arguments: CommonKlibBasedCompilerArguments,
        reporter: Reporter,
    ) {
        val partialLinkageLogLevel = arguments.partialLinkageLogLevel

        val logLevel = if (partialLinkageLogLevel != null) {
            PartialLinkageLogLevel.resolveLogLevel(partialLinkageLogLevel)
        } else {
            PartialLinkageLogLevel.DEFAULT
        }

        if (logLevel == null) {
            reporter.reportError(
                "Unknown value for parameter -Xpartial-linkage-loglevel: '$partialLinkageLogLevel'. Value should be one of ${PartialLinkageLogLevel.availableValues()}",
            )
            return
        }

        @Suppress("UNCHECKED_CAST")
        val currentWarningLevels = this[AnalysisFlags.warningLevels] as? Map<String, WarningLevel> ?: emptyMap()
        val updatedWarningLevels = currentWarningLevels.toMutableMap().apply {
            putPartialLinkageIssueWarningLevel(isMinorIssue = true, logLevel, reporter)
            putPartialLinkageIssueWarningLevel(isMinorIssue = false, logLevel, reporter)
        }

        putAnalysisFlag(AnalysisFlags.warningLevels, updatedWarningLevels)
    }

    private fun MutableMap<String, WarningLevel>.putPartialLinkageIssueWarningLevel(
        isMinorIssue: Boolean,
        logLevel: PartialLinkageLogLevel,
        reporter: Reporter,
    ) {
        val warningLevel = when (logLevel) {
            PartialLinkageLogLevel.SILENT -> WarningLevel.Disabled
            PartialLinkageLogLevel.INFO -> return
            PartialLinkageLogLevel.WARNING -> if (isMinorIssue) WarningLevel.Disabled else return
            PartialLinkageLogLevel.ERROR -> WarningLevel.Error
        }

        val diagnosticName = "${if (isMinorIssue) "MINOR" else "MAJOR"}_PARTIAL_LINKAGE_ISSUE"

        val existingLevel = put(diagnosticName, warningLevel)
        if (existingLevel != null) {
            reporter.reportError(
                "Severity of $diagnosticName is configured both with -Xpartial-linkage-loglevel and -Xwarning-level or -Xsuppress-warning flags"
            )
        }
    }

    override fun configureExtraLanguageFeatures(
        arguments: CommonCompilerArguments,
        map: HashMap<LanguageFeature, LanguageFeature.State>,
        reporter: Reporter,
    ) {
        require(arguments is CommonKlibBasedCompilerArguments)

        val klibIrInlinerMode = KlibIrInlinerMode.fromString(arguments.irInlinerBeforeKlibSerialization)
        when (klibIrInlinerMode) {
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
                // TODO(KT-71896): Drop this reporting when the cross-inlining becomes enabled by default.
                reporter.info(
                    "`-Xklib-ir-inliner=full` will trigger setting the `pre-release` flag for the compiled library."
                )
            }
            KlibIrInlinerMode.DISABLED -> {
                map[LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization] = LanguageFeature.State.DISABLED
                map[LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization] = LanguageFeature.State.DISABLED
            }
            null -> {
                reporter.reportError(
                    "Unknown value for parameter -Xklib-ir-inliner: '${arguments.irInlinerBeforeKlibSerialization}'. Value should be one of ${KlibIrInlinerMode.availableValues()}"
                )
            }
        }
    }
}
