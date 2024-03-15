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
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker

/**
 * @param allowNonCachedDeclarations
 *  Normally, FIR2IR caches all declarations it meets in a compiled module.
 *  It means asking for an IR element of a non-cached declaration is a sign of inconsistent state.
 *  Code generation in the IDE is trickier, though, as declarations from any module can be potentially referenced.
 *  For such a scenario, there is a flag that relaxes consistency checks.
 *
 * @param useFirBasedFakeOverrideGenerator
 *  Generate all fake overrides via FIR2IR instead of IR, i.e. revert to behavior before
 *  [KT-61514](https://youtrack.jetbrains.com/issue/KT-61514) was resolved.
 */
class Fir2IrConfiguration private constructor(
    val languageVersionSettings: LanguageVersionSettings,
    val diagnosticReporter: DiagnosticReporter,
    val evaluatedConstTracker: EvaluatedConstTracker,
    val inlineConstTracker: InlineConstTracker?,
    val expectActualTracker: ExpectActualTracker?,
    val allowNonCachedDeclarations: Boolean,
    val useFirBasedFakeOverrideGenerator: Boolean,
) {
    companion object {
        fun forJvmCompilation(
            compilerConfiguration: CompilerConfiguration,
            diagnosticReporter: DiagnosticReporter,
        ): Fir2IrConfiguration =
            Fir2IrConfiguration(
                languageVersionSettings = compilerConfiguration.languageVersionSettings,
                diagnosticReporter = diagnosticReporter,
                evaluatedConstTracker = compilerConfiguration.putIfAbsent(
                    CommonConfigurationKeys.EVALUATED_CONST_TRACKER,
                    EvaluatedConstTracker.create(),
                ),
                inlineConstTracker = compilerConfiguration[CommonConfigurationKeys.INLINE_CONST_TRACKER],
                expectActualTracker = compilerConfiguration[CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER],
                allowNonCachedDeclarations = false,
                useFirBasedFakeOverrideGenerator = compilerConfiguration.getBoolean(CommonConfigurationKeys.USE_FIR_BASED_FAKE_OVERRIDE_GENERATOR),
            )

        fun forKlibCompilation(
            compilerConfiguration: CompilerConfiguration,
            diagnosticReporter: DiagnosticReporter,
        ): Fir2IrConfiguration =
            Fir2IrConfiguration(
                languageVersionSettings = compilerConfiguration.languageVersionSettings,
                diagnosticReporter = diagnosticReporter,
                evaluatedConstTracker = compilerConfiguration.putIfAbsent(
                    CommonConfigurationKeys.EVALUATED_CONST_TRACKER,
                    EvaluatedConstTracker.create(),
                ),
                inlineConstTracker = null,
                expectActualTracker = compilerConfiguration[CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER],
                allowNonCachedDeclarations = false,
                useFirBasedFakeOverrideGenerator = false,
            )

        fun forAnalysisApi(
            compilerConfiguration: CompilerConfiguration,
            languageVersionSettings: LanguageVersionSettings,
            diagnosticReporter: DiagnosticReporter,
        ): Fir2IrConfiguration =
            Fir2IrConfiguration(
                languageVersionSettings = languageVersionSettings,
                diagnosticReporter = diagnosticReporter,
                evaluatedConstTracker = compilerConfiguration.putIfAbsent(
                    CommonConfigurationKeys.EVALUATED_CONST_TRACKER,
                    EvaluatedConstTracker.create(),
                ),
                inlineConstTracker = compilerConfiguration[CommonConfigurationKeys.INLINE_CONST_TRACKER],
                expectActualTracker = compilerConfiguration[CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER],
                allowNonCachedDeclarations = true,
                useFirBasedFakeOverrideGenerator = compilerConfiguration.getBoolean(CommonConfigurationKeys.USE_FIR_BASED_FAKE_OVERRIDE_GENERATOR),
            )
    }
}
