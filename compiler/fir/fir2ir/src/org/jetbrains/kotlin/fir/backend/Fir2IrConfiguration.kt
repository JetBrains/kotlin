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

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext

/**
 * @param allowNonCachedDeclarations
 *  Normally, FIR2IR caches all declarations it meets in a compiled module.
 *  It means asking for an IR element of a non-cached declaration is a sign of inconsistent state.
 *  Code generation in the IDE is trickier, though, as declarations from any module can be potentially referenced.
 *  For such a scenario, there is a flag that relaxes consistency checks.
 */
class Fir2IrConfiguration private constructor(
    val languageVersionSettings: LanguageVersionSettings,
    diagnosticReporter: BaseDiagnosticsCollector,
    val inlineConstTracker: InlineConstTracker?,
    val expectActualTracker: ExpectActualTracker?,
    val allowNonCachedDeclarations: Boolean,
    val skipBodies: Boolean,
    val irVerificationSettings: IrVerificationSettings,
    val carefulApproximationOfContravariantProjectionForSam: Boolean,
    val propagateLazyIrPrivateMembers: Boolean,
) {
    val diagnosticReporter: IrDiagnosticReporter =
        KtDiagnosticReporterWithImplicitIrBasedContext(diagnosticReporter, languageVersionSettings)

    class IrVerificationSettings(
        val mode: IrVerificationMode,
        val validateForKlibSerialization: Boolean,
        val disableIrCheckers: List<String>,
        val additionalIrCheckers: List<String>,
    )

    companion object {
        fun forJvmCompilation(
            compilerConfiguration: CompilerConfiguration,
            diagnosticReporter: BaseDiagnosticsCollector,
        ): Fir2IrConfiguration =
            Fir2IrConfiguration(
                languageVersionSettings = compilerConfiguration.languageVersionSettings,
                diagnosticReporter = diagnosticReporter,
                inlineConstTracker = compilerConfiguration[CommonConfigurationKeys.INLINE_CONST_TRACKER],
                expectActualTracker = compilerConfiguration[CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER],
                allowNonCachedDeclarations = false,
                skipBodies = compilerConfiguration.getBoolean(JVMConfigurationKeys.SKIP_BODIES),
                irVerificationSettings = IrVerificationSettings(
                    mode = compilerConfiguration.get(CommonConfigurationKeys.VERIFY_IR, IrVerificationMode.NONE),
                    disableIrCheckers = compilerConfiguration.disableIrCheckers,
                    additionalIrCheckers = compilerConfiguration.additionalIrCheckers,
                    validateForKlibSerialization = false,
                ),
                carefulApproximationOfContravariantProjectionForSam = compilerConfiguration.get(JVMConfigurationKeys.SAM_CONVERSIONS) != JvmClosureGenerationScheme.CLASS,
                propagateLazyIrPrivateMembers = false,
            )

        fun forJKlibCompilation(
            compilerConfiguration: CompilerConfiguration,
            diagnosticReporter: BaseDiagnosticsCollector,
        ): Fir2IrConfiguration =
            Fir2IrConfiguration(
                languageVersionSettings = compilerConfiguration.languageVersionSettings,
                diagnosticReporter = diagnosticReporter,
                inlineConstTracker = compilerConfiguration[CommonConfigurationKeys.INLINE_CONST_TRACKER],
                expectActualTracker = compilerConfiguration[CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER],
                allowNonCachedDeclarations = false,
                skipBodies = compilerConfiguration.getBoolean(JVMConfigurationKeys.SKIP_BODIES),
                irVerificationSettings = IrVerificationSettings(
                    mode = compilerConfiguration.get(CommonConfigurationKeys.VERIFY_IR, IrVerificationMode.NONE),
                    disableIrCheckers = compilerConfiguration.disableIrCheckers,
                    additionalIrCheckers = compilerConfiguration.additionalIrCheckers,
                    validateForKlibSerialization = false,
                ),
                carefulApproximationOfContravariantProjectionForSam = compilerConfiguration.get(JVMConfigurationKeys.SAM_CONVERSIONS) != JvmClosureGenerationScheme.CLASS,
                propagateLazyIrPrivateMembers = true,
            )

        fun forKlibCompilation(
            compilerConfiguration: CompilerConfiguration,
            diagnosticReporter: BaseDiagnosticsCollector,
        ): Fir2IrConfiguration =
            Fir2IrConfiguration(
                languageVersionSettings = compilerConfiguration.languageVersionSettings,
                diagnosticReporter = diagnosticReporter,
                inlineConstTracker = null,
                expectActualTracker = compilerConfiguration[CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER],
                allowNonCachedDeclarations = false,
                skipBodies = false,
                irVerificationSettings = IrVerificationSettings(
                    mode = compilerConfiguration.get(CommonConfigurationKeys.VERIFY_IR, IrVerificationMode.NONE),
                    disableIrCheckers = compilerConfiguration.disableIrCheckers,
                    additionalIrCheckers = compilerConfiguration.additionalIrCheckers,
                    validateForKlibSerialization = true,
                ),
                carefulApproximationOfContravariantProjectionForSam = false,
                propagateLazyIrPrivateMembers = false,
            )

        fun forAnalysisApi(
            compilerConfiguration: CompilerConfiguration,
            languageVersionSettings: LanguageVersionSettings,
            diagnosticReporter: BaseDiagnosticsCollector,
        ): Fir2IrConfiguration =
            Fir2IrConfiguration(
                languageVersionSettings = languageVersionSettings,
                diagnosticReporter = diagnosticReporter,
                inlineConstTracker = compilerConfiguration[CommonConfigurationKeys.INLINE_CONST_TRACKER],
                expectActualTracker = compilerConfiguration[CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER],
                allowNonCachedDeclarations = true,
                skipBodies = false,
                irVerificationSettings = IrVerificationSettings(
                    mode = compilerConfiguration.get(CommonConfigurationKeys.VERIFY_IR, IrVerificationMode.NONE),
                    disableIrCheckers = compilerConfiguration.disableIrCheckers,
                    additionalIrCheckers = compilerConfiguration.additionalIrCheckers,
                    validateForKlibSerialization = false,
                ),
                carefulApproximationOfContravariantProjectionForSam = compilerConfiguration.get(JVMConfigurationKeys.SAM_CONVERSIONS) != JvmClosureGenerationScheme.CLASS,
                propagateLazyIrPrivateMembers = false,
            )
    }
}
