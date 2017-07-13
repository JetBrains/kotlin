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

package org.jetbrains.kotlin.resolve.calls.inference.model

import org.jetbrains.kotlin.resolve.calls.components.KotlinCallCompleter
import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzer
import org.jetbrains.kotlin.resolve.calls.inference.*
import org.jetbrains.kotlin.resolve.calls.inference.components.*
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallDiagnostic
import org.jetbrains.kotlin.resolve.calls.model.PostponedKotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.model.PostponedLambdaArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedKotlinCall
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.utils.SmartList
import java.util.*

class NewConstraintSystemImpl(val constraintInjector: ConstraintInjector, val resultTypeResolver: ResultTypeResolver):
        NewConstraintSystem,
        ConstraintSystemBuilder,
        ConstraintInjector.Context,
        ResultTypeResolver.Context,
        KotlinCallCompleter.Context,
        ConstraintSystemCompleter.Context,
        PostponedArgumentsAnalyzer.Context
{
    private val storage = MutableConstraintStorage()
    private var state = State.BUILDING
    private val typeVariablesTransaction: MutableList<NewTypeVariable> = SmartList()

    private enum class State {
        BUILDING,
        TRANSACTION,
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

    override fun asConstraintSystemCompleterContext() = apply { checkState(State.BUILDING) }

    override fun asPostponedArgumentsAnalyzerContext() = apply { checkState(State.BUILDING) }

    // ConstraintSystemOperation
    override fun registerVariable(variable: NewTypeVariable) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)

        transactionRegisterVariable(variable)
        storage.allTypeVariables[variable.freshTypeConstructor] = variable
        storage.notFixedTypeVariables[variable.freshTypeConstructor] = MutableVariableWithConstraints(variable)
    }

    override fun addSubtypeConstraint(lowerType: UnwrappedType, upperType: UnwrappedType, position: ConstraintPosition) =
            constraintInjector.addInitialSubtypeConstraint(apply { checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION) }, lowerType, upperType, position)

    override fun addEqualityConstraint(a: UnwrappedType, b: UnwrappedType, position: ConstraintPosition) =
            constraintInjector.addInitialEqualityConstraint(apply { checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION) }, a, b, position)

    override fun getProperSuperTypeConstructors(type: UnwrappedType): List<TypeConstructor> {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        val variableWithConstraints = notFixedTypeVariables[type.constructor] ?: return listOf(type.constructor)

        return variableWithConstraints.constraints.mapNotNull {
            if (it.kind == ConstraintKind.LOWER) return@mapNotNull null
            it.type.constructor.takeUnless { allTypeVariables.containsKey(it) }
        }
    }

    // ConstraintSystemBuilder
    private fun transactionRegisterVariable(variable: NewTypeVariable) {
        if (state != State.TRANSACTION) return
        typeVariablesTransaction.add(variable)
    }

    private fun closeTransaction(beforeState: State) {
        checkState(State.TRANSACTION)
        typeVariablesTransaction.clear()
        state = beforeState
    }

    override fun runTransaction(runOperations: ConstraintSystemOperation.() -> Boolean): Boolean {
        checkState(State.BUILDING, State.COMPLETION)
        val beforeState = state
        val beforeInitialConstraintCount = storage.initialConstraints.size
        val beforeErrorsCount = storage.errors.size
        val beforeMaxTypeDepthFromInitialConstraints = storage.maxTypeDepthFromInitialConstraints

        state = State.TRANSACTION
        // typeVariablesTransaction is clear
        if (runOperations()) {
            closeTransaction(beforeState)
            return true
        }

        for (addedTypeVariable in typeVariablesTransaction) {
            storage.allTypeVariables.remove(addedTypeVariable.freshTypeConstructor)
            storage.notFixedTypeVariables.remove(addedTypeVariable.freshTypeConstructor)
        }
        storage.maxTypeDepthFromInitialConstraints = beforeMaxTypeDepthFromInitialConstraints
        storage.errors.trimToSize(beforeErrorsCount)

        val addedInitialConstraints = storage.initialConstraints.subList(beforeInitialConstraintCount, storage.initialConstraints.size)

        val shouldRemove = { c: Constraint -> addedInitialConstraints.contains(c.position.initialConstraint) }

        for (variableWithConstraint in storage.notFixedTypeVariables.values) {
            variableWithConstraint.removeLastConstraints(shouldRemove)
        }

        addedInitialConstraints.clear() // remove constraint from storage.initialConstraints
        closeTransaction(beforeState)
        return false
    }

    override fun addPostponedArgument(postponedArgument: PostponedKotlinCallArgument) {
        checkState(State.BUILDING, State.COMPLETION)
        storage.postponedArguments.add(postponedArgument)
    }

    private fun getVariablesForFixation(): Map<NewTypeVariable, UnwrappedType> {
        val fixedVariables = LinkedHashMap<NewTypeVariable, UnwrappedType>()

        for (variableWithConstrains in storage.notFixedTypeVariables.values) {
            val resultType = resultTypeResolver.findResultIfThereIsEqualsConstraint(
                    apply { checkState(State.BUILDING) },
                    variableWithConstrains,
                    allowedFixToNotProperType = false
            )
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

    // ConstraintSystemBuilder, ConstraintSystemCompleter.Context
    override val hasContradiction: Boolean
        get() = diagnostics.any { !it.candidateApplicability.isSuccess }.apply { checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION) }

    // ConstraintSystemBuilder
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
        storage.postponedArguments.addAll(otherSystem.postponedArguments)
        storage.innerCalls.addAll(otherSystem.innerCalls)
    }


    // ResultTypeResolver.Context, ConstraintSystemBuilder
    override fun isProperType(type: UnwrappedType): Boolean {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return !type.contains {
            storage.allTypeVariables.containsKey(it.constructor)
        }
    }

    // ConstraintInjector.Context
    override val allTypeVariables: Map<TypeConstructor, NewTypeVariable> get() {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return storage.allTypeVariables
    }

    override var maxTypeDepthFromInitialConstraints: Int
        get() = storage.maxTypeDepthFromInitialConstraints
        set(value) {
            checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
            storage.maxTypeDepthFromInitialConstraints = value
        }

    override fun addInitialConstraint(initialConstraint: InitialConstraint) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        storage.initialConstraints.add(initialConstraint)
    }

    // ConstraintInjector.Context, FixationOrderCalculator.Context
    override val notFixedTypeVariables: MutableMap<TypeConstructor, MutableVariableWithConstraints> get() {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return storage.notFixedTypeVariables
    }

    // ConstraintInjector.Context, ConstraintSystemCompleter.Context
    override fun addError(error: KotlinCallDiagnostic) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        storage.errors.add(error)
    }

    //  KotlinCallCompleter.Context, FixationOrderCalculator.Context
    override val lambdaArguments: List<PostponedLambdaArgument> get() {
        checkState(State.BUILDING, State.COMPLETION)
        return storage.postponedArguments.filterIsInstance<PostponedLambdaArgument>()
    }

    // FixationOrderCalculator.Context, KotlinCallCompleter.Context, ConstraintSystemCompleter.Context
    override val postponedArguments: List<PostponedKotlinCallArgument>
        get() = storage.postponedArguments.apply { checkState(State.BUILDING, State.COMPLETION) }

    // ConstraintSystemCompleter.Context
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

    // KotlinCallCompleter.Context
    override val innerCalls: List<ResolvedKotlinCall.OnlyResolvedKotlinCall> get() {
        checkState(State.COMPLETION)
        return storage.innerCalls
    }

    // ConstraintSystemCompleter.Context, PostponedArgumentsAnalyzer.Context
    override fun canBeProper(type: UnwrappedType): Boolean {
        checkState(State.BUILDING, State.COMPLETION)
        return !type.contains { storage.notFixedTypeVariables.containsKey(it.constructor) }
    }

    // PostponedArgumentsAnalyzer.Context
    override fun buildCurrentSubstitutor(): NewTypeSubstitutor {
        checkState(State.BUILDING, State.COMPLETION)
        return storage.buildCurrentSubstitutor()
    }

    // KotlinCallCompleter.Context
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