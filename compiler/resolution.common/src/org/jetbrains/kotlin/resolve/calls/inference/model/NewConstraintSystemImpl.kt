/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.model

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzerContext
import org.jetbrains.kotlin.resolve.calls.inference.*
import org.jetbrains.kotlin.resolve.calls.inference.components.*
import org.jetbrains.kotlin.resolve.checkers.EmptyIntersectionTypeInfo
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addToStdlib.trimToSize
import kotlin.math.max

class NewConstraintSystemImpl(
    private val constraintInjector: ConstraintInjector,
    val typeSystemContext: TypeSystemInferenceExtensionContext,
    private val languageVersionSettings: LanguageVersionSettings,
) : ConstraintSystemCompletionContext(),
    TypeSystemInferenceExtensionContext by typeSystemContext,
    NewConstraintSystem,
    ConstraintSystemBuilder,
    ConstraintInjector.Context,
    ResultTypeResolver.Context,
    PostponedArgumentsAnalyzerContext {
    private val utilContext = constraintInjector.constraintIncorporator.utilContext
    private val constraintsLogger = constraintInjector.constraintsLogger

    private val postponedComputationsAfterAllVariablesAreFixed = mutableListOf<() -> Unit>()

    private val storage = MutableConstraintStorage()
    private var state = State.BUILDING
    private val typeVariablesTransaction: MutableList<TypeVariableMarker> = SmartList()
    private val properTypesCache: MutableSet<KotlinTypeMarker> = SmartSet.create()
    private val notProperTypesCache: MutableSet<KotlinTypeMarker> = SmartSet.create()
    private val intersectionTypesCache: MutableMap<Collection<KotlinTypeMarker>, EmptyIntersectionTypeInfo?> = mutableMapOf()

    // Cached value that should be reset on each new constraint or fork point
    private var hasContradictionInForkPointsCache: Boolean? = null

    override var typeVariablesThatAreCountedAsProperTypes: Set<TypeConstructorMarker>? = null

    private var couldBeResolvedWithUnrestrictedBuilderInference: Boolean = false

    override var atCompletionState: Boolean = false

    override var allowSemiFixationToOtherTypeVariables: Boolean = false

    /**
     * @see [org.jetbrains.kotlin.resolve.calls.inference.components.VariableFixationFinder.Context.typeVariablesThatAreNotCountedAsProperTypes]
     * @see [org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer.fixInnerVariablesForProvideDelegateIfNeeded]
     */
    @K2Only
    override fun <R> withTypeVariablesThatAreCountedAsProperTypes(
        typeVariables: Set<TypeConstructorMarker>,
        allowSemiFixationToOtherTypeVariables: Boolean,
        block: () -> R,
    ): R {
        checkState(State.BUILDING)
        // Cleaning cache is necessary because temporarily we change the meaning of what does "proper type" mean
        properTypesCache.clear()
        notProperTypesCache.clear()
        val previousAllowSemiFixationToOtherTypeVariables = this.allowSemiFixationToOtherTypeVariables

        require(typeVariablesThatAreCountedAsProperTypes == null) {
            "Currently there should be no nested withDisallowingOnlyThisTypeVariablesForProperTypes calls"
        }

        typeVariablesThatAreCountedAsProperTypes = typeVariables
        this.allowSemiFixationToOtherTypeVariables = allowSemiFixationToOtherTypeVariables

        val result = block()

        this.allowSemiFixationToOtherTypeVariables = previousAllowSemiFixationToOtherTypeVariables
        typeVariablesThatAreCountedAsProperTypes = null
        properTypesCache.clear()
        notProperTypesCache.clear()

        return result
    }

    private enum class State {
        BUILDING,
        TRANSACTION,
        FREEZED,
        COMPLETION
    }

    /*
     * If remove spread operator then call `checkState` will resolve to itself
     *   instead of fun checkState(vararg allowedState: State)
     */
    private fun checkState(a: State) {
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        checkState(*arrayOf(a))
    }

    private fun checkState(a: State, b: State) {
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        checkState(*arrayOf(a, b))
    }

    private fun checkState(a: State, b: State, c: State) {
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        checkState(*arrayOf(a, b, c))
    }

    private fun checkState(a: State, b: State, c: State, d: State) {
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        checkState(*arrayOf(a, b, c, d))
    }

    private fun checkState(vararg allowedState: State) {
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        assert(state in allowedState) {
            "State $state is not allowed. AllowedStates: ${allowedState.joinToString()}"
        }
    }

    override val errors: List<ConstraintSystemError>
        get() = storage.errors

    override fun getBuilder() = apply { checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION) }

    override fun asReadOnlyStorage(): ConstraintStorage {
        checkState(State.BUILDING, State.FREEZED)

        if (languageVersionSettings.supportsFeature(LanguageFeature.ConsiderForkPointsWhenCheckingContradictions) && areThereContradictionsInForks()) {
            // If there are contradictions already, we might apply all the forks because CS is anyway already failed
            resolveForkPointsConstraints()
        }

        state = State.FREEZED
        return storage
    }

    override fun asConstraintSystemCompleterContext() = apply {
        checkState(State.BUILDING)

        this.atCompletionState = true
    }

    override fun asPostponedArgumentsAnalyzerContext() = apply { checkState(State.BUILDING) }

    // ConstraintSystemOperation
    override fun registerVariable(variable: TypeVariableMarker) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)

        transactionRegisterVariable(variable)
        storage.allTypeVariables.put(variable.freshTypeConstructor(), variable)
            ?.let { error("Type variable already registered: old: $it, new: $variable") }
        notProperTypesCache.clear()
        storage.notFixedTypeVariables[variable.freshTypeConstructor()] = MutableVariableWithConstraints(this, variable, constraintsLogger)
        constraintsLogger?.logNewVariable(variable, this)
    }

    override fun markPostponedVariable(variable: TypeVariableMarker) {
        storage.postponedTypeVariables += variable
    }

    override fun markCouldBeResolvedWithUnrestrictedBuilderInference() {
        couldBeResolvedWithUnrestrictedBuilderInference = true
    }

    override fun couldBeResolvedWithUnrestrictedBuilderInference() =
        couldBeResolvedWithUnrestrictedBuilderInference

    override fun unmarkPostponedVariable(variable: TypeVariableMarker) {
        storage.postponedTypeVariables -= variable
    }

    override fun removePostponedVariables() {
        storage.postponedTypeVariables.clear()
    }

    override fun substituteFixedVariables(substitutor: TypeSubstitutorMarker) {
        storage.fixedTypeVariables.replaceAll { _, type -> substitutor.safeSubstitute(type) }
    }

    override fun putBuiltFunctionalExpectedTypeForPostponedArgument(
        topLevelVariable: TypeConstructorMarker,
        pathToExpectedType: List<Pair<TypeConstructorMarker, Int>>,
        builtFunctionalType: KotlinTypeMarker,
    ) {
        storage.builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables[topLevelVariable to pathToExpectedType] =
            builtFunctionalType
    }

    override fun putBuiltFunctionalExpectedTypeForPostponedArgument(
        expectedTypeVariable: TypeConstructorMarker,
        builtFunctionalType: KotlinTypeMarker,
    ) {
        storage.builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables[expectedTypeVariable] = builtFunctionalType
    }

    override fun getBuiltFunctionalExpectedTypeForPostponedArgument(
        topLevelVariable: TypeConstructorMarker,
        pathToExpectedType: List<Pair<TypeConstructorMarker, Int>>,
    ) = storage.builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables[topLevelVariable to pathToExpectedType]

    override fun getBuiltFunctionalExpectedTypeForPostponedArgument(expectedTypeVariable: TypeConstructorMarker) =
        storage.builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables[expectedTypeVariable]

    override fun addSubtypeConstraint(lowerType: KotlinTypeMarker, upperType: KotlinTypeMarker, position: ConstraintPosition) =
        constraintInjector.addInitialSubtypeConstraint(
            apply { checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION) },
            lowerType,
            upperType,
            position
        )

    override fun addEqualityConstraint(a: KotlinTypeMarker, b: KotlinTypeMarker, position: ConstraintPosition) =
        constraintInjector.addInitialEqualityConstraint(
            apply { checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION) },
            a,
            b,
            position
        )

    override fun getProperSuperTypeConstructors(type: KotlinTypeMarker): List<TypeConstructorMarker> {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        val variableWithConstraints = notFixedTypeVariables[type.typeConstructor()] ?: return listOf(type.typeConstructor())

        return variableWithConstraints.constraints.mapNotNull {
            if (it.kind == ConstraintKind.LOWER) return@mapNotNull null
            it.type.typeConstructor().takeUnless { allTypeVariables.containsKey(it) }
        }
    }

    // ConstraintSystemBuilder
    private fun transactionRegisterVariable(variable: TypeVariableMarker) {
        if (state != State.TRANSACTION) return
        if (variable.freshTypeConstructor() in storage.allTypeVariables) return
        typeVariablesTransaction.add(variable)
    }

    private fun closeTransaction(beforeState: State, beforeTypeVariables: Int) {
        checkState(State.TRANSACTION)
        typeVariablesTransaction.trimToSize(beforeTypeVariables)
        state = beforeState
    }

    private inner class TransactionState(
        private val beforeState: State,
        private val beforeInitialConstraintCount: Int,
        private val beforeErrorsCount: Int,
        private val beforeMaxTypeDepthFromInitialConstraints: Int,
        private val beforeTypeVariablesTransactionSize: Int,
        private val beforeMissedConstraintsCount: Int,
        private val beforeConstraintCountByVariables: Map<TypeConstructorMarker, Int>,
        private val beforeConstraintsFromAllForks: Int,
    ) : ConstraintSystemTransaction() {
        override fun closeTransaction() {
            checkState(State.TRANSACTION)
            typeVariablesTransaction.trimToSize(beforeTypeVariablesTransactionSize)
            state = beforeState
        }

        override fun rollbackTransaction() {
            for (addedTypeVariable in typeVariablesTransaction.subList(beforeTypeVariablesTransactionSize, typeVariablesTransaction.size)) {
                storage.allTypeVariables.remove(addedTypeVariable.freshTypeConstructor())
                storage.notFixedTypeVariables.remove(addedTypeVariable.freshTypeConstructor())
            }
            storage.maxTypeDepthFromInitialConstraints = beforeMaxTypeDepthFromInitialConstraints
            storage.errors.trimToSize(beforeErrorsCount)
            storage.missedConstraints.trimToSize(beforeMissedConstraintsCount)
            storage.constraintsFromAllForkPoints.trimToSize(beforeConstraintsFromAllForks)

            val addedInitialConstraints = storage.initialConstraints.subList(
                beforeInitialConstraintCount,
                storage.initialConstraints.size
            )

            for (variableWithConstraint in storage.notFixedTypeVariables.values) {
                val sinceIndexToRemoveConstraints =
                    beforeConstraintCountByVariables[variableWithConstraint.typeVariable.freshTypeConstructor()]
                if (sinceIndexToRemoveConstraints != null) {
                    variableWithConstraint.removeLastConstraints(sinceIndexToRemoveConstraints)
                }
            }

            addedInitialConstraints.clear() // remove constraint from storage.initialConstraints
            closeTransaction(beforeState, beforeTypeVariablesTransactionSize)
        }
    }

    override fun prepareTransaction(): ConstraintSystemTransaction {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return TransactionState(
            beforeState = state,
            beforeInitialConstraintCount = storage.initialConstraints.size,
            beforeErrorsCount = storage.errors.size,
            beforeMaxTypeDepthFromInitialConstraints = storage.maxTypeDepthFromInitialConstraints,
            beforeTypeVariablesTransactionSize = typeVariablesTransaction.size,
            beforeMissedConstraintsCount = storage.missedConstraints.size,
            beforeConstraintCountByVariables = storage.notFixedTypeVariables.mapValues { it.value.rawConstraintsCount },
            beforeConstraintsFromAllForks = storage.constraintsFromAllForkPoints.size,
        ).also {
            state = State.TRANSACTION
        }
    }

    // ConstraintSystemBuilder, KotlinConstraintSystemCompleter.Context
    override val hasContradiction: Boolean
        get() {
            checkState(
                State.FREEZED,
                State.BUILDING,
                State.COMPLETION,
                State.TRANSACTION
            )

            if (storage.hasContradiction) return true

            if (!languageVersionSettings.supportsFeature(LanguageFeature.ConsiderForkPointsWhenCheckingContradictions)) return false

            // Since 2.2 at each hasContradiction check, we make sure that all forks might be successfully resolved, too
            return areThereContradictionsInForks()
        }

    fun addOuterSystem(outerSystem: ConstraintStorage) {
        require(!storage.usesOuterCs)

        storage.usesOuterCs = true
        storage.outerSystemVariablesPrefixSize = outerSystem.allTypeVariables.size
        @OptIn(AssertionsOnly::class)
        storage.outerCS = outerSystem

        @OptIn(AssertionsOnly::class)
        runOuterCSRelatedAssertions(outerSystem, isAddingOuter = true)

        doAddOtherSystem(outerSystem, mergeMode = false)
    }

    @K2Only
    fun setBaseSystem(baseSystem: ConstraintStorage) {
        require(storage.allTypeVariables.isEmpty())
        storage.usesOuterCs = baseSystem.usesOuterCs
        storage.outerSystemVariablesPrefixSize = baseSystem.outerSystemVariablesPrefixSize
        @OptIn(AssertionsOnly::class)
        storage.outerCS = (baseSystem as? MutableConstraintStorage)?.outerCS

        addOtherSystem(baseSystem)
    }

    fun prepareForGlobalCompletion() {
        // There's no more separation of outer/inner variables once global completion starts
        storage.outerSystemVariablesPrefixSize = 0
    }

    override fun addOtherSystem(otherSystem: ConstraintStorage) {
        @OptIn(AssertionsOnly::class)
        runOuterCSRelatedAssertions(otherSystem, isAddingOuter = false)

        doAddOtherSystem(otherSystem, mergeMode = false)
    }

    @K2Only
    @UnstableSystemMergeMode
    override fun mergeOtherSystem(otherSystem: ConstraintStorage) {
        @OptIn(AssertionsOnly::class)
        runOuterCSRelatedAssertions(otherSystem, isAddingOuter = false)

        doAddOtherSystem(otherSystem, mergeMode = true)
    }

    /**
     * This function is only expected to be called when [otherSystem] is a superset of this CS,
     * or in other words _this_ CS has used as a base/outer CS of the [otherSystem].
     *
     * Or one might say that [otherSystem] is expected to be a clone of the current CS with some additions: new variables, constraints, etc.
     */
    fun replaceContentWith(otherSystem: ConstraintStorage) {
        @OptIn(AssertionsOnly::class)
        runOuterCSRelatedAssertions(otherSystem, isAddingOuter = false)

        // Clean all existing data
        // NB: `postponedTypeVariables` is always empty in K2/PCLA, thus no need to clear it
        notFixedTypeVariables.clear()
        typeVariableDependencies.clear()
        storage.initialConstraints.clear()
        storage.errors.clear()
        storage.constraintsFromAllForkPoints.clear()

        // There's no need to clean `allTypeVariables` as `otherSystem.allTypeVariables` is expected to be a superset of what we've got
        if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
            check(otherSystem.allTypeVariables.keys.containsAll(storage.allTypeVariables.keys))
        }

        doAddOtherSystem(otherSystem, mergeMode = false)
    }

    private fun doAddOtherSystem(otherSystem: ConstraintStorage, mergeMode: Boolean) {
        if (otherSystem.allTypeVariables.isNotEmpty()) {
            otherSystem.allTypeVariables.forEach {
                transactionRegisterVariable(it.value)
            }
            storage.allTypeVariables.putAll(otherSystem.allTypeVariables)
            notProperTypesCache.clear()
        }

        for ((variable, constraints) in otherSystem.notFixedTypeVariables) {
            if (!mergeMode) {
                notFixedTypeVariables[variable] = MutableVariableWithConstraints(this, constraints, constraintsLogger)
            } else {
                val previous = notFixedTypeVariables[variable]
                if (previous != null) {
                    @OptIn(UnstableSystemMergeMode::class)
                    notFixedTypeVariables[variable] = MutableVariableWithConstraints(this, previous, constraints, constraintsLogger)
                } else {
                    notFixedTypeVariables[variable] = MutableVariableWithConstraints(this, constraints, constraintsLogger)
                }
            }
        }

        for ((variable, variablesThatReferenceGivenOne) in otherSystem.typeVariableDependencies) {
            if (!mergeMode || variable !in typeVariableDependencies) {
                typeVariableDependencies[variable] = variablesThatReferenceGivenOne.toMutableSet()
            } else {
                typeVariableDependencies[variable]?.addAll(variablesThatReferenceGivenOne)
            }
        }

        // Merge mode: filtering identical constraints
        if (mergeMode) {
            storage.initialConstraints.addAllDistinct(otherSystem.initialConstraints)
            storage.constraintsFromAllForkPoints.addAllDistinct(otherSystem.constraintsFromAllForkPoints)
            storage.errors.addAllDistinct(otherSystem.errors)
        } else {
            storage.initialConstraints.addAll(otherSystem.initialConstraints)
            storage.constraintsFromAllForkPoints.addAll(otherSystem.constraintsFromAllForkPoints)
            storage.errors.addAll(otherSystem.errors)
        }

        storage.maxTypeDepthFromInitialConstraints =
            max(storage.maxTypeDepthFromInitialConstraints, otherSystem.maxTypeDepthFromInitialConstraints)
        // Keys are compared by identity only.
        // Sometimes we create structurally identical type variables (at least in K2),
        // and they should be considered different.
        storage.fixedTypeVariables.putAll(otherSystem.fixedTypeVariables)
        // K1-only, so merge isn't important here
        storage.postponedTypeVariables.addAll(otherSystem.postponedTypeVariables)

        hasContradictionInForkPointsCache = null
    }

    private fun <T> MutableList<T>.addAllDistinct(other: List<T>) {
        val set = identityHashSetFromSum(this, other)
        clear()
        addAll(set)
    }

    @AssertionsOnly
    private fun runOuterCSRelatedAssertions(otherSystem: ConstraintStorage, isAddingOuter: Boolean) {
        if (!otherSystem.usesOuterCs) return

        // When integrating a child system back, it's ok that for root CS, `storage.usesOuterCs == false`
        if ((otherSystem as? MutableConstraintStorage)?.outerCS === storage) return

        require(storage.usesOuterCs)

        if (!isAddingOuter) {
            require(storage.outerSystemVariablesPrefixSize == otherSystem.outerSystemVariablesPrefixSize) {
                "Expected to be ${otherSystem.outerSystemVariablesPrefixSize}, but ${storage.outerSystemVariablesPrefixSize} found"
            }
        }
    }

    // ResultTypeResolver.Context, ConstraintSystemBuilder
    override fun isProperType(type: KotlinTypeMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        if (storage.allTypeVariables.isEmpty()) return true
        if (notProperTypesCache.contains(type)) return false
        if (properTypesCache.contains(type)) return true
        return isProperTypeImpl(type).also {
            (if (it) properTypesCache else notProperTypesCache).add(type)
        }
    }

    private fun isProperTypeImpl(type: KotlinTypeMarker): Boolean =
        !type.contains {
            val capturedType = it.asRigidType()?.asCapturedTypeUnwrappingDnn()

            val typeToCheck = if (capturedType is CapturedTypeMarker && capturedType.captureStatus() == CaptureStatus.FROM_EXPRESSION)
                capturedType.typeConstructorProjection().getType()
            else
                it

            if (typeToCheck == null) return@contains false
            if (typeVariablesThatAreCountedAsProperTypes?.contains(typeToCheck.typeConstructor()) == true) {
                return@contains false
            }

            return@contains storage.allTypeVariables.containsKey(typeToCheck.typeConstructor())
        }

    override fun isTypeVariable(type: KotlinTypeMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return notFixedTypeVariables.containsKey(type.typeConstructor())
    }

    override fun isPostponedTypeVariable(typeVariable: TypeVariableMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return typeVariable in postponedTypeVariables
    }

    // ConstraintInjector.Context, KotlinConstraintSystemCompleter.Context
    override val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>
        get() {
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
    override val notFixedTypeVariables: MutableMap<TypeConstructorMarker, MutableVariableWithConstraints>
        get() {
            checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
            return storage.notFixedTypeVariables
        }

    /**
     * @see org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage.typeVariableDependencies
     */
    override val typeVariableDependencies: MutableMap<TypeConstructorMarker, MutableSet<TypeConstructorMarker>>
        get() {
            checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
            return storage.typeVariableDependencies
        }

    override val fixedTypeVariables: MutableMap<TypeConstructorMarker, KotlinTypeMarker>
        get() {
            checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
            return storage.fixedTypeVariables
        }

    override val postponedTypeVariables: List<TypeVariableMarker>
        get() {
            checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
            return storage.postponedTypeVariables
        }

    override val outerSystemVariablesPrefixSize: Int
        get() = storage.outerSystemVariablesPrefixSize

    override val constraintsFromAllForkPoints: MutableList<Pair<IncorporationConstraintPosition, ForkPointData>>
        get() {
            checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
            return storage.constraintsFromAllForkPoints
        }

    /**
     * This function tries to find the solution (set of constraints) that is consistent with some branch of each fork.
     * And those constraints are being immediately applied to the system
     */
    override fun resolveForkPointsConstraints() {
        if (constraintsFromAllForkPoints.isEmpty()) return
        val allForkPointsData = constraintsFromAllForkPoints.toList()
        constraintsFromAllForkPoints.clear()

        // There may be multiple fork points:
        // - One from subtyping A<Int> & A<T> <: A<Xv>
        // - Another one from B<String> & B<F> <: B<Yv>
        // Each of them defines two sets of constraints, e.g. for the first for point:
        // 1. {Xv=Int} – is a one-element set (but potentially there might be more constraints in the set)
        // 2. {Xv=T} – second constraints set
        for ((position, forkPointData) in allForkPointsData) {
            applyTheBestBranchFromForkPoint(forkPointData, position)
        }
    }

    override fun onNewConstraintOrForkPoint() {
        hasContradictionInForkPointsCache = null
    }

    /**
     * Checks if the current state of forked constraints is not contradictory.
     *
     * That function is expected to be pure, i.e., it should leave the system in the same state it was found before the call.
     *
     */
    fun areThereContradictionsInForks(): Boolean {
        // Before freezing, we guarantee to apply contradictions to the regular storage if there are any
        // (see NewConstraintSystemImpl.asReadOnlyStorage)
        if (state == State.FREEZED) return false

        if (constraintsFromAllForkPoints.isEmpty()) return false

        hasContradictionInForkPointsCache?.let { return it }

        val allForkPointsData = constraintsFromAllForkPoints.toList()
        constraintsFromAllForkPoints.clear()

        val isThereAnyUnsuccessful: Boolean
        runTransaction {
            isThereAnyUnsuccessful = allForkPointsData.any { (position, forkPointData) ->
                !applyTheBestBranchFromForkPoint(forkPointData, position)
            }

            false
        }

        constraintsFromAllForkPoints.addAll(allForkPointsData)

        return isThereAnyUnsuccessful.also { hasContradictionInForkPointsCache = it }
    }

    /**
     * Applies the first successful branch if there's any.
     * Otherwise, applies just the first branch (containing contradictions)
     *
     * @return true if there is a successful constraint set for the fork point.
     */
    private fun applyTheBestBranchFromForkPoint(
        forkPointData: ForkPointData,
        position: IncorporationConstraintPosition,
    ): Boolean {
        val isSuccessful = forkPointData.any { constraintSetForForkBranch ->
            runTransaction {
                applyForkPointBranch(constraintSetForForkBranch, position)

                !storage.hasContradiction
            }
        }

        if (!isSuccessful) {
            applyForkPointBranch(forkPointData.first(), position)
        }

        return isSuccessful
    }

    private fun applyForkPointBranch(
        constraintSetForForkBranch: ForkPointBranchDescription,
        position: IncorporationConstraintPosition,
    ) {
        constraintInjector.processGivenForkPointBranchConstraints(
            this@NewConstraintSystemImpl.apply { checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION) },
            constraintSetForForkBranch,
            position,
        )

        // Some new fork points constraints might be introduced, and we apply them immediately because we anyway at the
        // completion state (as we already started resolving them)
        resolveForkPointsConstraints()
    }

    // ConstraintInjector.Context, KotlinConstraintSystemCompleter.Context
    override fun addError(error: ConstraintSystemError) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        storage.errors.add(error)
        constraintsLogger?.logError(error, this)
    }

    // KotlinConstraintSystemCompleter.Context
    override fun fixVariable(
        variable: TypeVariableMarker,
        resultType: KotlinTypeMarker,
        position: FixVariableConstraintPosition<*>,
    ) = with(utilContext) {
        checkState(State.BUILDING, State.COMPLETION)

        checkInferredEmptyIntersection(variable, resultType)

        constraintInjector.addInitialEqualityConstraint(this@NewConstraintSystemImpl, variable.defaultType(), resultType, position)

        val freshTypeConstructor = variable.freshTypeConstructor()
        val variableWithConstraints =
            notFixedTypeVariables.remove(freshTypeConstructor) ?: error("Seems that $variable is being fixed second time")

        outerTypeVariables?.let { outerVariables ->
            require(freshTypeConstructor !in outerVariables) {
                "Outer type variables are not assumed to be fixed during nested calls analysis, but $variable is being fixed"
            }
        }

        for (otherVariableWithConstraints in notFixedTypeVariables.values) {
            otherVariableWithConstraints.removeConstraints { containsTypeVariable(it.type, freshTypeConstructor) }
        }

        storage.fixedTypeVariables[freshTypeConstructor] = resultType

        // Substitute freshly fixed type variable into missed constraints
        substituteMissedConstraints()

        postponeOnlyInputTypesCheck(variableWithConstraints, resultType)

        doPostponedComputationsIfAllVariablesAreFixed()
    }

    override fun getEmptyIntersectionTypeKind(types: Collection<KotlinTypeMarker>): EmptyIntersectionTypeInfo? {
        if (types in intersectionTypesCache)
            return intersectionTypesCache.getValue(types)

        return computeEmptyIntersectionTypeKind(types).also {
            intersectionTypesCache[types] = it
        }
    }

    private fun checkInferredEmptyIntersection(variable: TypeVariableMarker, resultType: KotlinTypeMarker) {
        val intersectionTypeConstructor = resultType.typeConstructor().takeIf { it is IntersectionTypeConstructorMarker } ?: return
        val upperTypes = intersectionTypeConstructor.supertypes()

        // Diagnostic with these incompatible types has already been reported at the resolution stage
        if (upperTypes.size <= 1 || storage.errors.any { it is InferredEmptyIntersection && it.incompatibleTypes == upperTypes })
            return

        val emptyIntersectionTypeInfo = getEmptyIntersectionTypeKind(upperTypes) ?: return

        // Remove existing errors from the resolution stage because a completion stage error is always more precise
        storage.errors.removeIf { it is InferredEmptyIntersection }

        val isInferredEmptyIntersectionForbidden =
            languageVersionSettings.supportsFeature(LanguageFeature.ForbidInferringTypeVariablesIntoEmptyIntersection)
        val errorFactory = if (emptyIntersectionTypeInfo.kind.isDefinitelyEmpty && isInferredEmptyIntersectionForbidden)
            ::InferredEmptyIntersectionError
        else ::InferredEmptyIntersectionWarning

        addError(
            errorFactory(upperTypes.toList(), emptyIntersectionTypeInfo.casingTypes.toList(), variable, emptyIntersectionTypeInfo.kind)
        )
    }

    private fun substituteMissedConstraints() {
        val substitutor = buildCurrentSubstitutor()
        for ((_, constraints) in storage.missedConstraints) {
            for ((index, variableWithConstraint) in constraints.withIndex()) {
                val (typeVariable, constraint) = variableWithConstraint
                val newConstraint = constraint.replaceType(substitutor.safeSubstitute(constraint.type))
                constraints[index] = typeVariable to newConstraint
                constraintsLogger?.logConstraintSubstitution(typeVariable, newConstraint, this)
            }
        }
    }

    private fun ConstraintSystemUtilContext.postponeOnlyInputTypesCheck(
        variableWithConstraints: MutableVariableWithConstraints?,
        resultType: KotlinTypeMarker,
    ) {
        if (variableWithConstraints != null && variableWithConstraints.typeVariable.hasOnlyInputTypesAttribute()) {
            postponedComputationsAfterAllVariablesAreFixed.add { checkOnlyInputTypesAnnotation(variableWithConstraints, resultType) }
        }
    }

    private fun doPostponedComputationsIfAllVariablesAreFixed() {
        if (notFixedTypeVariables.isEmpty()) {
            postponedComputationsAfterAllVariablesAreFixed.forEach { it() }
        }
    }

    private fun KotlinTypeMarker.substituteAndApproximateIfNecessary(
        substitutor: TypeSubstitutorMarker,
        approximator: AbstractTypeApproximator,
        constraintKind: ConstraintKind,
    ): KotlinTypeMarker {
        val doesInputTypeContainsOtherVariables = this.contains { it.typeConstructor() is TypeVariableTypeConstructorMarker }
        val substitutedType = if (doesInputTypeContainsOtherVariables) substitutor.safeSubstitute(this) else this
        // Appoximation here is the same as ResultTypeResolver do
        val approximatedType = when (constraintKind) {
            ConstraintKind.LOWER ->
                approximator.approximateToSuperType(substitutedType, TypeApproximatorConfiguration.InternalTypesApproximation)
            ConstraintKind.UPPER ->
                approximator.approximateToSubType(substitutedType, TypeApproximatorConfiguration.InternalTypesApproximation)
            ConstraintKind.EQUALITY -> substitutedType
        } ?: substitutedType

        return approximatedType
    }

    private fun checkOnlyInputTypesAnnotation(variableWithConstraints: MutableVariableWithConstraints, resultType: KotlinTypeMarker) {
        val substitutor = buildCurrentSubstitutor()
        val approximator = constraintInjector.typeApproximator
        val projectedInputCallTypes = variableWithConstraints.getProjectedInputCallTypes(utilContext)
        val isResultTypeEqualSomeInputType = projectedInputCallTypes.any { (inputType, constraintKind) ->
            val inputTypeConstructor = inputType.typeConstructor()
            val otherResultType = inputType.substituteAndApproximateIfNecessary(substitutor, approximator, constraintKind)

            if (AbstractTypeChecker.equalTypes(this, resultType, otherResultType)) return@any true
            if (!inputTypeConstructor.isIntersection()) return@any false

            inputTypeConstructor.supertypes().any {
                val intersectionComponentResultType = it.substituteAndApproximateIfNecessary(substitutor, approximator, constraintKind)
                AbstractTypeChecker.equalTypes(this, resultType, intersectionComponentResultType)
            }
        }
        if (!isResultTypeEqualSomeInputType) {
            addError(OnlyInputTypesDiagnostic(variableWithConstraints.typeVariable))
        }
    }

    // KotlinConstraintSystemCompleter.Context, PostponedArgumentsAnalyzer.Context
    override fun canBeProper(type: KotlinTypeMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION)
        return !type.contains { storage.notFixedTypeVariables.containsKey(it.typeConstructor()) }
    }

    override fun containsOnlyFixedOrPostponedVariables(type: KotlinTypeMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION)
        return !type.contains {
            val typeConstructor = it.typeConstructor()
            val variable = storage.notFixedTypeVariables[typeConstructor]?.typeVariable
            variable !in storage.postponedTypeVariables && storage.notFixedTypeVariables.containsKey(typeConstructor)
        }
    }

    override fun containsOnlyFixedVariables(type: KotlinTypeMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION)
        return !type.contains {
            val typeConstructor = it.typeConstructor()
            storage.notFixedTypeVariables.containsKey(typeConstructor)
        }
    }

    // PostponedArgumentsAnalyzer.Context
    override fun buildCurrentSubstitutor(): TypeSubstitutorMarker {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return buildCurrentSubstitutor(emptyMap())
    }

    override fun buildCurrentSubstitutor(additionalBindings: Map<TypeConstructorMarker, KotlinTypeMarker>): TypeSubstitutorMarker {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return storage.buildCurrentSubstitutor(this, additionalBindings)
    }

    override fun buildNotFixedVariablesToStubTypesSubstitutor(): TypeSubstitutorMarker {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return storage.buildNotFixedVariablesToNonSubtypableTypesSubstitutor(this)
    }

    // ResultTypeResolver.Context, VariableFixationFinder.Context
    override fun isReified(variable: TypeVariableMarker): Boolean {
        return with(utilContext) { variable.isReified() }
    }

    override fun bindingStubsForPostponedVariables(): Map<TypeVariableMarker, StubTypeMarker> {
        checkState(State.BUILDING, State.COMPLETION)
        // TODO: SUB
        return storage.postponedTypeVariables.associateWith { createStubTypeForBuilderInference(it) }
    }

    override fun currentStorage(): ConstraintStorage {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return storage
    }

    @K2Only
    val usesOuterCs: Boolean get() = storage.usesOuterCs

    // PostponedArgumentsAnalyzer.Context
    override fun hasUpperOrEqualUnitConstraint(type: KotlinTypeMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION, State.FREEZED)
        val constraints = storage.notFixedTypeVariables[type.typeConstructor()]?.constraints ?: return false
        return constraints.any {
            (it.kind == ConstraintKind.UPPER || it.kind == ConstraintKind.EQUALITY) &&
                    it.type.lowerBoundIfFlexible().isUnit()
        }
    }

    override fun removePostponedTypeVariablesFromConstraints(postponedTypeVariables: Set<TypeConstructorMarker>) {
        for ((_, variableWithConstraints) in storage.notFixedTypeVariables) {
            variableWithConstraints.removeConstraints { constraint ->
                constraint.type.contains { it is StubTypeMarker && it.getOriginalTypeVariable() in postponedTypeVariables }
            }
        }
    }

    override fun recordTypeVariableReferenceInConstraint(
        constraintOwner: TypeConstructorMarker,
        referencedVariable: TypeConstructorMarker,
    ) {
        typeVariableDependencies.getOrPut(referencedVariable) { mutableSetOf() }
            .add(constraintOwner)
    }
}
