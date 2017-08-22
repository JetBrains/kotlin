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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzer
import org.jetbrains.kotlin.resolve.calls.inference.*
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintInjector
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.ResultTypeResolver
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallDiagnostic
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.IntersectionTypeConstructor
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class NewConstraintSystemImpl(
        private val constraintInjector: ConstraintInjector,
        override val builtIns: KotlinBuiltIns
):
        NewConstraintSystem,
        ConstraintSystemBuilder,
        ConstraintInjector.Context,
        ResultTypeResolver.Context,
        KotlinConstraintSystemCompleter.Context,
        PostponedArgumentsAnalyzer.Context
{
    companion object {
        private val TYPE_PARAMETER_FOR_EXCLXCL = "<TYPE-PARAMETER-FOR-EXCLEXCL-RESOLVE>" // TODO: Get this variable from ResolveConstruct
    }

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

    // ConstraintSystemBuilder, KotlinConstraintSystemCompleter.Context
    override val hasContradiction: Boolean
        get() = diagnostics.any { !it.candidateApplicability.isSuccess }.apply { checkState(State.FREEZED, State.BUILDING, State.COMPLETION, State.TRANSACTION) }

    override fun addOtherSystem(otherSystem: ConstraintStorage) {
        storage.allTypeVariables.putAll(otherSystem.allTypeVariables)
        for ((variable, constraints) in otherSystem.notFixedTypeVariables) {
            notFixedTypeVariables[variable] = MutableVariableWithConstraints(constraints.typeVariable, constraints.constraints)
        }
        storage.initialConstraints.addAll(otherSystem.initialConstraints)
        storage.maxTypeDepthFromInitialConstraints = Math.max(storage.maxTypeDepthFromInitialConstraints, otherSystem.maxTypeDepthFromInitialConstraints)
        storage.errors.addAll(otherSystem.errors)
        storage.fixedTypeVariables.putAll(otherSystem.fixedTypeVariables)
    }

    // ResultTypeResolver.Context, ConstraintSystemBuilder
    override fun isProperType(type: UnwrappedType): Boolean {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return !type.contains {
            storage.allTypeVariables.containsKey(it.constructor)
        }
    }

    override fun isTypeVariable(type: UnwrappedType): Boolean {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return notFixedTypeVariables.containsKey(type.constructor)
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

    // ConstraintInjector.Context, KotlinConstraintSystemCompleter.Context
    override fun addError(error: KotlinCallDiagnostic) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        storage.errors.add(error)
    }

    // KotlinConstraintSystemCompleter.Context
    override fun fixVariable(variable: NewTypeVariable, resultType: UnwrappedType) {
        checkState(State.BUILDING, State.COMPLETION)

        val actualResultType = eliminateSpecialIntersectionType(variable, resultType) ?: resultType
        constraintInjector.addInitialEqualityConstraint(this, variable.defaultType, actualResultType, FixVariableConstraintPosition(variable))
        notFixedTypeVariables.remove(variable.freshTypeConstructor)

        for (variableWithConstraint in notFixedTypeVariables.values) {
            variableWithConstraint.removeConstrains {
                it.type.contains { it.constructor == variable.freshTypeConstructor }
            }
        }

        storage.fixedTypeVariables[variable.freshTypeConstructor] = actualResultType
    }

    private fun eliminateSpecialIntersectionType(variable: NewTypeVariable, type: UnwrappedType): UnwrappedType? {
        if (variable.shouldBeDefinitelyNotNull()) return null

        val constructor = type.constructor.safeAs<IntersectionTypeConstructor>() ?: return null

        if (constructor.supertypes.size != 2) return null
        val actualType = constructor.supertypes.singleOrNull { it != type.builtIns.anyType }

        return actualType?.unwrap()
    }

    private fun NewTypeVariable.shouldBeDefinitelyNotNull(): Boolean {
        return when (this) {
            is TypeVariableFromCallableDescriptor -> originalTypeParameter.name.asString() == TYPE_PARAMETER_FOR_EXCLXCL
            is TypeVariableForLambdaReturnType -> false
        }
    }

    // KotlinConstraintSystemCompleter.Context, PostponedArgumentsAnalyzer.Context
    override fun canBeProper(type: UnwrappedType): Boolean {
        checkState(State.BUILDING, State.COMPLETION)
        return !type.contains { storage.notFixedTypeVariables.containsKey(it.constructor) }
    }

    // PostponedArgumentsAnalyzer.Context
    override fun buildCurrentSubstitutor(): NewTypeSubstitutor {
        checkState(State.BUILDING, State.COMPLETION)
        return storage.buildCurrentSubstitutor()
    }
}