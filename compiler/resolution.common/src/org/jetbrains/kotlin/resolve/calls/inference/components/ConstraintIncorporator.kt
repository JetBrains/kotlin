/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.builtins.functions.AllowedToUsedOnlyInK1
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorCachesPerConfiguration
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
    inferenceLoggerParameter: InferenceLogger? = null,
) {
    /**
     * A workaround for K1's DI: the dummy instance must be provided, but
     * because it's useless, it's better to avoid calling its members to
     * prevent performance penalties.
     */
    @OptIn(AllowedToUsedOnlyInK1::class)
    val inferenceLogger = inferenceLoggerParameter.takeIf { it !is InferenceLogger.Dummy }

    interface Context : TypeSystemInferenceExtensionContext {
        val allTypeVariablesWithConstraints: Collection<VariableWithConstraints>

        fun getVariablesWithConstraintsContainingGivenTypeVariable(
            variableConstructorMarker: TypeConstructorMarker,
        ): Collection<VariableWithConstraints>

        // if such a type variable is fixed then it is error
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
            isFromNullabilityConstraint: Boolean,
            isFromDeclaredUpperBound: Boolean,
            isNoInfer: Boolean,
        )

        fun addNewIncorporatedConstraint(typeVariable: TypeVariableMarker, type: KotlinTypeMarker, constraintContext: ConstraintContext)

        val approximatorCaches: TypeApproximatorCachesPerConfiguration
    }

    // \alpha is typeVariable, \beta -- other type variable registered in ConstraintStorage
    context(c: Context)
    fun incorporate(typeVariable: TypeVariableMarker, constraint: Constraint) {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        // we shouldn't incorporate recursive constraint -- It is too dangerous
        if (constraint.areThereRecursiveConstraints(typeVariable)) return

        directWithVariable(typeVariable, constraint)
        insideOtherConstraint(typeVariable, constraint)
    }

    context(c: Context)
    private fun Constraint.areThereRecursiveConstraints(typeVariable: TypeVariableMarker) =
        type.contains { it.typeConstructor().unwrapStubTypeVariableConstructor() == typeVariable.freshTypeConstructor() }

    // A <:(=) \alpha <:(=) B => A <: B
    context(c: Context)
    private fun directWithVariable(typeVariable: TypeVariableMarker, constraint: Constraint) {
        val shouldBeTypeVariableFlexible = with(utilContext) { typeVariable.shouldBeFlexible() }

        // \alpha <: constraint.type
        if (constraint.kind != ConstraintKind.LOWER) {
            typeVariable.forEachConstraint {
                if (it.kind != ConstraintKind.UPPER) {
                    inferenceLogger.withOrigins(
                        typeVariable, it,
                        typeVariable, constraint,
                    ) {
                        val upperType = if (!it.isFromFlexiblePosition ||
                            constraint.type.isMarkedNullable()
                        ) constraint.type else constraint.type.toFlexible()
                        c.processNewInitialConstraintFromIncorporation(
                            lowerType = it.type,
                            upperType = upperType,
                            shouldTryUseDifferentFlexibilityForUpperType = shouldBeTypeVariableFlexible,
                            newDerivedFrom = constraint.computeNewDerivedFrom(it),
                            isFromNullabilityConstraint = it.isNullabilityConstraint,
                            isFromDeclaredUpperBound = false,
                            isNoInfer = constraint.isNoInfer || it.isNoInfer,
                        )
                    }
                }
            }
        }

        // constraint.type <: \alpha
        if (constraint.kind != ConstraintKind.UPPER) {
            typeVariable.forEachConstraint {
                if (it.kind != ConstraintKind.LOWER) {
                    val isFromDeclaredUpperBound =
                        it.position.from is DeclaredUpperBoundConstraintPosition<*> && !it.type.typeConstructor().isTypeVariable()

                    inferenceLogger.withOrigins(
                        typeVariable, constraint,
                        typeVariable, it,
                    ) {
                        val upperType = if (!constraint.isFromFlexiblePosition ||
                            it.type.isMarkedNullable()
                        ) it.type else it.type.toFlexible()
                        c.processNewInitialConstraintFromIncorporation(
                            lowerType = constraint.type,
                            upperType = upperType,
                            shouldTryUseDifferentFlexibilityForUpperType = shouldBeTypeVariableFlexible,
                            newDerivedFrom = constraint.computeNewDerivedFrom(it),
                            isFromDeclaredUpperBound = isFromDeclaredUpperBound,
                            isFromNullabilityConstraint = false,
                            isNoInfer = constraint.isNoInfer || it.isNoInfer,
                        )
                    }
                }
            }
        }
    }

    context(c: Context)
    private fun KotlinTypeMarker.toFlexible(): KotlinTypeMarker {
        return (asFlexibleType() ?: this).let {
            if (it is FlexibleTypeMarker && it.upperBound().isMarkedNullable()) return it
            val lowerBound = it.makeDefinitelyNotNullOrNotNull().asRigidType()!!
            val upperBound = it.withNullability(nullable = true).asRigidType()!!
            c.createFlexibleType(lowerBound, upperBound)
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

    context(c: Context)
    private inline fun TypeVariableMarker.forEachConstraint(action: (Constraint) -> Unit) {
        // We use an indexed loop because the collection might be modified during the iteration.
        // However, the only modification is appending, so we should be fine.
        val constraints = c.getConstraintsForVariable(this)
        var i = 0
        while (i < constraints.size) {
            action(constraints[i++])
        }
    }

    // \alpha <: Number, \beta <: Inv<\alpha> => \beta <: Inv<out Number>
    context(c: Context)
    private fun insideOtherConstraint(
        typeVariable: TypeVariableMarker,
        constraint: Constraint,
    ) {
        if (typeVariable in constraint.derivedFrom) return
        val freshTypeConstructor = typeVariable.freshTypeConstructor()
        for (storageForOtherVariable in c.getVariablesWithConstraintsContainingGivenTypeVariable(freshTypeConstructor)) {
            for (otherConstraint in storageForOtherVariable.getConstraintsContainedSpecifiedTypeVariable(freshTypeConstructor)) {
                inferenceLogger.withOrigins(
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
    context(c: Context)
    private fun generateNewConstraintForSecondIncorporationKind(
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
    context(c: Context)
    private fun computeConstraintTypeForSecondIncorporationKind(
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
                when (otherConstraint.kind) {
                    ConstraintKind.LOWER if !isBaseGenericType && !isBaseOrOtherCapturedType -> c.nothingType() to false
                    ConstraintKind.UPPER if !isBaseGenericType && !isBaseOrOtherCapturedType -> causeOfIncorporationConstraint.type to false
                    else -> c.createCapturedType(
                        c.createTypeArgument(causeOfIncorporationConstraint.type, TypeVariance.OUT),
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
                when (otherConstraint.kind) {
                    ConstraintKind.UPPER if !isBaseGenericType && !isBaseOrOtherCapturedType -> c.nullableAnyType() to false
                    ConstraintKind.LOWER if !isBaseGenericType && !isBaseOrOtherCapturedType -> causeOfIncorporationConstraint.type to false
                    else -> c.createCapturedType(
                        c.createTypeArgument(causeOfIncorporationConstraint.type, TypeVariance.IN),
                        emptyList(),
                        causeOfIncorporationConstraint.type,
                        CaptureStatus.FOR_INCORPORATION
                    ) to true
                }
            }
        }

        return otherConstraint.type.substitute(causeOfIncorporationVariable, alphaReplacement) to needsApproximation
    }

    // By "Second" we mean `insideOtherConstraint` here
    // \alpha <: Number, \beta <: Inv<\alpha> => \beta <: Inv<out Number>
    context(c: Context)
    private fun addNewConstraintForSecondIncorporationKind(
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
        if (targetVariable in newConstraintType.getNestedTypeVariables()) return

        val isUsefulForNullabilityConstraint =
            newConstraintType.isPotentialUsefulNullabilityConstraint(
                causeOfIncorporationConstraint.type,
                causeOfIncorporationConstraint.kind,
            )
        val isFromVariableFixation = otherConstraint.position.from is FixVariableConstraintPosition<*>
                || causeOfIncorporationConstraint.position.from is FixVariableConstraintPosition<*>

        if (!causeOfIncorporationConstraint.kind.isEqual() &&
            !isUsefulForNullabilityConstraint &&
            !isFromVariableFixation &&
            !newConstraintType.containsConstrainingTypeWithoutProjection(causeOfIncorporationConstraint)
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

        val constraintContext = ConstraintContext(
            kind = kind,
            derivedFrom = derivedFrom,
            inputTypePositionBeforeIncorporation = inputTypePosition,
            isNullabilityConstraint = isNullabilityConstraint,
            isFromFlexiblePosition = false,
            isNoInfer = causeOfIncorporationConstraint.isNoInfer || otherConstraint.isNoInfer
        )

        c.addNewIncorporatedConstraint(targetVariable, newConstraintType, constraintContext)
    }

    context(c: Context)
    private fun KotlinTypeMarker.containsConstrainingTypeWithoutProjection(otherConstraint: Constraint): Boolean {
        return getNestedArguments().any {
            it.getType()?.typeConstructor() == otherConstraint.type.typeConstructor() && it.getVariance() == TypeVariance.INV
        }
    }

    context(c: Context)
    private fun KotlinTypeMarker.isPotentialUsefulNullabilityConstraint(otherConstraint: KotlinTypeMarker, kind: ConstraintKind): Boolean {
        if (trivialConstraintTypeInferenceOracle.isSuitableResultedType(this)) return false

        val otherConstraintCanAddNullabilityToNewOne =
            !isNullableType(considerTypeVariableBounds = false) && otherConstraint.isNullableType(considerTypeVariableBounds = false) && kind == ConstraintKind.LOWER
        val newConstraintCanAddNullabilityToOtherOne =
            isNullableType(considerTypeVariableBounds = false) && !otherConstraint.isNullableType(considerTypeVariableBounds = false) && kind == ConstraintKind.UPPER

        return otherConstraintCanAddNullabilityToNewOne || newConstraintCanAddNullabilityToOtherOne
    }

    context(c: Context)
    private fun KotlinTypeMarker.getNestedTypeVariables(): List<TypeVariableMarker> {
        return getNestedArguments().mapNotNullTo(SmartList()) { typeArgument ->
            typeArgument.getType()?.let { c.getTypeVariable(it.typeConstructor().unwrapStubTypeVariableConstructor()) }
        }
    }

    context(c: Context)
    private fun KotlinTypeMarker.substitute(typeVariable: TypeVariableMarker, value: KotlinTypeMarker): KotlinTypeMarker {
        val substitutor = c.typeSubstitutorByTypeConstructor(mapOf(typeVariable.freshTypeConstructor(c) to value))
        return substitutor.safeSubstitute(c, this)
    }

    context(c: Context)
    private fun approximateCapturedTypes(type: KotlinTypeMarker, toSuper: Boolean): KotlinTypeMarker =
        when {
            toSuper -> typeApproximator.approximateToSuperType(
                type, TypeApproximatorConfiguration.IncorporationConfiguration,
                c.approximatorCaches,
            ) ?: type
            else -> typeApproximator.approximateToSubType(
                type, TypeApproximatorConfiguration.IncorporationConfiguration,
                c.approximatorCaches,
            ) ?: type
        }
}

context(c: TypeSystemInferenceExtensionContext)
private fun KotlinTypeMarker.getNestedArguments(): List<TypeArgumentMarker> {
    val result = SmartList<TypeArgumentMarker>()
    val stack = ArrayDeque<TypeArgumentMarker>()

    when (this) {
        is FlexibleTypeMarker -> {
            stack.push(c.createTypeArgument(this.lowerBound(), TypeVariance.INV))
            stack.push(c.createTypeArgument(this.upperBound(), TypeVariance.INV))
        }
        else -> stack.push(c.createTypeArgument(this, TypeVariance.INV))
    }

    stack.push(c.createTypeArgument(this, TypeVariance.INV))

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
