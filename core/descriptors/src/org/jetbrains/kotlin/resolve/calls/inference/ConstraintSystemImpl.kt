/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.TypeUtils.DONT_CARE
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemImpl.ConstraintKind
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedure
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedureCallbacks
import org.jetbrains.kotlin.types.TypeConstructor
import java.util.LinkedHashMap
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.*
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemImpl.ConstraintKind.*
import java.util.HashMap
import java.util.ArrayList
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.*
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.CompoundConstraintPosition
import org.jetbrains.kotlin.types.getCustomTypeVariable
import org.jetbrains.kotlin.types.isFlexible
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.Bound
import org.jetbrains.kotlin.types.TypeSubstitution

public class ConstraintSystemImpl : ConstraintSystem {

    public enum class ConstraintKind {
        SUB_TYPE
        EQUAL
    }

    private val typeParameterBounds = LinkedHashMap<TypeParameterDescriptor, TypeBoundsImpl>()
    private val errors = ArrayList<ConstraintError>()

    private val constraintSystemStatus = object : ConstraintSystemStatus {
        // for debug ConstraintsUtil.getDebugMessageForStatus might be used

        override fun isSuccessful() = !hasContradiction() && !hasUnknownParameters()

        override fun hasContradiction() = hasTypeConstructorMismatch() || hasConflictingConstraints() || hasCannotCaptureTypesError()

        override fun hasViolatedUpperBound() = !isSuccessful() && getSystemWithoutWeakConstraints().getStatus().isSuccessful()

        override fun hasConflictingConstraints() = typeParameterBounds.values().any { it.getValues().size() > 1 }

        override fun hasUnknownParameters() = typeParameterBounds.values().any { it.isEmpty() }

        override fun hasTypeConstructorMismatch() = errors.any { it is TypeConstructorMismatch }

        override fun hasTypeConstructorMismatchAt(constraintPosition: ConstraintPosition) =
                errors.any { it is TypeConstructorMismatch && it.constraintPosition == constraintPosition }

        override fun hasOnlyErrorsFromPosition(constraintPosition: ConstraintPosition): Boolean {
            if (isSuccessful()) return false
            if (filterConstraintsOut(constraintPosition).getStatus().isSuccessful()) return true
            return errors.isNotEmpty() && errors.all { it.constraintPosition == constraintPosition }
        }

        override fun hasErrorInConstrainingTypes() = errors.any { it is ErrorInConstrainingType }

        override fun hasCannotCaptureTypesError() = errors.any { it is CannotCapture }
    }

    private fun getParameterToInferredValueMap(
            typeParameterBounds: Map<TypeParameterDescriptor, TypeBoundsImpl>,
            getDefaultTypeProjection: (TypeParameterDescriptor) -> TypeProjection
    ): Map<TypeParameterDescriptor, TypeProjection> {
        val substitutionContext = HashMap<TypeParameterDescriptor, TypeProjection>()
        for ((typeParameter, typeBounds) in typeParameterBounds) {
            val typeProjection: TypeProjection
            val value = typeBounds.getValue()
            if (value != null && !TypeUtils.containsSpecialType(value, DONT_CARE)) {
                typeProjection = TypeProjectionImpl(value)
            }
            else {
                typeProjection = getDefaultTypeProjection(typeParameter)
            }
            substitutionContext.put(typeParameter, typeProjection)
        }
        return substitutionContext
    }

    private fun replaceUninferredBy(getDefaultValue: (TypeParameterDescriptor) -> TypeProjection): TypeSubstitutor {
        return TypeUtils.makeSubstitutorForTypeParametersMap(getParameterToInferredValueMap(typeParameterBounds, getDefaultValue))
    }

    private fun replaceUninferredBy(defaultValue: JetType): TypeSubstitutor {
        return replaceUninferredBy { TypeProjectionImpl(defaultValue) }
    }

    private fun replaceUninferredBySpecialErrorType(): TypeSubstitutor {
        return replaceUninferredBy { TypeProjectionImpl(ErrorUtils.createUninferredParameterType(it)) }
    }

    override fun getStatus(): ConstraintSystemStatus = constraintSystemStatus

    override fun registerTypeVariables(typeVariables: Map<TypeParameterDescriptor, Variance>) {
        for ((typeVariable, positionVariance) in typeVariables) {
            typeParameterBounds.put(typeVariable, TypeBoundsImpl(typeVariable, positionVariance))
        }
        val constantSubstitutor = TypeUtils.makeConstantSubstitutor(typeParameterBounds.keySet(), DONT_CARE)
        for ((typeVariable, typeBounds) in typeParameterBounds) {
            for (declaredUpperBound in typeVariable.getUpperBounds()) {
                if (KotlinBuiltIns.getInstance().getNullableAnyType() == declaredUpperBound) continue //todo remove this line (?)
                val substitutedBound = constantSubstitutor?.substitute(declaredUpperBound, Variance.INVARIANT)
                val position = TYPE_BOUND_POSITION.position(typeVariable.getIndex())
                if (substitutedBound != null && !isErrorOrSpecialType(substitutedBound, position)) {
                    typeBounds.addBound(UPPER_BOUND, substitutedBound, position)
                }
            }
        }
    }

    public fun copy(): ConstraintSystem = createNewConstraintSystemFromThis({ it }, { it.copy() }, { true })

    public fun substituteTypeVariables(typeVariablesMap: (TypeParameterDescriptor) -> TypeParameterDescriptor?): ConstraintSystem {
        // type bounds are proper types and don't contain other variables
        return createNewConstraintSystemFromThis(typeVariablesMap, { it }, { true })
    }

    public fun filterConstraintsOut(vararg excludePositions: ConstraintPosition): ConstraintSystem {
        val positions = excludePositions.toSet()
        return filterConstraints { !positions.contains(it) }
    }

    public fun filterConstraints(condition: (ConstraintPosition) -> Boolean): ConstraintSystem {
        return createNewConstraintSystemFromThis({ it }, { it.filter(condition) }, condition)
    }

    public fun getSystemWithoutWeakConstraints(): ConstraintSystem {
        return filterConstraints {
            constraintPosition ->
            // 'isStrong' for compound means 'has some strong constraints'
            // but for testing absence of weak constraints we need 'has only strong constraints' here
            if (constraintPosition is CompoundConstraintPosition) {
                constraintPosition.positions.all { it.isStrong() }
            }
            else {
                constraintPosition.isStrong()
            }
        }
    }

    private fun createNewConstraintSystemFromThis(
            substituteTypeVariable: (TypeParameterDescriptor) -> TypeParameterDescriptor?,
            replaceTypeBounds: (TypeBoundsImpl) -> TypeBoundsImpl,
            filterConstraintPosition: (ConstraintPosition) -> Boolean
    ): ConstraintSystem {
        val newSystem = ConstraintSystemImpl()
        for ((typeParameter, typeBounds) in typeParameterBounds) {
            val newTypeParameter = substituteTypeVariable(typeParameter)
            newSystem.typeParameterBounds.put(newTypeParameter!!, replaceTypeBounds(typeBounds))
        }
        newSystem.errors.addAll(errors.filter { filterConstraintPosition(it.constraintPosition) })
        return newSystem
    }

    override fun addSupertypeConstraint(constrainingType: JetType?, subjectType: JetType, constraintPosition: ConstraintPosition) {
        if (constrainingType != null && TypeUtils.noExpectedType(constrainingType)) return

        addConstraint(SUB_TYPE, subjectType, constrainingType, constraintPosition)
    }

    override fun addSubtypeConstraint(constrainingType: JetType?, subjectType: JetType, constraintPosition: ConstraintPosition) {
        addConstraint(SUB_TYPE, constrainingType, subjectType, constraintPosition)
    }

    private fun addConstraint(constraintKind: ConstraintKind, subType: JetType?, superType: JetType?, constraintPosition: ConstraintPosition) {
        val typeCheckingProcedure = TypeCheckingProcedure(object : TypeCheckingProcedureCallbacks {
            private var isTopLevel = true

            override fun assertEqualTypes(a: JetType, b: JetType, typeCheckingProcedure: TypeCheckingProcedure): Boolean {
                isTopLevel = false
                doAddConstraint(EQUAL, a, b, constraintPosition, typeCheckingProcedure)
                return true

            }

            override fun assertEqualTypeConstructors(a: TypeConstructor, b: TypeConstructor): Boolean {
                return a == b
            }

            override fun assertSubtype(subtype: JetType, supertype: JetType, typeCheckingProcedure: TypeCheckingProcedure): Boolean {
                isTopLevel = false
                doAddConstraint(SUB_TYPE, subtype, supertype, constraintPosition, typeCheckingProcedure)
                return true
            }

            override fun capture(typeVariable: JetType, typeProjection: TypeProjection): Boolean {
                val myTypeVariable = getMyTypeVariable(typeVariable)
                if (myTypeVariable != null && constraintPosition.isCaptureAllowed()) {
                    if (!isTopLevel) {
                        errors.add(CannotCapture(constraintPosition, myTypeVariable))
                    }
                    generateTypeParameterCaptureConstraint(typeVariable, typeProjection, constraintPosition)
                    return true
                }
                return false
            }

            override fun noCorrespondingSupertype(subtype: JetType, supertype: JetType): Boolean {
                errors.add(TypeConstructorMismatch(constraintPosition))
                return true
            }
        })
        doAddConstraint(constraintKind, subType, superType, constraintPosition, typeCheckingProcedure)
    }

    private fun isErrorOrSpecialType(type: JetType?, constraintPosition: ConstraintPosition): Boolean {
        if (TypeUtils.isDontCarePlaceholder(type) || ErrorUtils.isUninferredParameter(type)) {
            return true
        }

        if (type == null || (type.isError() && type != TypeUtils.PLACEHOLDER_FUNCTION_TYPE)) {
            errors.add(ErrorInConstrainingType(constraintPosition))
            return true
        }
        return false
    }

    private fun doAddConstraint(
            constraintKind: ConstraintKind,
            subType: JetType?,
            superType: JetType?,
            constraintPosition: ConstraintPosition,
            typeCheckingProcedure: TypeCheckingProcedure
    ) {
        if (isErrorOrSpecialType(subType, constraintPosition) || isErrorOrSpecialType(superType, constraintPosition)) return
        if (subType == null || superType == null) return

        assert(superType != TypeUtils.PLACEHOLDER_FUNCTION_TYPE) {
            "The type for " + constraintPosition + " shouldn't be a placeholder for function type"
        }

        if (subType == TypeUtils.PLACEHOLDER_FUNCTION_TYPE) {
            if (!KotlinBuiltIns.isFunctionOrExtensionFunctionType(superType)) {
                if (isMyTypeVariable(superType)) {
                    // a constraint binds type parameter and any function type, so there is no new info and no error
                    return
                }
                errors.add(TypeConstructorMismatch(constraintPosition))
            }
            return
        }

        // todo temporary hack
        // function literal without declaring receiver type { x -> ... }
        // can be considered as extension function if one is expected
        // (special type constructor for function/ extension function should be introduced like PLACEHOLDER_FUNCTION_TYPE)
        val newSubType = if (constraintKind == SUB_TYPE
                && KotlinBuiltIns.isFunctionType(subType)
                && KotlinBuiltIns.isExtensionFunctionType(superType)) {
            createCorrespondingExtensionFunctionType(subType, DONT_CARE)
        }
        else {
            subType : JetType
        }

        fun simplifyConstraint(subType: JetType, superType: JetType) {
            // can be equal for the recursive invocations: fun <T> foo(i: Int) : T { ... return foo(i); } => T <: T
            // the right processing of constraints connecting type variables is not supported yet
            if (isMyTypeVariable(subType) && isMyTypeVariable(superType)) return

            if (isMyTypeVariable(subType)) {
                val boundKind = if (constraintKind == SUB_TYPE) UPPER_BOUND else EXACT_BOUND
                generateTypeParameterConstraint(subType, superType, boundKind, constraintPosition)
                return
            }
            if (isMyTypeVariable(superType)) {
                val boundKind = if (constraintKind == SUB_TYPE) LOWER_BOUND else EXACT_BOUND
                generateTypeParameterConstraint(superType, subType, boundKind, constraintPosition)
                return
            }
            // if superType is nullable and subType is not nullable, unsafe call or type mismatch error will be generated later,
            // but constraint system should be solved anyway
            val subTypeNotNullable = TypeUtils.makeNotNullable(subType)
            val superTypeNotNullable = TypeUtils.makeNotNullable(superType)
            if (constraintKind == EQUAL) {
                typeCheckingProcedure.equalTypes(subTypeNotNullable, superTypeNotNullable)
            }
            else {
                typeCheckingProcedure.isSubtypeOf(subTypeNotNullable, superTypeNotNullable)
            }
        }
        simplifyConstraint(newSubType, superType)
    }

    private fun generateTypeParameterConstraint(
            parameterType: JetType,
            constrainingType: JetType,
            boundKind: TypeBounds.BoundKind,
            constraintPosition: ConstraintPosition
    ) {
        var newConstrainingType = constrainingType

        // Here we are handling the case when T! gets a bound Foo (or Foo?)
        // In this case, type parameter T is supposed to get the bound Foo!
        // Example:
        // val c: Collection<Foo> = Collections.singleton(null : Foo?)
        // Constraints for T are:
        //   Foo? <: T!
        //   Foo >: T!
        // both Foo and Foo? transform to Foo! here
        if (parameterType.isFlexible()) {
            val typeVariable = parameterType.getCustomTypeVariable()
            if (typeVariable != null) {
                newConstrainingType = typeVariable.substitutionResult(constrainingType)
            }
        }

        val typeBounds = getTypeBounds(parameterType)

        if (!parameterType.isMarkedNullable() || !newConstrainingType.isMarkedNullable()) {
            typeBounds.addBound(boundKind, newConstrainingType, constraintPosition)
            return
        }
        // For parameter type T:
        // constraint T? =  Int? should transform to T >: Int and T <: Int?
        // constraint T? >: Int? should transform to T >: Int
        val notNullConstrainingType = TypeUtils.makeNotNullable(newConstrainingType)
        if (boundKind == EXACT_BOUND || boundKind == LOWER_BOUND) {
            typeBounds.addBound(LOWER_BOUND, notNullConstrainingType, constraintPosition)
        }
        // constraint T? <: Int? should transform to T <: Int?
        if (boundKind == EXACT_BOUND || boundKind == UPPER_BOUND) {
            typeBounds.addBound(UPPER_BOUND, newConstrainingType, constraintPosition)
        }
    }

    private fun generateTypeParameterCaptureConstraint(
            parameterType: JetType,
            constrainingTypeProjection: TypeProjection,
            constraintPosition: ConstraintPosition
    ) {
        val typeVariable = getMyTypeVariable(parameterType)!!
        if (!KotlinBuiltIns.isNullableAny(typeVariable.getUpperBoundsAsType())
            && constrainingTypeProjection.getProjectionKind() == Variance.IN_VARIANCE) {
            errors.add(CannotCapture(constraintPosition, typeVariable))
        }
        val typeBounds = getTypeBounds(typeVariable)
        val typeProjection = if (parameterType.isMarkedNullable()) {
            TypeProjectionImpl(constrainingTypeProjection.getProjectionKind(), TypeUtils.makeNotNullable(constrainingTypeProjection.getType()))
        }
        else {
            constrainingTypeProjection
        }
        val capturedType = createCapturedType(typeProjection)
        typeBounds.addBound(EXACT_BOUND, capturedType, constraintPosition)
    }

    public fun processDeclaredBoundConstraints() {
        for ((typeParameterDescriptor, typeBounds) in typeParameterBounds) {
            fun compoundPosition(bound: Bound) = CompoundConstraintPosition(
                    TYPE_BOUND_POSITION.position(typeParameterDescriptor.getIndex()), bound.position)

            // todo order matters here
            // it's important to create a separate variable here,
            // because the following code may add new elements to typeBounds.bounds collection
            val bounds = ArrayList(typeBounds.bounds)
            for (declaredUpperBound in typeParameterDescriptor.getUpperBounds()) {
                bounds.filter { it.kind != UPPER_BOUND }.forEach {
                    lowerOrExactBound ->
                    addSubtypeConstraint(lowerOrExactBound.constrainingType, declaredUpperBound, compoundPosition(lowerOrExactBound))
                }
                if (!isMyTypeVariable(declaredUpperBound)) continue
                getTypeBounds(declaredUpperBound).bounds.filter { it.kind != LOWER_BOUND }.forEach {
                    upperOrExactBound ->
                    typeBounds.addBound(UPPER_BOUND, upperOrExactBound.constrainingType, compoundPosition(upperOrExactBound))
                }
            }
        }
    }

    override fun getTypeVariables() = typeParameterBounds.keySet()

    override fun getTypeBounds(typeVariable: TypeParameterDescriptor): TypeBoundsImpl {
        if (!isMyTypeVariable(typeVariable)) {
            throw IllegalArgumentException("TypeParameterDescriptor is not a type variable for constraint system: $typeVariable")
        }
        return typeParameterBounds[typeVariable]!!
    }

    private fun getTypeBounds(parameterType: JetType): TypeBoundsImpl {
        assert (isMyTypeVariable(parameterType)) { "Type is not a type variable for constraint system: $parameterType" }
        return getTypeBounds(getMyTypeVariable(parameterType)!!)
    }

    private fun isMyTypeVariable(typeVariable: TypeParameterDescriptor) = typeParameterBounds.contains(typeVariable)

    private fun isMyTypeVariable(type: JetType): Boolean = getMyTypeVariable(type) != null

    private fun getMyTypeVariable(type: JetType): TypeParameterDescriptor? {
        val typeParameterDescriptor = type.getConstructor().getDeclarationDescriptor() as? TypeParameterDescriptor
        return if (typeParameterDescriptor != null && isMyTypeVariable(typeParameterDescriptor)) typeParameterDescriptor else null
    }

    override fun getResultingSubstitutor() = replaceUninferredBySpecialErrorType().setApproximateCapturedTypes()

    override fun getCurrentSubstitutor() = replaceUninferredBy(TypeUtils.DONT_CARE).setApproximateCapturedTypes()

    private fun createCorrespondingExtensionFunctionType(functionType: JetType, receiverType: JetType): JetType {
        assert(KotlinBuiltIns.isFunctionType(functionType))

        val typeArguments = functionType.getArguments()
        assert(!typeArguments.isEmpty())

        val arguments = ArrayList<JetType>()
        // excluding the last type argument of the function type, which is the return type
        var index = 0
        val lastIndex = typeArguments.size() - 1
        for (typeArgument in typeArguments) {
            if (index < lastIndex) {
                arguments.add(typeArgument.getType())
            }
            index++
        }
        val returnType = typeArguments.get(lastIndex).getType()
        return KotlinBuiltIns.getInstance().getFunctionType(functionType.getAnnotations(), receiverType, arguments, returnType)
    }
}

private fun TypeSubstitutor.setApproximateCapturedTypes(): TypeSubstitutor {
    return TypeSubstitutor.create(SubstitutionWithCapturedTypeApproximation(getSubstitution()))
}

private class SubstitutionWithCapturedTypeApproximation(val substitution: TypeSubstitution) : TypeSubstitution() {
    override fun get(key: TypeConstructor?) = substitution[key]
    override fun isEmpty() = substitution.isEmpty()
    override fun approximateCapturedTypes() = true
}
