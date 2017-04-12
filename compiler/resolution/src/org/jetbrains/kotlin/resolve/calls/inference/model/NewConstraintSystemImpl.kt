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

package org.jetbrains.kotlin.resolve.calls.inference.model

import org.jetbrains.kotlin.resolve.calls.components.KotlinCallCompleter
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.buildCurrentSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.*
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallDiagnostic
import org.jetbrains.kotlin.resolve.calls.model.ResolvedKotlinCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedLambdaArgument
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.typeUtil.contains
import java.util.*

class NewConstraintSystemImpl(val constraintInjector: ConstraintInjector, val resultTypeResolver: ResultTypeResolver):
        NewConstraintSystem,
        ConstraintSystemBuilder,
        ConstraintInjector.Context,
        ResultTypeResolver.Context,
        KotlinCallCompleter.Context,
        FixationOrderCalculator.Context
{
    val storage = MutableConstraintStorage()
    private var state = State.BUILDING

    private enum class State {
        BUILDING,
        FREEZED,
        COMPLETION
    }

    private fun checkState(vararg allowedState: State) {
        assert(state in allowedState) {
            "State $state is not allowed. AllowedStates: ${allowedState.joinToString()}"
        }
    }

    override val diagnostics: List<KotlinCallDiagnostic>
        get() = storage.errors

    override fun getBuilder() = apply { checkState(State.BUILDING, State.COMPLETION) }

    override fun asConstraintInjectorContext() = apply { checkState(State.BUILDING, State.COMPLETION) }

    override fun asReadOnlyStorage(): ConstraintStorage {
        checkState(State.BUILDING, State.FREEZED)
        state = State.FREEZED
        return storage
    }

    override fun asCallCompleterContext(): KotlinCallCompleter.Context {
        checkState(State.BUILDING, State.COMPLETION)
        state = State.COMPLETION
        return this
    }

    // ConstraintSystemBuilder
    override fun registerVariable(variable: NewTypeVariable) {
        checkState(State.BUILDING, State.COMPLETION)

        storage.allTypeVariables[variable.freshTypeConstructor] = variable
        storage.notFixedTypeVariables[variable.freshTypeConstructor] = MutableVariableWithConstraints(variable)
    }

    override fun addSubtypeConstraint(lowerType: UnwrappedType, upperType: UnwrappedType, position: ConstraintPosition) =
            constraintInjector.addInitialSubtypeConstraint(apply { checkState(State.BUILDING, State.COMPLETION) }, lowerType, upperType, position)

    override fun addEqualityConstraint(a: UnwrappedType, b: UnwrappedType, position: ConstraintPosition) =
            constraintInjector.addInitialEqualityConstraint(apply { checkState(State.BUILDING, State.COMPLETION) }, a, b, position)

    override fun addLambdaArgument(resolvedLambdaArgument: ResolvedLambdaArgument) {
        checkState(State.BUILDING, State.COMPLETION)
        storage.lambdaArguments.add(resolvedLambdaArgument)
    }

    override fun addSubtypeConstraintIfCompatible(lowerType: UnwrappedType, upperType: UnwrappedType, position: ConstraintPosition): Boolean {
        checkState(State.BUILDING, State.COMPLETION)

        if (hasContradiction) return false
        addSubtypeConstraint(lowerType, upperType, position)
        if (!hasContradiction) return true

        val shouldRemove = { c: Constraint -> c.position === position ||
                                              (c.position is IncorporationConstraintPosition && c.position.from === position) }

        for (variableWithConstraint in storage.notFixedTypeVariables.values) {
            variableWithConstraint.removeLastConstraints(shouldRemove)
        }
        storage.errors.clear()
        storage.initialConstraints.removeAt(storage.initialConstraints.lastIndex)

        return false
    }

    private fun getVariablesForFixation(): Map<NewTypeVariable, UnwrappedType> {
        val fixedVariables = LinkedHashMap<NewTypeVariable, UnwrappedType>()

        for (variableWithConstrains in storage.notFixedTypeVariables.values) {
            val resultType = resultTypeResolver.findResultIfThereIsEqualsConstraint(apply { checkState(State.BUILDING) }, variableWithConstrains,
                                                                                                allowedFixToNotProperType = false)
            if (resultType != null) {
                fixedVariables[variableWithConstrains.typeVariable] = resultType
            }
        }
        return fixedVariables
    }

    override fun simplify(): NewTypeSubstitutor {
        checkState(State.BUILDING)

        var fixedVariables = getVariablesForFixation()
        while (fixedVariables.isNotEmpty()) {
            for ((variable, resultType) in fixedVariables) {
                fixVariable(variable, resultType)
            }
            fixedVariables = getVariablesForFixation()
        }

        return storage.buildCurrentSubstitutor()
    }

    // ConstraintSystemBuilder, KotlinCallCompleter.Context
    override val hasContradiction: Boolean
        get() = diagnostics.any { !it.candidateApplicability.isSuccess }.apply { checkState(State.BUILDING, State.COMPLETION) }

    override fun addInnerCall(innerCall: ResolvedKotlinCall.OnlyResolvedKotlinCall) {
        checkState(State.BUILDING, State.COMPLETION)
        storage.innerCalls.add(innerCall)

        val otherSystem = innerCall.candidate.lastCall.constraintSystem.asReadOnlyStorage()
        storage.allTypeVariables.putAll(otherSystem.allTypeVariables)
        for ((variable, constraints) in otherSystem.notFixedTypeVariables) {
            notFixedTypeVariables[variable] = MutableVariableWithConstraints(constraints.typeVariable, constraints.constraints)
        }
        storage.initialConstraints.addAll(otherSystem.initialConstraints)
        storage.maxTypeDepthFromInitialConstraints = Math.max(storage.maxTypeDepthFromInitialConstraints, otherSystem.maxTypeDepthFromInitialConstraints)
        storage.errors.addAll(otherSystem.errors)
        storage.fixedTypeVariables.putAll(otherSystem.fixedTypeVariables)
        storage.lambdaArguments.addAll(otherSystem.lambdaArguments)
        storage.innerCalls.addAll(otherSystem.innerCalls)
    }


    // ResultTypeResolver.Context, ConstraintSystemBuilder
    override fun isProperType(type: UnwrappedType): Boolean {
        checkState(State.BUILDING, State.COMPLETION)
        return !type.contains {
            storage.allTypeVariables.containsKey(it.constructor)
        }
    }

    // ConstraintInjector.Context
    override val allTypeVariables: Map<TypeConstructor, NewTypeVariable> get() {
        checkState(State.BUILDING, State.COMPLETION)
        return storage.allTypeVariables
    }

    override var maxTypeDepthFromInitialConstraints: Int
        get() = storage.maxTypeDepthFromInitialConstraints
        set(value) {
            checkState(State.BUILDING, State.COMPLETION)
            storage.maxTypeDepthFromInitialConstraints = value
        }

    override fun addInitialConstraint(initialConstraint: InitialConstraint) {
        checkState(State.BUILDING, State.COMPLETION)
        storage.initialConstraints.add(initialConstraint)
    }

    // ConstraintInjector.Context, FixationOrderCalculator.Context
    override val notFixedTypeVariables: MutableMap<TypeConstructor, MutableVariableWithConstraints> get() {
        checkState(State.BUILDING, State.COMPLETION)
        return storage.notFixedTypeVariables
    }

    // ConstraintInjector.Context, KotlinCallCompleter.Context
    override fun addError(error: KotlinCallDiagnostic) {
        checkState(State.BUILDING, State.COMPLETION)
        storage.errors.add(error)
    }

    // FixationOrderCalculator.Context, KotlinCallCompleter.Context
    override val lambdaArguments: List<ResolvedLambdaArgument> get() {
        checkState(State.COMPLETION)
        return storage.lambdaArguments
    }

    // KotlinCallCompleter.Context
    override fun asResultTypeResolverContext() = apply { checkState(State.COMPLETION) }

    override fun asFixationOrderCalculatorContext() = apply { checkState(State.COMPLETION) }

    override fun fixVariable(variable: NewTypeVariable, resultType: UnwrappedType) {
        checkState(State.BUILDING, State.COMPLETION)

        constraintInjector.addInitialEqualityConstraint(this, variable.defaultType, resultType, FixVariableConstraintPosition(variable))
        notFixedTypeVariables.remove(variable.freshTypeConstructor)

        for (variableWithConstraint in notFixedTypeVariables.values) {
            variableWithConstraint.removeConstrains {
                it.type.contains { it.constructor == variable.freshTypeConstructor }
            }
        }

        storage.fixedTypeVariables[variable.freshTypeConstructor] = resultType
    }

    override val innerCalls: List<ResolvedKotlinCall.OnlyResolvedKotlinCall> get() {
        checkState(State.COMPLETION)
        return storage.innerCalls
    }

    override fun canBeProper(type: UnwrappedType): Boolean {
        checkState(State.COMPLETION)
        return !type.contains { storage.notFixedTypeVariables.containsKey(it.constructor) }
    }

    override fun buildCurrentSubstitutor(): NewTypeSubstitutor {
        checkState(State.COMPLETION)
        return storage.buildCurrentSubstitutor()
    }

    override fun buildResultingSubstitutor(): NewTypeSubstitutor {
        checkState(State.COMPLETION)
        val currentSubstitutorMap = storage.fixedTypeVariables.entries.associate {
            it.key to it.value
        }
        val uninferredSubstitutorMap = storage.notFixedTypeVariables.entries.associate { (freshTypeConstructor, typeVariable) ->
            freshTypeConstructor to ErrorUtils.createErrorTypeWithCustomConstructor("Uninferred type", typeVariable.typeVariable.freshTypeConstructor)
        }

        return NewTypeSubstitutorByConstructorMap(currentSubstitutorMap + uninferredSubstitutorMap)
    }
}