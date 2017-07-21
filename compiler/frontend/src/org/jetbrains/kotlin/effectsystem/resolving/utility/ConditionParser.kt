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

package org.jetbrains.kotlin.effectsystem.resolving.utility

import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.effectsystem.functors.IsFunctor
import org.jetbrains.kotlin.effectsystem.impls.ESEqual
import org.jetbrains.kotlin.effectsystem.impls.ESIs
import org.jetbrains.kotlin.effectsystem.impls.ESVariable
import org.jetbrains.kotlin.effectsystem.resolving.*
import org.jetbrains.kotlin.effectsystem.structure.ESBooleanExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ConditionParser {
    fun parseCondition(resolvedCall: ResolvedCall<*>): ESBooleanExpression? {
        val joiner = getJoiner(resolvedCall) ?: return null

        val primitiveConditions = mutableListOf<ESBooleanExpression>()
        resolvedCall.resultingDescriptor.valueParameters.flatMapTo(primitiveConditions) { getConditionsOnArgument(it) }

        val conditionOnReceiver = resolvedCall.resultingDescriptor.extensionReceiverParameter?.let { getConditionsOnReceiver(it) } ?: emptyList()
        primitiveConditions.addAll(conditionOnReceiver)

        if (primitiveConditions.isEmpty()) return null

        return joiner.join(primitiveConditions)
    }

    private fun getJoiner(resolvedCall: ResolvedCall<*>): EffectsConditionsJoiners? {
        val joiningStrategyName = resolvedCall.resultingDescriptor.annotations
                .findAnnotation(CONDITION_JOINING_ANNOTATION)?.allValueArguments?.values?.single()?.safeAs<EnumValue>()?.value?.name?.identifier
        return EffectsConditionsJoiners.safeValueOf(joiningStrategyName)
    }


    private fun getConditionsOnReceiver(receiverParameter: ReceiverParameterDescriptor): List<ESBooleanExpression> {
        val variable = receiverParameter.extensionReceiverToESVariable()

        val isNegated = receiverParameter.type.annotations.getAllAnnotations().any {
            it.annotation.annotationClass?.fqNameEquals(NOT_CONDITION) ?: false
        }

        return receiverParameter.type.annotations.getAllAnnotations().mapNotNull {
            it.annotation.parseCondition(variable, isNegated)
        }
    }

    private fun getConditionsOnArgument(parameterDescriptor: ValueParameterDescriptor): List<ESBooleanExpression> {
        val parameterVariable = parameterDescriptor.toESVariable()

        val isNegated = parameterDescriptor.annotations.findAnnotation(NOT_CONDITION) != null

        return parameterDescriptor.annotations.mapNotNull { it.parseCondition(parameterVariable, isNegated) }
    }

    private fun AnnotationDescriptor.parseCondition(subjectVariable: ESVariable, isNegated: Boolean): ESBooleanExpression? {
        return when {
            this.annotationClass.fqNameEquals(EQUALS_CONDITION) ->
                ESEqual(subjectVariable, allValueArguments.values.single().toESConstant() ?: return null, isNegated)

            this.annotationClass.fqNameEquals(IS_INSTANCE_CONDITION) ->
                ESIs(subjectVariable, IsFunctor(allValueArguments.values.single().value as KotlinType, isNegated))

            else -> null
        }
    }
}