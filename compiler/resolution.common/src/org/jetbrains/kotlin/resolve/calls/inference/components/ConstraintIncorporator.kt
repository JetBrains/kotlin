/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

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
    val utilContext: ConstraintSystemUtilContext
) {

    interface Context : TypeSystemInferenceExtensionContext {
        val allTypeVariablesWithConstraints: Collection<VariableWithConstraints>

        // if such type variable is fixed then it is error
        fun getTypeVariable(typeConstructor: TypeConstructorMarker): TypeVariableMarker?

        fun getConstraintsForVariable(typeVariable: TypeVariableMarker): List<Constraint>

        fun addNewIncorporatedConstraint(
            lowerType: KotlinTypeMarker,
            upperType: KotlinTypeMarker,
            shouldTryUseDifferentFlexibilityForUpperType: Boolean,
            isFromNullabilityConstraint: Boolean = false,
            isFromDeclaredUpperBound: Boolean = false
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
        constraint: Constraint
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
                    addNewIncorporatedConstraint(it.type, constraint.type, shouldBeTypeVariableFlexible, it.isNullabilityConstraint)
                }
            }
        }

        // constraint.type <: \alpha
        if (constraint.kind != ConstraintKind.UPPER) {
            forEachConstraint(typeVariable) {
                if (it.kind != ConstraintKind.LOWER) {
                    val isFromDeclaredUpperBound =
                        it.position.from is DeclaredUpperBoundConstraintPosition<*> && !it.type.typeConstructor().isTypeVariable()

                    addNewIncorporatedConstraint(
                        constraint.type,
                        it.type,
                        shouldBeTypeVariableFlexible,
                        isFromDeclaredUpperBound = isFromDeclaredUpperBound
                    )
                }
            }
        }
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
        constraint: Constraint
    ) {
        val freshTypeConstructor = typeVariable.freshTypeConstructor()
        for (typeVariableWithConstraint in this@insideOtherConstraint.allTypeVariablesWithConstraints) {
            val constraintsWhichConstraintMyVariable = typeVariableWithConstraint.constraints.filter {
                containsTypeVariable(it.type, freshTypeConstructor)
            }
            constraintsWhichConstraintMyVariable.forEach {
                generateNewConstraint(typeVariableWithConstraint.typeVariable, it, typeVariable, constraint)
            }
        }
    }

    private fun Context.approximateIfNeededAndAddNewConstraint(
        baseConstraint: Constraint,
        type: KotlinTypeMarker,
        targetVariable: TypeVariableMarker,
        otherVariable: TypeVariableMarker,
        otherConstraint: Constraint,
        needApproximation: Boolean = true
    ) {
        val typeWithSubstitution = baseConstraint.type.substitute(this, otherVariable, type)
        val prepareType = { toSuper: Boolean ->
            if (needApproximation) approximateCapturedTypes(typeWithSubstitution, toSuper) else typeWithSubstitution
        }

        if (baseConstraint.kind != ConstraintKind.LOWER) {
            addNewConstraint(targetVariable, baseConstraint, otherVariable, otherConstraint, prepareType(true), isSubtype = false)
        }
        if (baseConstraint.kind != ConstraintKind.UPPER) {
            addNewConstraint(targetVariable, baseConstraint, otherVariable, otherConstraint, prepareType(false), isSubtype = true)
        }
    }

    private fun Context.generateNewConstraint(
        targetVariable: TypeVariableMarker,
        baseConstraint: Constraint,
        otherVariable: TypeVariableMarker,
        otherConstraint: Constraint
    ) {
        val isBaseGenericType = baseConstraint.type.argumentsCount() != 0
        val isBaseOrOtherCapturedType = baseConstraint.type.isCapturedType() || otherConstraint.type.isCapturedType()
        val (type, needApproximation) = when (otherConstraint.kind) {
            ConstraintKind.EQUALITY -> {
                otherConstraint.type to false
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
                if (baseConstraint.kind == ConstraintKind.LOWER && !isBaseGenericType && !isBaseOrOtherCapturedType) {
                    nothingType() to false
                } else if (baseConstraint.kind == ConstraintKind.UPPER && !isBaseGenericType && !isBaseOrOtherCapturedType) {
                    otherConstraint.type to false
                } else {
                    createCapturedType(
                        createTypeArgument(otherConstraint.type, TypeVariance.OUT),
                        listOf(otherConstraint.type),
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
                if (baseConstraint.kind == ConstraintKind.UPPER && !isBaseGenericType && !isBaseOrOtherCapturedType) {
                    nullableAnyType() to false
                } else if (baseConstraint.kind == ConstraintKind.LOWER && !isBaseGenericType && !isBaseOrOtherCapturedType) {
                    otherConstraint.type to false
                } else {
                    createCapturedType(
                        createTypeArgument(otherConstraint.type, TypeVariance.IN),
                        emptyList(),
                        otherConstraint.type,
                        CaptureStatus.FOR_INCORPORATION
                    ) to true
                }
            }
        }

        approximateIfNeededAndAddNewConstraint(baseConstraint, type, targetVariable, otherVariable, otherConstraint, needApproximation)
    }

    private fun Context.addNewConstraint(
        targetVariable: TypeVariableMarker,
        baseConstraint: Constraint,
        otherVariable: TypeVariableMarker,
        otherConstraint: Constraint,
        newConstraint: KotlinTypeMarker,
        isSubtype: Boolean
    ) {
        if (targetVariable in getNestedTypeVariables(newConstraint)) return

        val isUsefulForNullabilityConstraint =
            isPotentialUsefulNullabilityConstraint(newConstraint, otherConstraint.type, otherConstraint.kind)
        val isFromVariableFixation = baseConstraint.position.from is FixVariableConstraintPosition<*>
                || otherConstraint.position.from is FixVariableConstraintPosition<*>

        if (!otherConstraint.kind.isEqual() &&
            !isUsefulForNullabilityConstraint &&
            !isFromVariableFixation &&
            !containsConstrainingTypeWithoutProjection(newConstraint, otherConstraint)
        ) return

        if (trivialConstraintTypeInferenceOracle.isGeneratedConstraintTrivial(
                baseConstraint, otherConstraint, newConstraint, isSubtype
            )
        ) return

        val derivedFrom = SmartSet.create(baseConstraint.derivedFrom).also { it.addAll(otherConstraint.derivedFrom) }
        if (otherVariable in derivedFrom) return

        derivedFrom.add(otherVariable)

        val kind = if (isSubtype) ConstraintKind.LOWER else ConstraintKind.UPPER

        val inputTypePosition =
            baseConstraint.position.from as? OnlyInputTypeConstraintPosition ?: baseConstraint.inputTypePositionBeforeIncorporation

        val isNewConstraintUsefulForNullability = isUsefulForNullabilityConstraint && newConstraint.isNullableNothing()
        val isOtherConstraintUsefulForNullability = otherConstraint.isNullabilityConstraint && otherConstraint.type.isNullableNothing()
        val isNullabilityConstraint = isNewConstraintUsefulForNullability || isOtherConstraintUsefulForNullability

        val constraintContext = ConstraintContext(kind, derivedFrom, inputTypePosition, isNullabilityConstraint)

        addNewIncorporatedConstraint(targetVariable, newConstraint, constraintContext)
    }

    private fun Context.containsConstrainingTypeWithoutProjection(
        newConstraint: KotlinTypeMarker,
        otherConstraint: Constraint
    ): Boolean {
        return getNestedArguments(newConstraint).any {
            it.getType().typeConstructor() == otherConstraint.type.typeConstructor() && it.getVariance() == TypeVariance.INV
        }
    }

    private fun Context.isPotentialUsefulNullabilityConstraint(
        newConstraint: KotlinTypeMarker,
        otherConstraint: KotlinTypeMarker,
        kind: ConstraintKind
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
            getTypeVariable(it.getType().typeConstructor().unwrapStubTypeVariableConstructor())
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
        if (typeProjection.isStarProjection()) continue

        result.add(typeProjection)

        when (val projectedType = typeProjection.getType()) {
            is FlexibleTypeMarker -> {
                addArgumentsToStack(projectedType.lowerBound())
                addArgumentsToStack(projectedType.upperBound())
            }
            else -> addArgumentsToStack(projectedType)
        }
    }
    return result
}
