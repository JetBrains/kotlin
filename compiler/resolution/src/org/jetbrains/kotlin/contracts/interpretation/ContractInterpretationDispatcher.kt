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

package org.jetbrains.kotlin.contracts.interpretation

import org.jetbrains.kotlin.contracts.description.BooleanExpression
import org.jetbrains.kotlin.contracts.description.ConditionalEffectDeclaration
import org.jetbrains.kotlin.contracts.description.ContractDescription
import org.jetbrains.kotlin.contracts.description.EffectDeclaration
import org.jetbrains.kotlin.contracts.description.expressions.ConstantReference
import org.jetbrains.kotlin.contracts.description.expressions.VariableReference
import org.jetbrains.kotlin.contracts.model.ESEffect
import org.jetbrains.kotlin.contracts.model.ESExpression
import org.jetbrains.kotlin.contracts.model.Functor
import org.jetbrains.kotlin.contracts.model.functors.SubstitutingFunctor
import org.jetbrains.kotlin.contracts.model.structure.ESConstant
import org.jetbrains.kotlin.contracts.model.structure.ESVariable

/**
 * This class manages conversion of [ContractDescription] to [Functor]
 */
class ContractInterpretationDispatcher {
    private val constantsInterpreter = ConstantValuesInterpreter()
    private val conditionInterpreter = ConditionInterpreter(this)
    private val conditionalEffectInterpreter = ConditionalEffectInterpreter(this)
    private val effectsInterpreters: List<EffectDeclarationInterpreter> = listOf(
        ReturnsEffectInterpreter(this),
        CallsEffectInterpreter(this)
    )

    fun convertContractDescriptorToFunctor(contractDescription: ContractDescription): Functor? {
        val resultingClauses = mutableListOf<ESEffect>()
        for (effect in contractDescription.effects) {
            if (effect is ConditionalEffectDeclaration) {
                resultingClauses.add(conditionalEffectInterpreter.interpret(effect) ?: return null)
            } else {
                val interpreted = effectsInterpreters.mapNotNull { it.tryInterpret(effect) }
                when (interpreted.size) {
                    0 -> continue  // Unknown effect type (e.g. returnsResultOf from K2-compiled metadata) — skip gracefully
                    1 -> resultingClauses.add(interpreted[0])
                    else -> return null  // Multiple interpreters claimed the same effect — should not happen
                }
            }
        }

        return SubstitutingFunctor(resultingClauses, contractDescription.ownerFunction)
    }

    internal fun interpretEffect(effectDeclaration: EffectDeclaration): ESEffect? {
        val convertedFunctors = effectsInterpreters.mapNotNull { it.tryInterpret(effectDeclaration) }
        return convertedFunctors.singleOrNull()
    }

    internal fun interpretConstant(constantReference: ConstantReference): ESConstant? =
        constantsInterpreter.interpretConstant(constantReference)

    internal fun interpretCondition(booleanExpression: BooleanExpression): ESExpression? =
        booleanExpression.accept(conditionInterpreter, Unit)

    internal fun interpretVariable(variableReference: VariableReference): ESVariable? = ESVariable(variableReference.descriptor)
}
