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

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.incremental.components.InlineConstTracker

/**
 * @param allowNonCachedDeclarations
 *  Normally, FIR-to-IR caches all declarations it meets in a compiled module.
 *  It means asking for an IR element of a non-cached declaration is a sign of inconsistent state.
 *  Code generation in the IDE is trickier, though, as declarations from any module can be potentially referenced.
 *  For such a scenario, there is a flag that relaxes consistency checks.
 */
data class Fir2IrConfiguration(
    val languageVersionSettings: LanguageVersionSettings,
    val diagnosticReporter: DiagnosticReporter,
    val linkViaSignatures: Boolean,
    val evaluatedConstTracker: EvaluatedConstTracker,
    val inlineConstTracker: InlineConstTracker?,
    val allowNonCachedDeclarations: Boolean,
)
