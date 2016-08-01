/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallDiagnostic
import org.jetbrains.kotlin.resolve.calls.model.ResolutionPart
import org.jetbrains.kotlin.resolve.calls.model.SimpleKotlinResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.tower.InfixCallNoInfixModifier
import org.jetbrains.kotlin.resolve.calls.tower.InvokeConventionCallNoOperatorModifier

object CheckInfixResolutionPart : ResolutionPart {
    override fun SimpleKotlinResolutionCandidate.process(): List<KotlinCallDiagnostic> {
        if (kotlinCall.isInfixCall && (candidateDescriptor !is FunctionDescriptor || !candidateDescriptor.isInfix)) {
            return listOf(InfixCallNoInfixModifier)
        }

        return emptyList()
    }
}

object CheckOperatorResolutionPart : ResolutionPart {
    override fun SimpleKotlinResolutionCandidate.process(): List<KotlinCallDiagnostic> {
        if (kotlinCall.isOperatorCall && (candidateDescriptor !is FunctionDescriptor || !candidateDescriptor.isOperator)) {
            return listOf(InvokeConventionCallNoOperatorModifier)
        }

        return emptyList()
    }
}