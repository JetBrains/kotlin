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

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilderImpl.ConstraintKind.EQUAL
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilderImpl.ConstraintKind.SUB_TYPE
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.Bound
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.*
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.TYPE_BOUND_POSITION
import org.jetbrains.kotlin.resolve.calls.results.SimpleConstraintSystem
import org.jetbrains.kotlin.resolve.calls.util.createFunctionType
import org.jetbrains.kotlin.resolve.descriptorUtil.hasExactAnnotation
import org.jetbrains.kotlin.resolve.descriptorUtil.hasNoInferAnnotation
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.TypeUtils.DONT_CARE
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedure
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedureCallbacks
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.defaultProjections
import org.jetbrains.kotlin.types.typeUtil.isDefaultBound
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.util.*

open class ConstraintSystemBuilderImpl(private val mode: Mode = ConstraintSystemBuilderImpl.Mode.INFERENCE) : ConstraintSystem.Builder {
    enum class Mode {
        INFERENCE,
        SPECIFICITY
    }

    internal data class Constraint(
            val kind: ConstraintKind, val subtype: KotlinType, val superType: KotlinType, val position: ConstraintPosition
    )

    enum class ConstraintKind(val bound: TypeBounds.BoundKind) {
        SUB_TYPE(UPPER_BOUND),
        EQUAL(EXACT_BOUND)
    }

    internal val allTypeParameterBounds = LinkedHashMap<TypeVariable, TypeBoundsImpl>()
    internal val usedInBounds = HashMap<TypeVariable, MutableList<TypeBounds.Bound>>()
    internal val errors = ArrayList<ConstraintError>()
    internal val initialConstraints = ArrayList<Constraint>()

    override val typeVariableSubstitutors = LinkedHashMap<CallHandle, TypeSubstitutor>()

    private fun storeSubstitutor(call: CallHandle, substitutor: TypeSubstitutor): TypeSubstitutor {
        if (typeVariableSubstitutors.containsKey(call)) {
            throw IllegalStateException("Type variables for the same call can be registered only once: $call")
        }
        typeVariableSubstitutors[call] = substitutor
        return substitutor
    }

    override fun registerTypeVariables(
            call: CallHandle, typeParameters: Collection<TypeParameterDescriptor>, external: Boolean
    ): TypeSubstitutor {
        if (typeParameters.isEmpty()) return storeSubstitutor(call, TypeSubstitutor.EMPTY)

        val typeVariables = if (external) {
            typeParameters.map {
                TypeVariable(call, it, it, true)
            }
        }
        else {
            val freshTypeParameters = ArrayList<TypeParameterDescriptor>(typeParameters.size)
            DescriptorSubstitutor.substituteTypeParameters(
                    typeParameters.toList(), TypeSubstitution.EMPTY, typeParameters.first().containingDeclaration, freshTypeParameters
            )
            freshTypeParameters.zip(typeParameters).map {
                val (fresh, original) = it
                TypeVariable(call, fresh, original, external)
            }
        }

        for ((descriptor, typeVariable) in typeParameters.zip(typeVariables)) {
            allTypeParameterBounds.put(typeVariable, TypeBoundsImpl(typeVariable))
        }

        for ((typeVariable, typeBounds) in allTypeParameterBounds) {
            for (declaredUpperBound in typeVariable.freshTypeParameter.upperBounds) {
                if (declaredUpperBound.isDefaultBound()) continue //todo remove this line (?)
                val context = ConstraintContext(TYPE_BOUND_POSITION.position(typeVariable.originalTypeParameter.index))
                addBound(typeVariable, declaredUpperBound, UPPER_BOUND, context)
            }
        }

        return storeSubstitutor(call, TypeSubstitutor.create(TypeConstructorSubstitution.createByParametersMap(
                typeParameters.zip(typeVariables.map { it.type }.defaultProjections()).toMap()
        )))
    }

    private fun KotlinType.isProper() = !TypeUtils.contains(this) {
        type -> type.constructor.declarationDescriptor.let { it is TypeParameterDescriptor && isMyTypeVariable(it) }
    }

    internal fun getNestedTypeVariables(type: KotlinType): List<TypeVariable> =
            type.getNestedTypeParameters().mapNotNull { getMyTypeVariable(it) }

    override fun addSubtypeConstraint(constrainingType: KotlinType?, subjectType: KotlinType?, constraintPosition: ConstraintPosition) {
        addConstraint(SUB_TYPE, constrainingType, subjectType, ConstraintContext(constraintPosition, initial = true, initialReduction = true))
    }

    fun addConstraint(
            constraintKind: ConstraintKind,
            subType: KotlinType?,
            superType: KotlinType?,
            constraintContext: ConstraintContext
    ) {
        val constraintPosition = constraintContext.position

        // when processing nested constraints, `derivedFrom` information should be reset
        val newConstraintContext = ConstraintContext(constraintContext.position, derivedFrom = null, initial = false,
                                                     initialReduction = constraintContext.initialReduction)
        val typeCheckingProcedure = TypeCheckingProcedure(object : TypeCheckingProcedureCallbacks {
            private var depth = 0

            override fun assertEqualTypes(a: KotlinType, b: KotlinType, typeCheckingProcedure: TypeCheckingProcedure): Boolean {
                depth++
                doAddConstraint(EQUAL, a, b, newConstraintContext, typeCheckingProcedure)
                depth--
                return true

            }

            override fun assertEqualTypeConstructors(a: TypeConstructor, b: TypeConstructor): Boolean {
                return a == b
            }

            override fun assertSubtype(subtype: KotlinType, supertype: KotlinType, typeCheckingProcedure: TypeCheckingProcedure): Boolean {
                depth++
                doAddConstraint(SUB_TYPE, subtype, supertype, newConstraintContext, typeCheckingProcedure)
                depth--
                return true
            }

            override fun capture(type: KotlinType, typeProjection: TypeProjection): Boolean {
                if (isMyTypeVariable(typeProjection.type) || depth > 0) return false
                val myTypeVariable = getMyTypeVariable(type)

                if (myTypeVariable != null && constraintPosition.isParameter()) {
                    generateTypeParameterCaptureConstraint(myTypeVariable, typeProjection, newConstraintContext, type.isMarkedNullable)
                    return true
                }
                return false
            }

            override fun noCorrespondingSupertype(subtype: KotlinType, supertype: KotlinType): Boolean {
                errors.add(newTypeInferenceOrParameterConstraintError(constraintPosition))
                return true
            }
        })
        doAddConstraint(constraintKind, subType, superType, constraintContext, typeCheckingProcedure)
    }

    private fun isErrorOrSpecialType(type: KotlinType?, constraintPosition: ConstraintPosition): Boolean {
        if (TypeUtils.isDontCarePlaceholder(type) || ErrorUtils.isUninferredParameter(type)) {
            return true
        }

        if (type == null || (type.isError && !type.isFunctionPlaceholder)) {
            errors.add(ErrorInConstrainingType(constraintPosition))
            return true
        }
        return false
    }

    private fun doAddConstraint(
            constraintKind: ConstraintKind,
            subType: KotlinType?,
            superType: KotlinType?,
            constraintContext: ConstraintContext,
            typeCheckingProcedure: TypeCheckingProcedure
    ) {
        val constraintPosition = constraintContext.position
        if (isErrorOrSpecialType(subType, constraintPosition) || isErrorOrSpecialType(superType, constraintPosition)) return
        if (subType == null || superType == null) return

        if (constraintContext.initialReduction && (subType.hasExactAnnotation() || superType.hasExactAnnotation()) && (constraintKind != EQUAL)) {
            return doAddConstraint(EQUAL, subType, superType, constraintContext, typeCheckingProcedure)
        }

        assert(!superType.isFunctionPlaceholder) { "The type for $constraintPosition shouldn't be a placeholder for function type" }

        // function literal { x -> ... } goes without declaring receiver type
        // and can be considered as extension function if one is expected
        val newSubType = if (constraintKind == SUB_TYPE && subType.isFunctionPlaceholder) {
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

        fun simplifyConstraint(subType: KotlinType, superType: KotlinType) {
            if (isMyTypeVariable(subType)) {
                generateTypeParameterBound(subType, superType, constraintKind.bound, constraintContext)
                return
            }
            if (isMyTypeVariable(superType)) {
                generateTypeParameterBound(superType, subType, constraintKind.bound.reverse(), constraintContext)
                return
            }
            val subType2 = simplifyType(subType, constraintContext.initial)
            val superType2 = simplifyType(superType, constraintContext.initial)
            val result = if (constraintKind == EQUAL) {
                typeCheckingProcedure.equalTypes(subType2, superType2)
            }
            else {
                typeCheckingProcedure.isSubtypeOf(subType2, superType)
            }
            if (!result) errors.add(newTypeInferenceOrParameterConstraintError(constraintPosition))
        }
        if (constraintContext.initial) {
            storeInitialConstraint(constraintKind, subType, superType, constraintPosition)
        }
        if (subType.hasNoInferAnnotation() || superType.hasNoInferAnnotation()) return

        simplifyConstraint(newSubType, superType)
    }

    private fun simplifyType(type: KotlinType, isInitialConstraint: Boolean): KotlinType =
            if (mode == Mode.SPECIFICITY || !isInitialConstraint)
                type
            else {
                // if subType is nullable and superType is not nullable, unsafe call or type mismatch error will be generated later,
                // but constraint system should be solved anyway
                TypeUtils.makeNotNullable(type)
            }

    internal fun addBound(
            typeVariable: TypeVariable,
            constrainingType: KotlinType,
            kind: TypeBounds.BoundKind,
            constraintContext: ConstraintContext
    ) {
        val bound = Bound(typeVariable, constrainingType, kind, constraintContext.position,
                          constrainingType.isProper(), constraintContext.derivedFrom ?: emptySet())
        val typeBounds = getTypeBounds(typeVariable)
        if (typeBounds.bounds.contains(bound)) return

        typeBounds.addBound(bound)

        if (!bound.isProper) {
            for (dependentTypeVariable in getNestedTypeVariables(bound.constrainingType)) {
                val dependentBounds = usedInBounds.getOrPut(dependentTypeVariable) { arrayListOf() }
                dependentBounds.add(bound)
            }
        }

        incorporateBound(bound)
    }

    private fun generateTypeParameterBound(
            parameterType: KotlinType,
            constrainingType: KotlinType,
            boundKind: TypeBounds.BoundKind,
            constraintContext: ConstraintContext
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

        if (!parameterType.isMarkedNullable || !TypeUtils.isNullableType(newConstrainingType)) {
            addBound(typeVariable, newConstrainingType, boundKind, constraintContext)
            return
        }
        // For parameter type T:
        // constraint T? =  Int? should transform to T >: Int and T <: Int?
        // constraint T? = Int! should transform to T >: Int and T <: Int!

        // constraints T? >: Int?; T? >: Int! should transform to T >: Int
        val notNullConstrainingType = TypeUtils.makeNotNullable(newConstrainingType)
        if (boundKind == EXACT_BOUND || boundKind == LOWER_BOUND) {
            addBound(typeVariable, notNullConstrainingType, LOWER_BOUND, constraintContext)
        }
        // constraints T? <: Int?; T? <: Int! should transform to T <: Int?; T <: Int! correspondingly
        if (boundKind == EXACT_BOUND || boundKind == UPPER_BOUND) {
            addBound(typeVariable, newConstrainingType, UPPER_BOUND, constraintContext)
        }
    }

    private fun generateTypeParameterCaptureConstraint(
            typeVariable: TypeVariable,
            constrainingTypeProjection: TypeProjection,
            constraintContext: ConstraintContext,
            isTypeMarkedNullable: Boolean
    ) {
        if (!typeVariable.originalTypeParameter.upperBounds.let { it.size == 1 && it.single().isDefaultBound() } &&
            constrainingTypeProjection.projectionKind == Variance.IN_VARIANCE) {
            errors.add(CannotCapture(constraintContext.position, typeVariable))
        }
        val typeProjection = if (isTypeMarkedNullable) {
            TypeProjectionImpl(constrainingTypeProjection.projectionKind, TypeUtils.makeNotNullable(constrainingTypeProjection.type))
        }
        else {
            constrainingTypeProjection
        }
        val capturedType = createCapturedType(typeProjection)
        addBound(typeVariable, capturedType, EXACT_BOUND, constraintContext)
    }

    internal fun getTypeBounds(variable: TypeVariable): TypeBoundsImpl {
        return allTypeParameterBounds[variable] ?:
               throw IllegalArgumentException("TypeParameterDescriptor is not a type variable for constraint system: $variable")
    }

    private fun isMyTypeVariable(typeParameter: TypeParameterDescriptor) =
            getMyTypeVariable(typeParameter) != null

    internal fun isMyTypeVariable(type: KotlinType): Boolean =
            getMyTypeVariable(type) != null

    internal fun getMyTypeVariable(type: KotlinType): TypeVariable? {
        return getMyTypeVariable(type.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return null)
    }

    private fun getMyTypeVariable(typeParameter: TypeParameterDescriptor): TypeVariable? =
            allTypeParameterBounds.keys.find { it.freshTypeParameter == typeParameter }

    private fun storeInitialConstraint(constraintKind: ConstraintKind, subType: KotlinType, superType: KotlinType, position: ConstraintPosition) {
        initialConstraints.add(Constraint(constraintKind, subType, superType, position))
    }

    private fun fixVariable(typeVariable: TypeVariable) {
        val typeBounds = getTypeBounds(typeVariable)
        if (typeBounds.isFixed) return
        typeBounds.setFixed()

        val nestedTypeVariables = typeBounds.bounds.flatMap { getNestedTypeVariables(it.constrainingType) }
        nestedTypeVariables.forEach { fixVariable(it) }

        val value = typeBounds.value ?: return

        addBound(typeVariable, value, TypeBounds.BoundKind.EXACT_BOUND, ConstraintContext(ConstraintPositionKind.FROM_COMPLETER.position()))
    }

    override fun add(other: ConstraintSystem.Builder) {
        if (other !is ConstraintSystemBuilderImpl) {
            throw IllegalArgumentException("Unknown constraint system builder implementation: $other")
        }
        if (!Collections.disjoint(typeVariableSubstitutors.keys, other.typeVariableSubstitutors.keys)) {
            throw IllegalArgumentException(
                    "Combining two constraint systems only makes sense when they were created for different calls. " +
                    "Calls of the first system: ${typeVariableSubstitutors.keys}, second: ${other.typeVariableSubstitutors.keys}"
            )
        }
        if (!Collections.disjoint(other.allTypeParameterBounds.keys, allTypeParameterBounds.keys)) {
            throw IllegalArgumentException(
                    "Combining two constraint systems only makes sense when they have no common variables. " +
                    "First system variables: ${allTypeParameterBounds.keys}, second: ${other.allTypeParameterBounds.keys}"
            )
        }

        allTypeParameterBounds.putAll(other.allTypeParameterBounds)
        usedInBounds.putAll(other.usedInBounds)
        errors.addAll(other.errors)
        initialConstraints.addAll(other.initialConstraints)
        typeVariableSubstitutors.putAll(other.typeVariableSubstitutors)
    }

    override fun fixVariables() {
        // todo variables should be fixed in the right order
        val (external, functionTypeParameters) = allTypeParameterBounds.keys.partition { it.isExternal }
        external.forEach { fixVariable(it) }
        functionTypeParameters.forEach { fixVariable(it) }
    }

    override fun build(): ConstraintSystem {
        return ConstraintSystemImpl(allTypeParameterBounds, usedInBounds, errors, initialConstraints, typeVariableSubstitutors)
    }

    companion object {
        fun forSpecificity(): SimpleConstraintSystem = object : ConstraintSystemBuilderImpl(Mode.SPECIFICITY), SimpleConstraintSystem {
            var counter = 0

            override fun registerTypeVariables(typeParameters: Collection<TypeParameterDescriptor>) =
                    registerTypeVariables(CallHandle.NONE, typeParameters)

            override fun addSubtypeConstraint(subType: UnwrappedType, superType: UnwrappedType) =
                    addSubtypeConstraint(subType, superType, ConstraintPositionKind.VALUE_PARAMETER_POSITION.position(counter++))

            override fun hasContradiction(): Boolean {
                fixVariables()
                return build().status.hasContradiction()
            }
        }
    }
}

internal fun createTypeForFunctionPlaceholder(
        functionPlaceholder: KotlinType,
        expectedType: KotlinType
): KotlinType {
    if (!functionPlaceholder.isFunctionPlaceholder) return functionPlaceholder

    val functionPlaceholderTypeConstructor = functionPlaceholder.constructor as FunctionPlaceholderTypeConstructor

    val isExtension = expectedType.isExtensionFunctionType
    val newArgumentTypes = if (!functionPlaceholderTypeConstructor.hasDeclaredArguments) {
        val typeParamSize = expectedType.constructor.parameters.size
        // the first parameter is receiver (if present), the last one is return type,
        // the remaining are function arguments
        val functionArgumentsSize = if (isExtension) typeParamSize - 2 else typeParamSize - 1
        val result = arrayListOf<KotlinType>()
        (1..functionArgumentsSize).forEach { result.add(DONT_CARE) }
        result
    }
    else {
        functionPlaceholderTypeConstructor.argumentTypes
    }
    val receiverType = if (isExtension) DONT_CARE else null
    val parameterNames = newArgumentTypes.map { SpecialNames.NO_NAME_PROVIDED }
    return createFunctionType(functionPlaceholder.builtIns, Annotations.EMPTY, receiverType, newArgumentTypes, parameterNames, DONT_CARE)
}
