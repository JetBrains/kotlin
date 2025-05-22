/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.SmartSet
import java.util.*

// todo problem: intersection types in constrains: A <: Number, B <: Inv<A & Any> =>? B <: Inv<out Number & Any>
class ConstraintIncorporator(
    val typeApproximator: AbstractTypeApproximator,
    val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    val utilContext: ConstraintSystemUtilContext,
    private val languageVersionSettings: LanguageVersionSettings,
    val constraintsLogger: ConstraintsLogger? = null,
) {
    interface Context : TypeSystemInferenceExtensionContext {
        val allTypeVariablesWithConstraints: Collection<VariableWithConstraints>

        fun getVariablesWithConstraintsContainingGivenTypeVariable(
            variableConstructorMarker: TypeConstructorMarker,
        ): Collection<VariableWithConstraints>

        // if such type variable is fixed then it is error
        fun getTypeVariable(typeConstructor: TypeConstructorMarker): TypeVariableMarker?

        fun getConstraintsForVariable(typeVariable: TypeVariableMarker): List<Constraint>

        // A <:(=) \alpha <:(=) B => A <: B
        fun processNewInitialConstraintFromIncorporation(
            // A
            lowerType: KotlinTypeMarker,
            // B
            upperType: KotlinTypeMarker,
            shouldTryUseDifferentFlexibilityForUpperType: Boolean,
            // Union of `derivedFrom` for `A <:(=) \alpha` and `\alpha <:(=) B`
            newDerivedFrom: Set<TypeVariableMarker>,
            isFromNullabilityConstraint: Boolean = false,
            isFromDeclaredUpperBound: Boolean = false,
        )

        fun addNewIncorporatedConstraint(typeVariable: TypeVariableMarker, type: KotlinTypeMarker, constraintContext: ConstraintContext)
    }

    // \alpha is typeVariable, \beta -- other type variable registered in ConstraintStorage
    fun incorporate(c: Context, typeVariable: TypeVariableMarker, constraint: Constraint) {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        // we shouldn't incorporate recursive constraint -- It is too dangerous
        if (c.areThereRecursiveConstraints(typeVariable, constraint)) return

        c.directWithVariable(typeVariable, constraint)
        c.insideOtherConstraint(typeVariable, constraint)
    }

    private fun Context.areThereRecursiveConstraints(typeVariable: TypeVariableMarker, constraint: Constraint) =
        constraint.type.contains { it.typeConstructor().unwrapStubTypeVariableConstructor() == typeVariable.freshTypeConstructor() }

    // A <:(=) \alpha <:(=) B => A <: B
    private fun Context.directWithVariable(
        typeVariable: TypeVariableMarker,
        constraint: Constraint,
    ) {
        val shouldBeTypeVariableFlexible =
            if (useRefinedBoundsForTypeVariableInFlexiblePosition())
                false
            else
                with(utilContext) { typeVariable.shouldBeFlexible() }

        // \alpha <: constraint.type
        if (constraint.kind != ConstraintKind.LOWER) {
            forEachConstraint(typeVariable) {
                if (it.kind != ConstraintKind.UPPER) {
                    constraintsLogger.withPrevious(
                        typeVariable, it,
                        typeVariable, constraint,
                    ) {
                        processNewInitialConstraintFromIncorporation(
                            it.type,
                            constraint.type,
                            shouldBeTypeVariableFlexible,
                            constraint.computeNewDerivedFrom(it),
                            it.isNullabilityConstraint
                        )
                    }
                }
            }
        }

        // constraint.type <: \alpha
        if (constraint.kind != ConstraintKind.UPPER) {
            forEachConstraint(typeVariable) {
                if (it.kind != ConstraintKind.LOWER) {
                    val isFromDeclaredUpperBound =
                        it.position.from is DeclaredUpperBoundConstraintPosition<*> && !it.type.typeConstructor().isTypeVariable()

                    constraintsLogger.withPrevious(
                        typeVariable, constraint,
                        typeVariable, it,
                    ) {
                        processNewInitialConstraintFromIncorporation(
                            constraint.type,
                            it.type,
                            shouldBeTypeVariableFlexible,
                            constraint.computeNewDerivedFrom(it),
                            isFromDeclaredUpperBound = isFromDeclaredUpperBound
                        )
                    }
                }
            }
        }
    }

    // NB: The result is reflexive
    private fun Constraint.computeNewDerivedFrom(other: Constraint): Set<TypeVariableMarker> =
        when {
            !languageVersionSettings.supportsFeature(LanguageFeature.StricterConstraintIncorporationRecursionDetector) -> emptySet()
            derivedFrom.isEmpty() -> other.derivedFrom
            other.derivedFrom.isEmpty() -> derivedFrom
            else -> derivedFrom + other.derivedFrom
        }

    private inline fun Context.forEachConstraint(typeVariable: TypeVariableMarker, action: (Constraint) -> Unit) {
        // We use an indexed loop because the collection might be modified during the iteration.
        // However, the only modification is appending, so we should be fine.
        val constraints = getConstraintsForVariable(typeVariable)
        var i = 0
        while (i < constraints.size) {
            action(constraints[i++])
        }
    }

    // \alpha <: Number, \beta <: Inv<\alpha> => \beta <: Inv<out Number>
    private fun Context.insideOtherConstraint(
        typeVariable: TypeVariableMarker,
        constraint: Constraint,
    ) {
        if (typeVariable in constraint.derivedFrom) return
        val freshTypeConstructor = typeVariable.freshTypeConstructor()
        for (storageForOtherVariable in getVariablesWithConstraintsContainingGivenTypeVariable(freshTypeConstructor)) {
            for (otherConstraint in storageForOtherVariable.getConstraintsContainedSpecifiedTypeVariable(freshTypeConstructor)) {
                constraintsLogger.withPrevious(
                    typeVariable, constraint,
                    storageForOtherVariable.typeVariable, otherConstraint,
                ) {
                    generateNewConstraintForSecondIncorporationKind(
                        typeVariable,
                        constraint,
                        storageForOtherVariable.typeVariable,
                        otherConstraint
                    )
                }
            }
        }
    }


    // By "Second" we mean `insideOtherConstraint` here
    // \alpha <: Number, \beta <: Inv<\alpha> => \beta <: Inv<out Number>
    private fun Context.generateNewConstraintForSecondIncorporationKind(
        // \alpha
        causeOfIncorporationVariable: TypeVariableMarker,
        // \alpha <: Number
        causeOfIncorporationConstraint: Constraint,
        // \beta
        otherVariable: TypeVariableMarker,
        // \beta <: Inv<\alpha>
        otherConstraint: Constraint,
    ) {
        if (causeOfIncorporationVariable in otherConstraint.derivedFrom) return
        val (type, needApproximation) = computeConstraintTypeForSecondIncorporationKind(
            causeOfIncorporationVariable, causeOfIncorporationConstraint, otherConstraint
        )

        fun prepareType(toSuper: Boolean): KotlinTypeMarker =
            when {
                needApproximation -> approximateCapturedTypes(type, toSuper)
                else -> type
            }

        if (otherConstraint.kind != ConstraintKind.LOWER) {
            addNewConstraintForSecondIncorporationKind(
                causeOfIncorporationVariable,
                causeOfIncorporationConstraint,
                otherVariable,
                otherConstraint,
                prepareType(true),
                isSubtype = false
            )
        }

        if (otherConstraint.kind != ConstraintKind.UPPER) {
            addNewConstraintForSecondIncorporationKind(
                causeOfIncorporationVariable,
                causeOfIncorporationConstraint,
                otherVariable,
                otherConstraint,
                prepareType(false),
                isSubtype = true
            )
        }
    }

    /**
     * By "Second" we mean `insideOtherConstraint` here
     * \alpha <: Number, \beta <: Inv<\alpha> => \beta <: Inv<out Number>
     *  The second boolean component defines if further approximation is required.
     *
     *  @return `Pair(Inv<Captured(out Number)>, true)`
     */
    private fun Context.computeConstraintTypeForSecondIncorporationKind(
        // \alpha
        causeOfIncorporationVariable: TypeVariableMarker,
        // \alpha <: Number
        causeOfIncorporationConstraint: Constraint,
        // \beta <: Inv<\alpha>
        otherConstraint: Constraint,
    ): Pair<KotlinTypeMarker, Boolean> {
        val isBaseGenericType = otherConstraint.type.argumentsCount() != 0
        val isBaseOrOtherCapturedType = otherConstraint.type.isCapturedType() || causeOfIncorporationConstraint.type.isCapturedType()

        val (alphaReplacement, needsApproximation) = when (causeOfIncorporationConstraint.kind) {
            ConstraintKind.EQUALITY -> {
                causeOfIncorporationConstraint.type to false
            }
            ConstraintKind.UPPER -> {
                /*
                 * Creating a captured type isn't needed due to its future approximation to `Nothing` or itself
                 * Example:
                 *      targetVariable = TypeVariable(A)
                 *      baseConstraint = LOWER(TypeVariable(B))
                 *      otherConstraint = UPPER(Number)
                 *      incorporatedConstraint = Approx(CapturedType(out Number)) <: TypeVariable(A) => Nothing <: TypeVariable(A)
                 * TODO: implement this for generics and captured types
                 */
                when {
                    otherConstraint.kind == ConstraintKind.LOWER && !isBaseGenericType && !isBaseOrOtherCapturedType ->
                        nothingType() to false
                    otherConstraint.kind == ConstraintKind.UPPER && !isBaseGenericType && !isBaseOrOtherCapturedType ->
                        causeOfIncorporationConstraint.type to false
                    else ->
                        createCapturedType(
                            createTypeArgument(causeOfIncorporationConstraint.type, TypeVariance.OUT),
                            listOf(causeOfIncorporationConstraint.type),
                            null,
                            CaptureStatus.FOR_INCORPORATION
                        ) to true
                }
            }
            ConstraintKind.LOWER -> {
                /*
                 * Creating a captured type isn't needed due to its future approximation to `Any?` or itself
                 * Example:
                 *      targetVariable = TypeVariable(A)
                 *      baseConstraint = UPPER(TypeVariable(B))
                 *      otherConstraint = LOWER(Number)
                 *      incorporatedConstraint = TypeVariable(A) <: Approx(CapturedType(in Number)) => TypeVariable(A) <: Any?
                 * TODO: implement this for generics and captured types
                 */
                when {
                    otherConstraint.kind == ConstraintKind.UPPER && !isBaseGenericType && !isBaseOrOtherCapturedType ->
                        nullableAnyType() to false
                    otherConstraint.kind == ConstraintKind.LOWER && !isBaseGenericType && !isBaseOrOtherCapturedType ->
                        causeOfIncorporationConstraint.type to false
                    else ->
                        createCapturedType(
                            createTypeArgument(causeOfIncorporationConstraint.type, TypeVariance.IN),
                            emptyList(),
                            causeOfIncorporationConstraint.type,
                            CaptureStatus.FOR_INCORPORATION
                        ) to true
                }
            }
        }

        return otherConstraint.type.substitute(this, causeOfIncorporationVariable, alphaReplacement) to needsApproximation
    }

    // By "Second" we mean `insideOtherConstraint` here
    // \alpha <: Number, \beta <: Inv<\alpha> => \beta <: Inv<out Number>
    private fun Context.addNewConstraintForSecondIncorporationKind(
        // \alpha
        causeOfIncorporationVariable: TypeVariableMarker,
        // \alpha <: Number
        causeOfIncorporationConstraint: Constraint,
        // \beta
        targetVariable: TypeVariableMarker,
        // \beta <: Inv<\alpha>
        otherConstraint: Constraint,
        // Inv<out Number>
        newConstraintType: KotlinTypeMarker,
        isSubtype: Boolean,
    ) {
        if (targetVariable in getNestedTypeVariables(newConstraintType)) return

        val isUsefulForNullabilityConstraint =
            isPotentialUsefulNullabilityConstraint(
                newConstraintType,
                causeOfIncorporationConstraint.type,
                causeOfIncorporationConstraint.kind,
            )
        val isFromVariableFixation = otherConstraint.position.from is FixVariableConstraintPosition<*>
                || causeOfIncorporationConstraint.position.from is FixVariableConstraintPosition<*>

        if (!causeOfIncorporationConstraint.kind.isEqual() &&
            !isUsefulForNullabilityConstraint &&
            !isFromVariableFixation &&
            !containsConstrainingTypeWithoutProjection(newConstraintType, causeOfIncorporationConstraint)
        ) return

        if (trivialConstraintTypeInferenceOracle.isGeneratedConstraintTrivial(
                otherConstraint, causeOfIncorporationConstraint, newConstraintType, isSubtype
            )
        ) return

        val derivedFrom = SmartSet.create(otherConstraint.derivedFrom).also { it.addAll(causeOfIncorporationConstraint.derivedFrom) }
        derivedFrom.add(causeOfIncorporationVariable)

        val kind = if (isSubtype) ConstraintKind.LOWER else ConstraintKind.UPPER

        val inputTypePosition =
            otherConstraint.position.from as? OnlyInputTypeConstraintPosition ?: otherConstraint.inputTypePositionBeforeIncorporation

        val isNewConstraintUsefulForNullability = isUsefulForNullabilityConstraint && newConstraintType.isNullableNothing()
        val isOtherConstraintUsefulForNullability =
            causeOfIncorporationConstraint.isNullabilityConstraint && causeOfIncorporationConstraint.type.isNullableNothing()
        val isNullabilityConstraint = isNewConstraintUsefulForNullability || isOtherConstraintUsefulForNullability

        val constraintContext = ConstraintContext(kind, derivedFrom, inputTypePosition, isNullabilityConstraint)

        addNewIncorporatedConstraint(targetVariable, newConstraintType, constraintContext)
    }

    private fun Context.containsConstrainingTypeWithoutProjection(
        newConstraint: KotlinTypeMarker,
        otherConstraint: Constraint,
    ): Boolean {
        return getNestedArguments(newConstraint).any {
            it.getType()?.typeConstructor() == otherConstraint.type.typeConstructor() && it.getVariance() == TypeVariance.INV
        }
    }

    private fun Context.isPotentialUsefulNullabilityConstraint(
        newConstraint: KotlinTypeMarker,
        otherConstraint: KotlinTypeMarker,
        kind: ConstraintKind,
    ): Boolean {
        if (trivialConstraintTypeInferenceOracle.isSuitableResultedType(newConstraint)) return false

        val otherConstraintCanAddNullabilityToNewOne =
            !newConstraint.isNullableType() && otherConstraint.isNullableType() && kind == ConstraintKind.LOWER
        val newConstraintCanAddNullabilityToOtherOne =
            newConstraint.isNullableType() && !otherConstraint.isNullableType() && kind == ConstraintKind.UPPER

        return otherConstraintCanAddNullabilityToNewOne || newConstraintCanAddNullabilityToOtherOne
    }

    private fun Context.getNestedTypeVariables(type: KotlinTypeMarker): List<TypeVariableMarker> =
        getNestedArguments(type).mapNotNullTo(SmartList()) {
            it.getType()?.let { getTypeVariable(it.typeConstructor().unwrapStubTypeVariableConstructor()) }
        }

    private fun KotlinTypeMarker.substitute(c: Context, typeVariable: TypeVariableMarker, value: KotlinTypeMarker): KotlinTypeMarker {
        val substitutor = c.typeSubstitutorByTypeConstructor(mapOf(typeVariable.freshTypeConstructor(c) to value))
        return substitutor.safeSubstitute(c, this)
    }


    private fun approximateCapturedTypes(type: KotlinTypeMarker, toSuper: Boolean): KotlinTypeMarker =
        if (toSuper) typeApproximator.approximateToSuperType(type, TypeApproximatorConfiguration.IncorporationConfiguration) ?: type
        else typeApproximator.approximateToSubType(type, TypeApproximatorConfiguration.IncorporationConfiguration) ?: type
}

private fun TypeSystemInferenceExtensionContext.getNestedArguments(type: KotlinTypeMarker): List<TypeArgumentMarker> {
    val result = SmartList<TypeArgumentMarker>()
    val stack = ArrayDeque<TypeArgumentMarker>()

    when (type) {
        is FlexibleTypeMarker -> {
            stack.push(createTypeArgument(type.lowerBound(), TypeVariance.INV))
            stack.push(createTypeArgument(type.upperBound(), TypeVariance.INV))
        }
        else -> stack.push(createTypeArgument(type, TypeVariance.INV))
    }

    stack.push(createTypeArgument(type, TypeVariance.INV))

    val addArgumentsToStack = { projectedType: KotlinTypeMarker ->
        for (argumentIndex in 0 until projectedType.argumentsCount()) {
            stack.add(projectedType.getArgument(argumentIndex))
        }
    }

    while (!stack.isEmpty()) {
        val typeProjection = stack.pop()
        val typeProjectionType = typeProjection.getType() ?: continue

        result.add(typeProjection)

        when (val projectedType = typeProjectionType) {
            is FlexibleTypeMarker -> {
                addArgumentsToStack(projectedType.lowerBound())
                addArgumentsToStack(projectedType.upperBound())
            }
            else -> addArgumentsToStack(projectedType)
        }
    }
    return result
}
