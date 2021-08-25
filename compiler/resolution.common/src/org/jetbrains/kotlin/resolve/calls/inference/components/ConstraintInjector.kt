/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.SmartList
import kotlin.math.max

class ConstraintInjector(
    val constraintIncorporator: ConstraintIncorporator,
    val typeApproximator: AbstractTypeApproximator,
    private val languageVersionSettings: LanguageVersionSettings,
) {
    private val ALLOWED_DEPTH_DELTA_FOR_INCORPORATION = 1

    interface Context : TypeSystemInferenceExtensionContext {
        val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>

        var maxTypeDepthFromInitialConstraints: Int
        val notFixedTypeVariables: MutableMap<TypeConstructorMarker, MutableVariableWithConstraints>
        val fixedTypeVariables: MutableMap<TypeConstructorMarker, KotlinTypeMarker>

        fun addInitialConstraint(initialConstraint: InitialConstraint)
        fun addError(error: ConstraintSystemError)

        fun addMissedConstraints(
            position: IncorporationConstraintPosition,
            constraints: MutableList<Pair<TypeVariableMarker, Constraint>>
        )
    }

    fun addInitialSubtypeConstraint(c: Context, lowerType: KotlinTypeMarker, upperType: KotlinTypeMarker, position: ConstraintPosition) {
        val initialConstraint = InitialConstraint(lowerType, upperType, UPPER, position).also { c.addInitialConstraint(it) }
        val typeCheckerState = TypeCheckerStateForConstraintInjector(c, IncorporationConstraintPosition(position, initialConstraint))

        updateAllowedTypeDepth(c, lowerType)
        updateAllowedTypeDepth(c, upperType)

        addSubTypeConstraintAndIncorporateIt(c, lowerType, upperType, typeCheckerState)
    }

    private fun Context.addInitialEqualityConstraintThroughSubtyping(
        a: KotlinTypeMarker,
        b: KotlinTypeMarker,
        typeCheckerState: TypeCheckerStateForConstraintInjector
    ) {
        updateAllowedTypeDepth(this, a)
        updateAllowedTypeDepth(this, b)
        addSubTypeConstraintAndIncorporateIt(this, a, b, typeCheckerState)
        addSubTypeConstraintAndIncorporateIt(this, b, a, typeCheckerState)
    }

    fun addInitialEqualityConstraint(c: Context, a: KotlinTypeMarker, b: KotlinTypeMarker, position: ConstraintPosition) = with(c) {
        val (typeVariable, equalType) = when {
            a.typeConstructor(c) is TypeVariableTypeConstructorMarker -> a to b
            b.typeConstructor(c) is TypeVariableTypeConstructorMarker -> b to a
            else -> return
        }
        val initialConstraint = InitialConstraint(typeVariable, equalType, EQUALITY, position).also { c.addInitialConstraint(it) }
        val typeCheckerState = TypeCheckerStateForConstraintInjector(c, IncorporationConstraintPosition(position, initialConstraint))

        // We add constraints like `T? == Foo!` in the old way
        if (!typeVariable.isSimpleType() || typeVariable.isMarkedNullable()) {
            addInitialEqualityConstraintThroughSubtyping(typeVariable, equalType, typeCheckerState)
            return
        }

        updateAllowedTypeDepth(c, equalType)
        addEqualityConstraintAndIncorporateIt(c, typeVariable, equalType, typeCheckerState)
    }

    private fun addSubTypeConstraintAndIncorporateIt(
        c: Context,
        lowerType: KotlinTypeMarker,
        upperType: KotlinTypeMarker,
        typeCheckerState: TypeCheckerStateForConstraintInjector
    ) {
        typeCheckerState.setConstrainingTypesToPrintDebugInfo(lowerType, upperType)
        typeCheckerState.runIsSubtypeOf(lowerType, upperType)

        // Missed constraints are constraints which we skipped in the constraints processor by mistake (incorrect optimization)
        val missedConstraints = processConstraints(c, typeCheckerState)

        if (missedConstraints != null) {
            c.addMissedConstraints(typeCheckerState.position, missedConstraints)
        }
    }

    private fun addEqualityConstraintAndIncorporateIt(
        c: Context,
        typeVariable: KotlinTypeMarker,
        equalType: KotlinTypeMarker,
        typeCheckerState: TypeCheckerStateForConstraintInjector
    ) {
        typeCheckerState.setConstrainingTypesToPrintDebugInfo(typeVariable, equalType)
        typeCheckerState.addEqualityConstraint(typeVariable.typeConstructor(c), equalType)

        // Missed constraints are constraints which we skipped in the constraints processor by mistake (incorrect optimization)
        val missedConstraints = processConstraints(c, typeCheckerState)

        if (missedConstraints != null) {
            c.addMissedConstraints(typeCheckerState.position, missedConstraints)
        }
    }

    fun processMissedConstraints(
        c: Context,
        position: IncorporationConstraintPosition,
        missedConstraints: List<Pair<TypeVariableMarker, Constraint>>
    ) {
        val properConstraintsProcessingEnabled =
            languageVersionSettings.supportsFeature(LanguageFeature.ProperTypeInferenceConstraintsProcessing)

        // If proper constraints processing is enabled, then we don't have missed constraints
        if (properConstraintsProcessingEnabled) return

        val typeCheckerState = TypeCheckerStateForConstraintInjector(c, position)
        for ((variable, constraint) in missedConstraints) {
            typeCheckerState.addPossibleNewConstraint(variable, constraint)
        }
        processConstraints(c, typeCheckerState, skipProperEqualityConstraints = false)
    }

    private fun processConstraints(
        c: Context,
        typeCheckerState: TypeCheckerStateForConstraintInjector,
        skipProperEqualityConstraints: Boolean = true
    ): MutableList<Pair<TypeVariableMarker, Constraint>>? {
        val properConstraintsProcessingEnabled =
            languageVersionSettings.supportsFeature(LanguageFeature.ProperTypeInferenceConstraintsProcessing)

        while (typeCheckerState.hasConstraintsToProcess()) {
            processGivenConstraints(c, typeCheckerState, typeCheckerState.extractAllConstraints()!!)

            val contextOps = c as? ConstraintSystemOperation

            val useIncorrectOptimization = skipProperEqualityConstraints && !properConstraintsProcessingEnabled

            if (!useIncorrectOptimization) continue

            // Optimization below is wrong and it's going to be removed after finished the corresponding deprecation cycle
            val hasProperEqualityConstraintForEachVariable = contextOps != null && c.notFixedTypeVariables.all { typeVariable ->
                typeVariable.value.constraints.any { constraint ->
                    constraint.kind == EQUALITY && contextOps.isProperType(constraint.type)
                }
            }

            if (hasProperEqualityConstraintForEachVariable) return typeCheckerState.extractAllConstraints()
        }
        return null
    }

    private fun processGivenConstraints(
        c: Context,
        typeCheckerState: TypeCheckerStateForConstraintInjector,
        constraintsToProcess: MutableList<Pair<TypeVariableMarker, Constraint>>
    ) {
        for ((typeVariable, constraint) in constraintsToProcess) {
            if (c.shouldWeSkipConstraint(typeVariable, constraint)) continue

            val constraints =
                c.notFixedTypeVariables[typeVariable.freshTypeConstructor(c)] ?: typeCheckerState.fixedTypeVariable(typeVariable)

            // it is important, that we add constraint here(not inside TypeCheckerContext), because inside incorporation we read constraints
            val (addedOrNonRedundantExistedConstraint, wasAdded) = constraints.addConstraint(constraint)
            val positionFrom = constraint.position.from
            val constraintToIncorporate = when {
                wasAdded && !constraint.isNullabilityConstraint -> addedOrNonRedundantExistedConstraint
                positionFrom is FixVariableConstraintPosition<*> && positionFrom.variable == typeVariable && constraint.kind == EQUALITY ->
                    addedOrNonRedundantExistedConstraint
                else -> null
            }

            if (constraintToIncorporate != null) {
                constraintIncorporator.incorporate(typeCheckerState, typeVariable, constraintToIncorporate)
            }
        }
    }

    private fun updateAllowedTypeDepth(c: Context, initialType: KotlinTypeMarker) = with(c) {
        c.maxTypeDepthFromInitialConstraints = max(c.maxTypeDepthFromInitialConstraints, initialType.typeDepth())
    }

    private fun Context.shouldWeSkipConstraint(typeVariable: TypeVariableMarker, constraint: Constraint): Boolean {
        if (constraint.kind == EQUALITY)
            return false

        val constraintType = constraint.type

        if (constraintType.typeConstructor() == typeVariable.freshTypeConstructor()) {
            if (constraintType.lowerBoundIfFlexible().isMarkedNullable() && constraint.kind == LOWER) return false // T? <: T

            return true // T <: T(?!)
        }

        if (constraint.position.from is DeclaredUpperBoundConstraintPosition<*> &&
            constraint.kind == UPPER && constraintType.isNullableAny()
        ) {
            return true // T <: Any?
        }

        return false
    }

    private fun Context.isAllowedType(type: KotlinTypeMarker) =
        type.typeDepth() <= maxTypeDepthFromInitialConstraints + ALLOWED_DEPTH_DELTA_FOR_INCORPORATION

    private inner class TypeCheckerStateForConstraintInjector(val c: Context, val position: IncorporationConstraintPosition) :
        TypeCheckerStateForConstraintSystem(c), ConstraintIncorporator.Context, TypeSystemInferenceExtensionContext by c {
        override val kotlinTypePreparator: AbstractTypePreparator
            get() = baseState.kotlinTypePreparator

        override val kotlinTypeRefiner: AbstractTypeRefiner
            get() = baseState.kotlinTypeRefiner

        // We use `var` intentionally to avoid extra allocations as this property is quite "hot"
        private var possibleNewConstraints: MutableList<Pair<TypeVariableMarker, Constraint>>? = null

        override val isInferenceCompatibilityEnabled = languageVersionSettings.supportsFeature(LanguageFeature.InferenceCompatibility)

        private var baseLowerType = position.initialConstraint.a
        private var baseUpperType = position.initialConstraint.b

        private var isIncorporatingConstraintFromDeclaredUpperBound = false

        fun extractAllConstraints() = possibleNewConstraints.also { possibleNewConstraints = null }

        fun addPossibleNewConstraint(variable: TypeVariableMarker, constraint: Constraint) {
            if (possibleNewConstraints == null) {
                possibleNewConstraints = SmartList()
            }
            possibleNewConstraints!!.add(variable to constraint)
        }

        fun hasConstraintsToProcess() = possibleNewConstraints != null

        fun setConstrainingTypesToPrintDebugInfo(lowerType: KotlinTypeMarker, upperType: KotlinTypeMarker) {
            baseLowerType = lowerType
            baseUpperType = upperType
        }

        val baseState: TypeCheckerState = newTypeCheckerState(isErrorTypeEqualsToAnything, isStubTypeEqualsToAnything)

        override fun substitutionSupertypePolicy(type: SimpleTypeMarker): SupertypesPolicy {
            return baseState.substitutionSupertypePolicy(type)
        }

        fun runIsSubtypeOf(
            lowerType: KotlinTypeMarker,
            upperType: KotlinTypeMarker,
            shouldTryUseDifferentFlexibilityForUpperType: Boolean = false,
            isFromNullabilityConstraint: Boolean = false
        ) {
            fun isSubtypeOf(upperType: KotlinTypeMarker) =
                AbstractTypeChecker.isSubtypeOf(
                    this@TypeCheckerStateForConstraintInjector as TypeCheckerState,
                    lowerType,
                    upperType,
                    isFromNullabilityConstraint
                )

            if (!isSubtypeOf(upperType)) {
                // todo improve error reporting -- add information about base types
                if (shouldTryUseDifferentFlexibilityForUpperType && upperType.isSimpleType()) {
                    /*
                     * Please don't reuse this logic.
                     * It's necessary to solve constraint systems when flexibility isn't propagated through a type variable.
                     * It's OK in the old inference because it uses already substituted types, that are with the correct flexibility.
                     */
                    require(upperType is SimpleTypeMarker)
                    val flexibleUpperType = createFlexibleType(upperType, upperType.withNullability(true))
                    if (!isSubtypeOf(flexibleUpperType)) {
                        c.addError(NewConstraintError(lowerType, flexibleUpperType, position))
                    }
                } else {
                    c.addError(NewConstraintError(lowerType, upperType, position))
                }
            }
        }

        // from AbstractTypeCheckerContextForConstraintSystem
        override fun isMyTypeVariable(type: SimpleTypeMarker): Boolean =
            c.allTypeVariables.containsKey(type.typeConstructor())

        override fun addUpperConstraint(typeVariable: TypeConstructorMarker, superType: KotlinTypeMarker) =
            addConstraint(typeVariable, superType, UPPER)

        override fun addLowerConstraint(
            typeVariable: TypeConstructorMarker,
            subType: KotlinTypeMarker,
            isFromNullabilityConstraint: Boolean
        ) = addConstraint(typeVariable, subType, LOWER, isFromNullabilityConstraint)

        override fun addEqualityConstraint(typeVariable: TypeConstructorMarker, type: KotlinTypeMarker) =
            addConstraint(typeVariable, type, EQUALITY, false)

        private fun isCapturedTypeFromSubtyping(type: KotlinTypeMarker) =
            when ((type as? CapturedTypeMarker)?.captureStatus()) {
                null, CaptureStatus.FROM_EXPRESSION -> false
                CaptureStatus.FOR_SUBTYPING -> true
                CaptureStatus.FOR_INCORPORATION ->
                    error("Captured type for incorporation shouldn't escape from incorporation: $type\n" + renderBaseConstraint())
            }

        private fun addConstraint(
            typeVariableConstructor: TypeConstructorMarker,
            type: KotlinTypeMarker,
            kind: ConstraintKind,
            isFromNullabilityConstraint: Boolean = false
        ) {
            val typeVariable = c.allTypeVariables[typeVariableConstructor]
                ?: error("Should by type variableConstructor: $typeVariableConstructor. ${c.allTypeVariables.values}")

            addNewIncorporatedConstraint(
                typeVariable,
                type,
                ConstraintContext(kind, emptySet(), isNullabilityConstraint = isFromNullabilityConstraint)
            )
        }

        private fun addNewIncorporatedConstraintFromDeclaredUpperBound(runIsSubtypeOf: Runnable) {
            isIncorporatingConstraintFromDeclaredUpperBound = true
            runIsSubtypeOf.run()
            isIncorporatingConstraintFromDeclaredUpperBound = false
        }

        // from ConstraintIncorporator.Context
        override fun addNewIncorporatedConstraint(
            lowerType: KotlinTypeMarker,
            upperType: KotlinTypeMarker,
            shouldTryUseDifferentFlexibilityForUpperType: Boolean,
            isFromNullabilityConstraint: Boolean,
            isFromDeclaredUpperBound: Boolean
        ) {
            if (lowerType === upperType) return
            if (c.isAllowedType(lowerType) && c.isAllowedType(upperType)) {
                fun runIsSubtypeOf() =
                    runIsSubtypeOf(lowerType, upperType, shouldTryUseDifferentFlexibilityForUpperType, isFromNullabilityConstraint)

                if (isFromDeclaredUpperBound) addNewIncorporatedConstraintFromDeclaredUpperBound(::runIsSubtypeOf) else runIsSubtypeOf()
            }
        }

        override fun addNewIncorporatedConstraint(
            typeVariable: TypeVariableMarker,
            type: KotlinTypeMarker,
            constraintContext: ConstraintContext
        ) {
            val (kind, derivedFrom, inputTypePosition, isNullabilityConstraint) = constraintContext

            var targetType = type
            if (targetType.isUninferredParameter()) {
                // there already should be an error, so there is no point in reporting one more
                return
            }

            if (targetType.isError()) {
                c.addError(ConstrainingTypeIsError(typeVariable, targetType, position))
                return
            }

            if (type.contains(this::isCapturedTypeFromSubtyping)) {
                // TypeVariable <: type -> if TypeVariable <: subType => TypeVariable <: type
                if (kind == UPPER) {
                    val subType =
                        typeApproximator.approximateToSubType(type, TypeApproximatorConfiguration.SubtypeCapturedTypesApproximation)
                    if (subType != null) {
                        targetType = subType
                    }
                }

                if (kind == LOWER) {
                    val superType =
                        typeApproximator.approximateToSuperType(type, TypeApproximatorConfiguration.SubtypeCapturedTypesApproximation)
                    if (superType != null) { // todo rethink error reporting for Any cases
                        targetType = superType
                    }
                }

                if (targetType === type) {
                    c.addError(CapturedTypeFromSubtyping(typeVariable, type, position))
                    return
                }
            }

            val position = if (isIncorporatingConstraintFromDeclaredUpperBound) position.copy(isFromDeclaredUpperBound = true) else position

            val newConstraint = Constraint(
                kind, targetType, position,
                derivedFrom = derivedFrom,
                isNullabilityConstraint = isNullabilityConstraint,
                inputTypePositionBeforeIncorporation = inputTypePosition
            )

            addPossibleNewConstraint(typeVariable, newConstraint)
        }

        override val allTypeVariablesWithConstraints: Collection<VariableWithConstraints>
            get() = c.notFixedTypeVariables.values

        override fun getTypeVariable(typeConstructor: TypeConstructorMarker): TypeVariableMarker? {
            val typeVariable = c.allTypeVariables[typeConstructor]
            if (typeVariable != null && !c.notFixedTypeVariables.containsKey(typeConstructor)) {
                fixedTypeVariable(typeVariable)
            }
            return typeVariable
        }

        override fun getConstraintsForVariable(typeVariable: TypeVariableMarker) =
            c.notFixedTypeVariables[typeVariable.freshTypeConstructor()]?.constraints
                ?: fixedTypeVariable(typeVariable)

        fun fixedTypeVariable(variable: TypeVariableMarker): Nothing {
            error(
                "Type variable $variable should not be fixed!\n" +
                        renderBaseConstraint()
            )
        }

        private fun renderBaseConstraint() = "Base constraint: $baseLowerType <: $baseUpperType from position: $position"
    }
}

data class ConstraintContext(
    val kind: ConstraintKind,
    val derivedFrom: Set<TypeVariableMarker>,
    val inputTypePositionBeforeIncorporation: OnlyInputTypeConstraintPosition? = null,
    val isNullabilityConstraint: Boolean
)
