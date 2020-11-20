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

package org.jetbrains.kotlin.resolve.calls.model

import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintSystemError
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability

abstract class KotlinCallDiagnostic(val candidateApplicability: CandidateApplicability) {
    abstract fun report(reporter: DiagnosticReporter)
}

interface DiagnosticReporter {
    fun onExplicitReceiver(diagnostic: KotlinCallDiagnostic)

    fun onCall(diagnostic: KotlinCallDiagnostic)

    fun onTypeArguments(diagnostic: KotlinCallDiagnostic)

    fun onCallName(diagnostic: KotlinCallDiagnostic)

    fun onTypeArgument(typeArgument: TypeArgument, diagnostic: KotlinCallDiagnostic)

    fun onCallReceiver(callReceiver: SimpleKotlinCallArgument, diagnostic: KotlinCallDiagnostic)

    fun onCallArgument(callArgument: KotlinCallArgument, diagnostic: KotlinCallDiagnostic)
    fun onCallArgumentName(callArgument: KotlinCallArgument, diagnostic: KotlinCallDiagnostic)
    fun onCallArgumentSpread(callArgument: KotlinCallArgument, diagnostic: KotlinCallDiagnostic)

    fun constraintError(error: ConstraintSystemError)
}
