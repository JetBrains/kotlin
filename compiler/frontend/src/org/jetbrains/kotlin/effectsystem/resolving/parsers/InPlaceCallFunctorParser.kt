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

package org.jetbrains.kotlin.effectsystem.resolving.parsers

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.effectsystem.effects.InvocationKind
import org.jetbrains.kotlin.effectsystem.functors.InPlaceCallFunctor
import org.jetbrains.kotlin.effectsystem.resolving.CALLS_EFFECT
import org.jetbrains.kotlin.effectsystem.resolving.FunctorParser
import org.jetbrains.kotlin.effectsystem.structure.ESFunctor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class InPlaceCallFunctorParser : FunctorParser {

    override fun tryParseFunctor(resolvedCall: ResolvedCall<*>): ESFunctor? {
        val argsEffects = mutableListOf<AnnotationDescriptor>()
        val isRelevantArg = mutableListOf<Boolean>()

        val allParametersIncludingReceiver =
                listOf(resolvedCall.resultingDescriptor.extensionReceiverParameter).filterNotNull() + resolvedCall.resultingDescriptor.valueParameters

        allParametersIncludingReceiver.forEach { param ->
            val callsEffect = param.annotations.findAnnotation(CALLS_EFFECT)
            if (callsEffect != null) {
                argsEffects.add(callsEffect)
            }
            isRelevantArg.add(callsEffect != null)
        }

        if (argsEffects.isEmpty()) return null

        assert(argsEffects.size == 1) { "Multi-effect annotations are not supported yet" }

        val invocationCount = argsEffects[0].allValueArguments.values.singleOrNull().toInvocationCountEnum()
        return InPlaceCallFunctor(invocationCount, isRelevantArg)
    }

    private fun ConstantValue<*>?.toInvocationCountEnum(): InvocationKind =
            when (this.safeAs<EnumValue>()?.value?.name?.identifier) {
                "AT_MOST_ONCE" -> InvocationKind.AT_MOST_ONCE
                "EXACTLY_ONCE" -> InvocationKind.EXACTLY_ONCE
                "AT_LEAST_ONCE" -> InvocationKind.AT_LEAST_ONCE
                "UNKNOWN" -> InvocationKind.UNKNOWN
                null -> InvocationKind.UNKNOWN
                else -> throw IllegalStateException("Unknown invocation type enum: $this")
            }
}