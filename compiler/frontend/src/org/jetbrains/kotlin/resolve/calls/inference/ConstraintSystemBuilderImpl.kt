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
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilderImpl.ConstraintKind.EQUAL
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilderImpl.ConstraintKind.SUB_TYPE
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.Bound
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.*
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.TYPE_BOUND_POSITION
import org.jetbrains.kotlin.resolve.descriptorUtil.hasExactAnnotation
import org.jetbrains.kotlin.resolve.descriptorUtil.hasNoInferAnnotation
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.TypeUtils.DONT_CARE
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedure
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedureCallbacks
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.isDefaultBound
import java.util.*

class ConstraintSystemBuilderImpl : ConstraintSystem.Builder {
    internal data class Constraint(
            val kind: ConstraintKind, val subtype: KotlinType, val superType: KotlinType, val position: ConstraintPosition
    )

    enum class ConstraintKind(val bound: TypeBounds.BoundKind) {
        SUB_TYPE(UPPER_BOUND),
        EQUAL(EXACT_BOUND)
    }

    internal val allTypeParameterBounds = LinkedHashMap<TypeParameterDescriptor, TypeBoundsImpl>()
    internal val externalTypeParameters = HashSet<TypeParameterDescriptor>()
    internal val cachedTypeForVariable = HashMap<TypeParameterDescriptor, KotlinType>()
    internal val usedInBounds = HashMap<TypeParameterDescriptor, MutableList<TypeBounds.Bound>>()
    internal val errors = ArrayList<ConstraintError>()
    internal val initialConstraints = ArrayList<Constraint>()
    internal val descriptorToVariable = LinkedHashMap<TypeParameterDescriptor, TypeParameterDescriptor>()
    internal val variableToDescriptor = LinkedHashMap<TypeParameterDescriptor, TypeParameterDescriptor>()

    private val descriptorToVariableSubstitutor: TypeSubstitutor by lazy {
        TypeSubstitutor.create(object : TypeConstructorSubstitution() {
            override fun get(key: TypeConstructor): TypeProjection? {
                val descriptor = key.declarationDescriptor
                if (descriptor !is TypeParameterDescriptor) return null
                val typeParameterDescriptor = descriptorToVariable[descriptor] ?: return null
                return TypeProjectionImpl(typeParameterDescriptor.defaultType)
            }
        })
    }

    override fun registerTypeVariables(
            typeVariables: Collection<TypeParameterDescriptor>,
            mapToDescriptor: (TypeParameterDescriptor) -> TypeParameterDescriptor,
            external: Boolean
    ) {
        if (external) externalTypeParameters.addAll(typeVariables)

        for (typeVariable in typeVariables) {
            allTypeParameterBounds.put(typeVariable, TypeBoundsImpl(typeVariable))
            val descriptor = mapToDescriptor(typeVariable)
            descriptorToVariable[descriptor] = typeVariable
            variableToDescriptor[typeVariable] = descriptor
        }
        for ((typeVariable, typeBounds) in allTypeParameterBounds) {
            for (declaredUpperBound in typeVariable.upperBounds) {
                if (declaredUpperBound.isDefaultBound()) continue //todo remove this line (?)
                val context = ConstraintContext(TYPE_BOUND_POSITION.position(typeVariable.index))
                addBound(typeVariable, declaredUpperBound, UPPER_BOUND, context)
            }
        }
    }

    internal val TypeParameterDescriptor.correspondingType: KotlinType
        get() = cachedTypeForVariable.getOrPut(this) {
            KotlinTypeImpl.create(Annotations.EMPTY, typeConstructor, false, listOf(), MemberScope.Empty)
        }

    private fun KotlinType.isProper() = !TypeUtils.containsSpecialType(this) {
        type -> type.constructor.declarationDescriptor.let { it is TypeParameterDescriptor && isMyTypeVariable(it) }
    }

    internal fun getNestedTypeVariables(type: KotlinType): List<TypeParameterDescriptor> =
            type.getNestedTypeParameters().filter { isMyTypeVariable(it) }

    override fun addSupertypeConstraint(constrainingType: KotlinType?, subjectType: KotlinType, constraintPosition: ConstraintPosition) {
        if (constrainingType != null && TypeUtils.noExpectedType(constrainingType)) return

        val newSubjectType = descriptorToVariableSubstitutor.substitute(subjectType, Variance.INVARIANT)
        addConstraint(SUB_TYPE, newSubjectType, constrainingType, ConstraintContext(constraintPosition, initial = true))
    }

    override fun addSubtypeConstraint(constrainingType: KotlinType?, subjectType: KotlinType, constraintPosition: ConstraintPosition) {
        val newSubjectType = descriptorToVariableSubstitutor.substitute(subjectType, Variance.INVARIANT)
        addConstraint(SUB_TYPE, constrainingType, newSubjectType, ConstraintContext(constraintPosition, initial = true))
    }

    fun addConstraint(
            constraintKind: ConstraintKind,
            subType: KotlinType?,
            superType: KotlinType?,
            constraintContext: ConstraintContext
    ) {
        val constraintPosition = constraintContext.position

        // when processing nested constraints, `derivedFrom` information should be reset
        val newConstraintContext = ConstraintContext(constraintContext.position, derivedFrom = null, initial = false)
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
                if (isMyTypeVariable(typeProjection.type)) return false
                val myTypeVariable = getMyTypeVariable(type)

                if (myTypeVariable != null && constraintPosition.isParameter()) {
                    if (depth > 0) {
                        errors.add(CannotCapture(constraintPosition, myTypeVariable))
                    }
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

        if ((subType.hasExactAnnotation() || superType.hasExactAnnotation()) && (constraintKind != EQUAL)) {
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
            // if subType is nullable and superType is not nullable, unsafe call or type mismatch error will be generated later,
            // but constraint system should be solved anyway
            val subTypeNotNullable = if (constraintContext.initial) TypeUtils.makeNotNullable(subType) else subType
            val superTypeNotNullable = if (constraintContext.initial) TypeUtils.makeNotNullable(superType) else superType
            val result = if (constraintKind == EQUAL) {
                typeCheckingProcedure.equalTypes(subTypeNotNullable, superTypeNotNullable)
            }
            else {
                typeCheckingProcedure.isSubtypeOf(subTypeNotNullable, superType)
            }
            if (!result) errors.add(newTypeInferenceOrParameterConstraintError(constraintPosition))
        }
        if (constraintContext.initial) {
            storeInitialConstraint(constraintKind, subType, superType, constraintPosition)
        }
        if (subType.hasNoInferAnnotation() || superType.hasNoInferAnnotation()) return

        simplifyConstraint(newSubType, superType)
    }

    internal fun addBound(
            typeVariable: TypeParameterDescriptor,
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
            typeVariable: TypeParameterDescriptor,
            constrainingTypeProjection: TypeProjection,
            constraintContext: ConstraintContext,
            isTypeMarkedNullable: Boolean
    ) {
        if (!typeVariable.upperBounds.let { it.size == 1 && it.single().isDefaultBound() } &&
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

    internal fun getBoundsUsedIn(typeVariable: TypeParameterDescriptor): List<Bound> = usedInBounds[typeVariable] ?: emptyList()

    internal fun getTypeBounds(descriptor: TypeParameterDescriptor): TypeBoundsImpl {
        val variable = descriptorToVariable[descriptor]
        if (variable != null && variable != descriptor) {
            return getTypeBounds(variable)
        }
        return allTypeParameterBounds[descriptor] ?:
               throw IllegalArgumentException("TypeParameterDescriptor is not a type variable for constraint system: $descriptor")
    }

    private fun isMyTypeVariable(typeVariable: TypeParameterDescriptor) = allTypeParameterBounds.contains(typeVariable)

    internal fun isMyTypeVariable(type: KotlinType): Boolean = getMyTypeVariable(type) != null

    internal fun getMyTypeVariable(type: KotlinType): TypeParameterDescriptor? {
        val typeParameterDescriptor = type.constructor.declarationDescriptor as? TypeParameterDescriptor
        return if (typeParameterDescriptor != null && isMyTypeVariable(typeParameterDescriptor)) typeParameterDescriptor else null
    }

    private fun storeInitialConstraint(constraintKind: ConstraintKind, subType: KotlinType, superType: KotlinType, position: ConstraintPosition) {
        initialConstraints.add(Constraint(constraintKind, subType, superType, position))
    }

    private fun fixVariable(typeVariable: TypeParameterDescriptor) {
        val typeBounds = getTypeBounds(typeVariable)
        if (typeBounds.isFixed) return
        typeBounds.setFixed()

        val nestedTypeVariables = typeBounds.bounds.flatMap { getNestedTypeVariables(it.constrainingType) }
        nestedTypeVariables.forEach { fixVariable(it) }

        val value = typeBounds.value ?: return

        addBound(typeVariable, value, TypeBounds.BoundKind.EXACT_BOUND, ConstraintContext(ConstraintPositionKind.FROM_COMPLETER.position()))
    }

    override fun fixVariables() {
        // todo variables should be fixed in the right order
        val (external, functionTypeParameters) = allTypeParameterBounds.keys.partition { it in externalTypeParameters }
        external.forEach { fixVariable(it) }
        functionTypeParameters.forEach { fixVariable(it) }
    }

    override fun build(): ConstraintSystem {
        return ConstraintSystemImpl(allTypeParameterBounds, externalTypeParameters, usedInBounds, errors, initialConstraints,
                                    descriptorToVariable, variableToDescriptor)
    }
}

internal fun createTypeForFunctionPlaceholder(
        functionPlaceholder: KotlinType,
        expectedType: KotlinType
): KotlinType {
    if (!functionPlaceholder.isFunctionPlaceholder) return functionPlaceholder

    val functionPlaceholderTypeConstructor = functionPlaceholder.constructor as FunctionPlaceholderTypeConstructor

    val isExtension = KotlinBuiltIns.isExtensionFunctionType(expectedType)
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
    return functionPlaceholder.builtIns.getFunctionType(Annotations.EMPTY, receiverType, newArgumentTypes, DONT_CARE)
}
