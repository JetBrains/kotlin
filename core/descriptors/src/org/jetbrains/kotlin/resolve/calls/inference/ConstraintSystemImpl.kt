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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemImpl.ConstraintKind.EQUAL
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemImpl.ConstraintKind.SUB_TYPE
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.Bound
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.EXACT_BOUND
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.LOWER_BOUND
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.UPPER_BOUND
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.CompoundConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.TYPE_BOUND_POSITION
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.derivedFrom
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.ErrorUtils.FunctionPlaceholderTypeConstructor
import org.jetbrains.kotlin.types.TypeUtils.DONT_CARE
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedure
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedureCallbacks
import org.jetbrains.kotlin.types.typeUtil.getNestedArguments
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedHashMap

public class ConstraintSystemImpl : ConstraintSystem {

    data class Constraint(val kind: ConstraintKind, val subtype: JetType, val superType: JetType, val position: ConstraintPosition)

    public enum class ConstraintKind {
        SUB_TYPE,
        EQUAL
    }

    fun ConstraintKind.toBound() = if (this == SUB_TYPE) UPPER_BOUND else EXACT_BOUND

    private val allTypeParameterBounds = LinkedHashMap<TypeParameterDescriptor, TypeBoundsImpl>()
    private val externalTypeParameters = HashSet<TypeParameterDescriptor>()
    private val localTypeParameterBounds: Map<TypeParameterDescriptor, TypeBoundsImpl>
        get() = if (externalTypeParameters.isEmpty()) allTypeParameterBounds
            else allTypeParameterBounds.filter { !externalTypeParameters.contains(it.key) }

    private val cachedTypeForVariable = HashMap<TypeParameterDescriptor, JetType>()

    private val usedInBounds = HashMap<TypeParameterDescriptor, MutableList<TypeBounds.Bound>>()

    private val errors = ArrayList<ConstraintError>()
    public val constraintErrors: List<ConstraintError>
        get() = errors

    private val initialConstraints = ArrayList<Constraint>()

    private val originalToVariablesSubstitutor: TypeSubstitutor by lazy {
        createTypeSubstitutor { originalToVariables[it] }
    }
    private val originalToVariables = LinkedHashMap<TypeParameterDescriptor, TypeParameterDescriptor>()
    private val variablesToOriginal = LinkedHashMap<TypeParameterDescriptor, TypeParameterDescriptor>()

    private val constraintSystemStatus = object : ConstraintSystemStatus {
        // for debug ConstraintsUtil.getDebugMessageForStatus might be used

        override fun isSuccessful() = !hasContradiction() && !hasUnknownParameters()

        override fun hasContradiction() = hasParameterConstraintError() || hasConflictingConstraints()
                                          || hasCannotCaptureTypesError() || hasTypeInferenceIncorporationError()

        override fun hasViolatedUpperBound() = !isSuccessful() && filterConstraintsOut(TYPE_BOUND_POSITION).getStatus().isSuccessful()

        override fun hasConflictingConstraints() = localTypeParameterBounds.values().any { it.values.size() > 1 }

        override fun hasUnknownParameters() = localTypeParameterBounds.values().any { it.values.isEmpty() }

        override fun hasParameterConstraintError() = errors.any { it is ParameterConstraintError }

        override fun hasOnlyErrorsDerivedFrom(kind: ConstraintPositionKind): Boolean {
            if (isSuccessful()) return false
            if (filterConstraintsOut(kind).getStatus().isSuccessful()) return true
            return errors.isNotEmpty() && errors.all { it.constraintPosition.derivedFrom(kind) }
        }

        override fun hasErrorInConstrainingTypes() = errors.any { it is ErrorInConstrainingType }

        override fun hasCannotCaptureTypesError() = errors.any { it is CannotCapture }

        override fun hasTypeInferenceIncorporationError() = errors.any { it is TypeInferenceError } || !satisfyInitialConstraints()
    }

    private fun getParameterToInferredValueMap(
            typeParameterBounds: Map<TypeParameterDescriptor, TypeBoundsImpl>,
            getDefaultTypeProjection: (TypeParameterDescriptor) -> TypeProjection,
            substituteOriginal: Boolean
    ): Map<TypeParameterDescriptor, TypeProjection> {
        val substitutionContext = HashMap<TypeParameterDescriptor, TypeProjection>()
        for ((typeParameter, typeBounds) in typeParameterBounds) {
            val typeProjection: TypeProjection
            val value = typeBounds.value
            if (value != null && !TypeUtils.containsSpecialType(value, DONT_CARE)) {
                typeProjection = TypeProjectionImpl(value)
            }
            else {
                typeProjection = getDefaultTypeProjection(typeParameter)
            }
            substitutionContext.put(if (substituteOriginal) variablesToOriginal[typeParameter]!! else typeParameter, typeProjection)
        }
        return substitutionContext
    }

    private fun replaceUninferredBy(
            getDefaultValue: (TypeParameterDescriptor) -> TypeProjection,
            substituteOriginal: Boolean
    ): TypeSubstitutor {
        val parameterToInferredValueMap = getParameterToInferredValueMap(allTypeParameterBounds, getDefaultValue, substituteOriginal)
        return TypeUtils.makeSubstitutorForTypeParametersMap(parameterToInferredValueMap)
    }

    override fun getStatus(): ConstraintSystemStatus = constraintSystemStatus

    override fun registerTypeVariables(
            typeVariables: Collection<TypeParameterDescriptor>,
            variance: (TypeParameterDescriptor) -> Variance,
            mapToOriginal: (TypeParameterDescriptor) -> TypeParameterDescriptor,
            external: Boolean
    ) {
        if (external) externalTypeParameters.addAll(typeVariables)

        for (typeVariable in typeVariables) {
            allTypeParameterBounds.put(typeVariable, TypeBoundsImpl(typeVariable, variance(typeVariable)))
            val original = mapToOriginal(typeVariable)
            originalToVariables[original] = typeVariable
            variablesToOriginal[typeVariable] = original
        }
        for ((typeVariable, typeBounds) in allTypeParameterBounds) {
            for (declaredUpperBound in typeVariable.getUpperBounds()) {
                if (KotlinBuiltIns.getInstance().getNullableAnyType() == declaredUpperBound) continue //todo remove this line (?)
                val position = TYPE_BOUND_POSITION.position(typeVariable.getIndex())
                addBound(typeVariable, declaredUpperBound, UPPER_BOUND, position)
            }
        }
    }

    val TypeParameterDescriptor.correspondingType: JetType
        get() = cachedTypeForVariable.getOrPut(this) {
            JetTypeImpl(Annotations.EMPTY, this.getTypeConstructor(), false, listOf(), JetScope.Empty)
        }

    fun JetType.isProper() = !TypeUtils.containsSpecialType(this) {
        type -> type.getConstructor().getDeclarationDescriptor() in getAllTypeVariables()
    }

    fun JetType.getNestedTypeVariables(original: Boolean = true): List<TypeParameterDescriptor> {
        return getNestedArguments().map { typeProjection ->
            typeProjection.getType().getConstructor().getDeclarationDescriptor() as? TypeParameterDescriptor
        }.filterNotNull().filter { if (original) it in originalToVariables.keySet() else it in getAllTypeVariables() }
    }

    public fun copy(): ConstraintSystem = createNewConstraintSystemFromThis { true }

    public fun filterConstraintsOut(excludePositionKind: ConstraintPositionKind): ConstraintSystem {
        return createNewConstraintSystemFromThis { !it.derivedFrom(excludePositionKind) }
    }

    private fun createNewConstraintSystemFromThis(
            filterConstraintPosition: (ConstraintPosition) -> Boolean
    ): ConstraintSystem {
        val newSystem = ConstraintSystemImpl()
        for ((typeParameter, typeBounds) in allTypeParameterBounds) {
            newSystem.allTypeParameterBounds.put(typeParameter, typeBounds.filter(filterConstraintPosition))
        }
        newSystem.usedInBounds.putAll(usedInBounds.map {
            val (variable, bounds) = it
            variable to bounds.filterTo(arrayListOf<Bound>()) { filterConstraintPosition(it.position )}
        }.toMap())
        newSystem.externalTypeParameters.addAll(externalTypeParameters )
        newSystem.errors.addAll(errors.filter { filterConstraintPosition(it.constraintPosition) })

        newSystem.initialConstraints.addAll(initialConstraints.filter { filterConstraintPosition(it.position) })
        newSystem.originalToVariables.putAll(originalToVariables)
        newSystem.variablesToOriginal.putAll(variablesToOriginal)
        return newSystem
    }

    override fun addSupertypeConstraint(constrainingType: JetType?, subjectType: JetType, constraintPosition: ConstraintPosition) {
        if (constrainingType != null && TypeUtils.noExpectedType(constrainingType)) return

        val newSubjectType = originalToVariablesSubstitutor.substitute(subjectType, Variance.INVARIANT)
        addConstraint(SUB_TYPE, newSubjectType, constrainingType, constraintPosition, topLevel = true)
    }

    override fun addSubtypeConstraint(constrainingType: JetType?, subjectType: JetType, constraintPosition: ConstraintPosition) {
        val newSubjectType = originalToVariablesSubstitutor.substitute(subjectType, Variance.INVARIANT)
        addConstraint(SUB_TYPE, constrainingType, newSubjectType, constraintPosition, topLevel = true)
    }

    fun addConstraint(
            constraintKind: ConstraintKind,
            subType: JetType?,
            superType: JetType?,
            constraintPosition: ConstraintPosition,
            topLevel: Boolean
    ) {
        val typeCheckingProcedure = TypeCheckingProcedure(object : TypeCheckingProcedureCallbacks {
            private var depth = 0

            override fun assertEqualTypes(a: JetType, b: JetType, typeCheckingProcedure: TypeCheckingProcedure): Boolean {
                depth++
                doAddConstraint(EQUAL, a, b, constraintPosition, typeCheckingProcedure, topLevel = false)
                depth--
                return true

            }

            override fun assertEqualTypeConstructors(a: TypeConstructor, b: TypeConstructor): Boolean {
                return a == b
            }

            override fun assertSubtype(subtype: JetType, supertype: JetType, typeCheckingProcedure: TypeCheckingProcedure): Boolean {
                depth++
                doAddConstraint(SUB_TYPE, subtype, supertype, constraintPosition, typeCheckingProcedure, topLevel = false)
                depth--
                return true
            }

            override fun capture(typeVariable: JetType, typeProjection: TypeProjection): Boolean {
                if (isMyTypeVariable(typeProjection.getType())) return false
                val myTypeVariable = getMyTypeVariable(typeVariable)

                if (myTypeVariable != null && constraintPosition.isParameter()) {
                    if (depth > 0) {
                        errors.add(CannotCapture(constraintPosition, myTypeVariable))
                    }
                    generateTypeParameterCaptureConstraint(typeVariable, typeProjection, constraintPosition)
                    return true
                }
                return false
            }

            override fun noCorrespondingSupertype(subtype: JetType, supertype: JetType): Boolean {
                errors.add(newTypeInferenceOrParameterConstraintError(constraintPosition))
                return true
            }
        })
        doAddConstraint(constraintKind, subType, superType, constraintPosition, typeCheckingProcedure, topLevel)
    }

    private fun isErrorOrSpecialType(type: JetType?, constraintPosition: ConstraintPosition): Boolean {
        if (TypeUtils.isDontCarePlaceholder(type) || ErrorUtils.isUninferredParameter(type)) {
            return true
        }

        if (type == null || (type.isError() && !ErrorUtils.isFunctionPlaceholder(type))) {
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
            typeCheckingProcedure: TypeCheckingProcedure,
            topLevel: Boolean
    ) {
        if (isErrorOrSpecialType(subType, constraintPosition) || isErrorOrSpecialType(superType, constraintPosition)) return
        if (subType == null || superType == null) return

        assert(!ErrorUtils.isFunctionPlaceholder(superType)) {
            "The type for " + constraintPosition + " shouldn't be a placeholder for function type"
        }

        // function literal { x -> ... } goes without declaring receiver type
        // and can be considered as extension function if one is expected
        val newSubType = if (constraintKind == SUB_TYPE && ErrorUtils.isFunctionPlaceholder(subType)) {
            if (isMyTypeVariable(superType)) {
                // the constraint binds type parameter and a function type,
                // we don't add it without knowing whether it's a function type or an extension function type
                return
            }
            createTypeForFunctionPlaceholder(subType, superType)
        }
        else {
            subType
        }

        fun simplifyConstraint(subType: JetType, superType: JetType) {
            if (isMyTypeVariable(subType)) {
                generateTypeParameterBound(subType, superType, constraintKind.toBound(), constraintPosition)
                return
            }
            if (isMyTypeVariable(superType)) {
                generateTypeParameterBound(superType, subType, constraintKind.toBound().reverse(), constraintPosition)
                return
            }
            // if subType is nullable and superType is not nullable, unsafe call or type mismatch error will be generated later,
            // but constraint system should be solved anyway
            val subTypeNotNullable = if (topLevel) TypeUtils.makeNotNullable(subType) else subType
            val superTypeNotNullable = if (topLevel) TypeUtils.makeNotNullable(superType) else superType
            val result = if (constraintKind == EQUAL) {
                typeCheckingProcedure.equalTypes(subTypeNotNullable, superTypeNotNullable)
            }
            else {
                typeCheckingProcedure.isSubtypeOf(subTypeNotNullable, superType)
            }
            if (!result) errors.add(newTypeInferenceOrParameterConstraintError(constraintPosition))
        }
        if (topLevel) {
            storeInitialConstraint(constraintKind, subType, superType, constraintPosition)
        }
        simplifyConstraint(newSubType, superType)
    }

    fun addBound(
            typeVariable: TypeParameterDescriptor,
            constrainingType: JetType,
            kind: TypeBounds.BoundKind,
            position: ConstraintPosition,
            derivedFrom: Set<TypeParameterDescriptor> = emptySet()
    ) {
        val bound = Bound(typeVariable, constrainingType, kind, position, constrainingType.isProper(), derivedFrom)
        val typeBounds = getTypeBounds(typeVariable)
        if (typeBounds.bounds.contains(bound)) return

        typeBounds.addBound(bound)

        if (!bound.isProper) {
            for (dependentTypeVariable in bound.constrainingType.getNestedTypeVariables(original = false)) {
                val dependentBounds = usedInBounds.getOrPut(dependentTypeVariable) { arrayListOf() }
                dependentBounds.add(bound)
            }
        }

        incorporateBound(bound)
    }

    private fun generateTypeParameterBound(
            parameterType: JetType,
            constrainingType: JetType,
            boundKind: TypeBounds.BoundKind,
            constraintPosition: ConstraintPosition
    ) {
        val typeVariable = getMyTypeVariable(parameterType)!!

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
            val customTypeVariable = parameterType.getCustomTypeVariable()
            if (customTypeVariable != null) {
                newConstrainingType = customTypeVariable.substitutionResult(constrainingType)
            }
        }

        if (!parameterType.isMarkedNullable() || !TypeUtils.isNullableType(newConstrainingType)) {
            addBound(typeVariable, newConstrainingType, boundKind, constraintPosition)
            return
        }
        // For parameter type T:
        // constraint T? =  Int? should transform to T >: Int and T <: Int?
        // constraint T? = Int! should transform to T >: Int and T <: Int!

        // constraints T? >: Int?; T? >: Int! should transform to T >: Int
        val notNullConstrainingType = TypeUtils.makeNotNullable(newConstrainingType)
        if (boundKind == EXACT_BOUND || boundKind == LOWER_BOUND) {
            addBound(typeVariable, notNullConstrainingType, LOWER_BOUND, constraintPosition)
        }
        // constraints T? <: Int?; T? <: Int! should transform to T <: Int?; T <: Int! correspondingly
        if (boundKind == EXACT_BOUND || boundKind == UPPER_BOUND) {
            addBound(typeVariable, newConstrainingType, UPPER_BOUND, constraintPosition)
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
        val typeProjection = if (parameterType.isMarkedNullable()) {
            TypeProjectionImpl(constrainingTypeProjection.getProjectionKind(), TypeUtils.makeNotNullable(constrainingTypeProjection.getType()))
        }
        else {
            constrainingTypeProjection
        }
        val capturedType = createCapturedType(typeProjection)
        addBound(typeVariable, capturedType, EXACT_BOUND, constraintPosition)
    }

    override fun getTypeVariables() = originalToVariables.keySet()

    fun getAllTypeVariables() = allTypeParameterBounds.keySet()

    fun getBoundsUsedIn(typeVariable: TypeParameterDescriptor): List<Bound> = usedInBounds[typeVariable] ?: emptyList()

    override fun getTypeBounds(typeVariable: TypeParameterDescriptor): TypeBoundsImpl {
        val variableForOriginal = originalToVariables[typeVariable]
        if (variableForOriginal != null && variableForOriginal != typeVariable) {
            return getTypeBounds(variableForOriginal)
        }
        if (!isMyTypeVariable(typeVariable)) {
            throw IllegalArgumentException("TypeParameterDescriptor is not a type variable for constraint system: $typeVariable")
        }
        return allTypeParameterBounds[typeVariable]!!
    }

    fun isMyTypeVariable(typeVariable: TypeParameterDescriptor) = allTypeParameterBounds.contains(typeVariable)

    fun isMyTypeVariable(type: JetType): Boolean = getMyTypeVariable(type) != null

    fun getMyTypeVariable(type: JetType): TypeParameterDescriptor? {
        val typeParameterDescriptor = type.getConstructor().getDeclarationDescriptor() as? TypeParameterDescriptor
        return if (typeParameterDescriptor != null && isMyTypeVariable(typeParameterDescriptor)) typeParameterDescriptor else null
    }

    override fun getResultingSubstitutor() =
            getSubstitutor(substituteOriginal = true) { TypeProjectionImpl(ErrorUtils.createUninferredParameterType(it)) }

    override fun getCurrentSubstitutor() =
            getSubstitutor(substituteOriginal = true) { TypeProjectionImpl(TypeUtils.DONT_CARE) }

    private fun getSubstitutor(substituteOriginal: Boolean, getDefaultValue: (TypeParameterDescriptor) -> TypeProjection) =
            replaceUninferredBy(getDefaultValue, substituteOriginal).setApproximateCapturedTypes()

    private fun storeInitialConstraint(constraintKind: ConstraintKind, subType: JetType, superType: JetType, position: ConstraintPosition) {
        initialConstraints.add(Constraint(constraintKind, subType, superType, position))
    }

    private fun satisfyInitialConstraints(): Boolean {
        fun JetType.substituteAndMakeNotNullable(): JetType? {
            val substitutor = getSubstitutor(substituteOriginal = false) { TypeProjectionImpl(ErrorUtils.createUninferredParameterType(it)) }
            val result = substitutor.substitute(this, Variance.INVARIANT) ?: return null
            return TypeUtils.makeNotNullable(result)
        }
        return initialConstraints.all {
            val resultSubType = it.subtype.substituteAndMakeNotNullable() ?: return false
            val resultSuperType = it.superType.substituteAndMakeNotNullable() ?: return false
            when (it.kind) {
                SUB_TYPE -> JetTypeChecker.DEFAULT.isSubtypeOf(resultSubType, resultSuperType)
                EQUAL -> JetTypeChecker.DEFAULT.equalTypes(resultSubType, resultSuperType)
            }
        }
    }

    fun fixVariable(typeVariable: TypeParameterDescriptor) {
        val typeBounds = getTypeBounds(typeVariable)
        if (typeBounds.isFixed) return
        typeBounds.setFixed()

        val nestedTypeVariables = typeBounds.bounds.flatMap { it.constrainingType.getNestedTypeVariables(original = false) }
        nestedTypeVariables.forEach { fixVariable(it) }

        val value = typeBounds.value ?: return

        addBound(typeVariable, value, TypeBounds.BoundKind.EXACT_BOUND, ConstraintPositionKind.FROM_COMPLETER.position())
    }

    fun fixVariables() {
        // todo variables should be fixed in the right order
        val (external, functionTypeParameters) = getAllTypeVariables().partition { externalTypeParameters.contains(it) }
        external.forEach { fixVariable(it) }
        functionTypeParameters.forEach { fixVariable(it) }
    }

}

fun createTypeForFunctionPlaceholder(
        functionPlaceholder: JetType,
        expectedType: JetType
): JetType {
    if (!ErrorUtils.isFunctionPlaceholder(functionPlaceholder)) return functionPlaceholder

    val functionPlaceholderTypeConstructor = functionPlaceholder.getConstructor() as FunctionPlaceholderTypeConstructor

    val isExtension = KotlinBuiltIns.isExtensionFunctionType(expectedType)
    val newArgumentTypes = if (!functionPlaceholderTypeConstructor.hasDeclaredArguments()) {
        val typeParamSize = expectedType.getConstructor().getParameters().size()
        // the first parameter is receiver (if present), the last one is return type,
        // the remaining are function arguments
        val functionArgumentsSize = if (isExtension) typeParamSize - 2 else typeParamSize - 1
        val result = arrayListOf<JetType>()
        (1..functionArgumentsSize).forEach { result.add(DONT_CARE) }
        result
    }
    else {
        functionPlaceholderTypeConstructor.getArgumentTypes()
    }
    val receiverType = if (isExtension) DONT_CARE else null
    return KotlinBuiltIns.getInstance().getFunctionType(Annotations.EMPTY, receiverType, newArgumentTypes, DONT_CARE)
}

private fun TypeSubstitutor.setApproximateCapturedTypes(): TypeSubstitutor {
    return TypeSubstitutor.create(SubstitutionWithCapturedTypeApproximation(getSubstitution()))
}

private class SubstitutionWithCapturedTypeApproximation(val substitution: TypeSubstitution) : TypeSubstitution() {
    override fun get(key: TypeConstructor?) = substitution[key]
    override fun isEmpty() = substitution.isEmpty()
    override fun approximateCapturedTypes() = true
}

public fun ConstraintSystemImpl.registerTypeVariables(typeVariables: Map<TypeParameterDescriptor, Variance>) {
    registerTypeVariables(typeVariables.keySet(), { typeVariables[it]!! }, { it })
}

public fun ConstraintSystemImpl.registerTypeVariables(
        typeVariables: Collection<TypeParameterDescriptor>,
        variance: (TypeParameterDescriptor) -> Variance
) {
    registerTypeVariables(typeVariables, variance, { it })
}

public fun createTypeSubstitutor(conversion: (TypeParameterDescriptor) -> TypeParameterDescriptor?): TypeSubstitutor {
    return TypeSubstitutor.create(object : TypeSubstitution() {
        override fun get(key: TypeConstructor): TypeProjection? {
            val descriptor = key.getDeclarationDescriptor()
            if (descriptor !is TypeParameterDescriptor) return null
            val typeParameterDescriptor = conversion(descriptor) ?: return null

            val type = JetTypeImpl(Annotations.EMPTY, typeParameterDescriptor.getTypeConstructor(), false, listOf(), JetScope.Empty)
            return TypeProjectionImpl(type)
        }
    })
}