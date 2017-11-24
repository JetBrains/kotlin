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

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.descriptorUtil.hasExactAnnotation
import org.jetbrains.kotlin.resolve.descriptorUtil.hasNoInferAnnotation
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.*
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.contains

abstract class TypeCheckerContextForConstraintSystem : TypeCheckerContext(errorTypeEqualsToAnything = true, allowedTypeVariable = false) {

    abstract fun isMyTypeVariable(type: SimpleType): Boolean

    // super and sub type isSingleClassifierType
    abstract fun addUpperConstraint(typeVariable: TypeConstructor, superType: UnwrappedType)
    abstract fun addLowerConstraint(typeVariable: TypeConstructor, subType: UnwrappedType)

    override fun getLowerCapturedTypePolicy(subType: SimpleType, superType: NewCapturedType) = when {
        isMyTypeVariable(subType) -> LowerCapturedTypePolicy.SKIP_LOWER
        subType.contains { it.anyBound(this::isMyTypeVariable) } -> LowerCapturedTypePolicy.CHECK_ONLY_LOWER
        else -> LowerCapturedTypePolicy.CHECK_SUBTYPE_AND_LOWER
    }

    /**
     * todo: possible we should override this method, because otherwise OR in subtyping transformed to AND in constraint system
     * Now we cannot do this, because sometimes we have proper intersection type as lower type and if we first supertype,
     * then we can get wrong result.
     * override val sameConstructorPolicy get() = SeveralSupertypesWithSameConstructorPolicy.TAKE_FIRST_FOR_SUBTYPING
     */
    override final fun addSubtypeConstraint(subType: UnwrappedType, superType: UnwrappedType): Boolean? {
        val hasNoInfer = subType.isTypeVariableWithNoInfer() || superType.isTypeVariableWithNoInfer()
        if (hasNoInfer) return true

        val hasExact = subType.isTypeVariableWithExact() || superType.isTypeVariableWithExact()

        // we should strip annotation's because we have incorporation operation and they should be not affected
        val mySubType = if (hasExact) subType.replaceAnnotations(Annotations.EMPTY) else subType
        val mySuperType = if (hasExact) superType.replaceAnnotations(Annotations.EMPTY) else superType

        val result = internalAddSubtypeConstraint(mySubType, mySuperType)
        if (!hasExact) return result

        val result2 = internalAddSubtypeConstraint(mySuperType, mySubType)

        if (result == null && result2 == null) return null
        return (result ?: true) && (result2 ?: true)
    }

    private fun UnwrappedType.isTypeVariableWithExact() =
            anyBound(this@TypeCheckerContextForConstraintSystem::isMyTypeVariable) && hasExactAnnotation()

    private fun UnwrappedType.isTypeVariableWithNoInfer() =
            anyBound(this@TypeCheckerContextForConstraintSystem::isMyTypeVariable) && hasNoInferAnnotation()

    private fun internalAddSubtypeConstraint(subType: UnwrappedType, superType: UnwrappedType): Boolean? {
        assertInputTypes(subType, superType)

        var answer: Boolean? = null

        if (superType.anyBound(this::isMyTypeVariable)) {
            answer = simplifyLowerConstraint(superType, subType)
        }

        if (subType.anyBound(this::isMyTypeVariable)) {
            return simplifyUpperConstraint(subType, superType) && (answer ?: true)
        }
        else {
            return simplifyConstraintForPossibleIntersectionSubType(subType, superType) ?: answer
        }
    }

    /**
     * Foo <: T -- leave as is
     *
     * T?
     *
     * Foo <: T? -- Foo & Any <: T
     * Foo? <: T? -- Foo? & Any <: T
     * (Foo..Bar) <: T? -- (Foo..Bar) & Any <: T
     *
     * T!
     *
     * Foo <: T! --
     * assert T! == (T..T?)
     *  Foo <: T
     *  Foo <: T?
     * =>
     *  Foo <: T
     *  Foo & Any <: T
     * =>
     *  (Foo & Any .. Foo) <: T
     *
     * => Foo <: T! -- (Foo & Any .. Foo) <: T
     *
     * Foo? <: T! -- (Foo? & Any .. Foo?) <: T
     *
     *
     * (Foo..Bar) <: T! --
     * assert T! == (T..T?)
     *  (Foo..Bar) <: (T..T?)
     * =>
     *  Foo <: T?
     *  Bar <: T
     * =>
     *  (Foo & Any .. Bar) <: T
     *
     * => (Foo..Bar) <: T! -- (Foo & Any .. Bar) <: T
     */
    private fun simplifyLowerConstraint(typeVariable: UnwrappedType, subType: UnwrappedType): Boolean {
        when (typeVariable) {
            is SimpleType ->
                addLowerConstraintForSimpleType(typeVariable, subType)

            is FlexibleType ->
                when (subType) {
                    is SimpleType -> addLowerConstraintWithSimpleSubtype(typeVariable, subType)
                    is FlexibleType -> addLowerConstraintWithFlexibleSubtype(typeVariable, subType)
                }
        }

        return true
    }

    // Foo <: T! -- (Foo & Any .. Foo) <: T
    private fun addLowerConstraintWithSimpleSubtype(typeVariable: FlexibleType, subType: SimpleType) {
        assertFlexibleTypeVariable(typeVariable)
        addLowerConstraint(typeVariable.constructor, KotlinTypeFactory.flexibleType(subType.makeSimpleTypeDefinitelyNotNullOrNotNull(), subType))
    }

    // (Foo..Bar) <: T! -- (Foo & Any .. Bar) <: T
    private fun addLowerConstraintWithFlexibleSubtype(typeVariable: FlexibleType, subType: FlexibleType) {
        assertFlexibleTypeVariable(typeVariable)

        val lowerBound = subType.lowerBound
        val upperBound = subType.upperBound

        addLowerConstraint(typeVariable.constructor, KotlinTypeFactory.flexibleType(lowerBound.makeSimpleTypeDefinitelyNotNullOrNotNull(), upperBound))
    }

    // Foo <: T or
    // Foo <: T? -- Foo & Any <: T
    private fun addLowerConstraintForSimpleType(typeVariable: SimpleType, subType: UnwrappedType) {
        if (typeVariable.isMarkedNullable)
            addLowerConstraint(typeVariable.constructor, subType.makeDefinitelyNotNullOrNotNull())
        else
            addLowerConstraint(typeVariable.constructor, subType)
    }

    private fun assertFlexibleTypeVariable(typeVariable: FlexibleType) {
        assert(typeVariable.lowerBound.constructor == typeVariable.upperBound.constructor) {
            "Flexible type variable ($typeVariable) should have bounds with the same type constructor, i.e. (T..T?)"
        }
    }

    /**
     * T! <: Foo <=> T <: Foo
     * T? <: Foo <=> T <: Foo && Nothing? <: Foo
     * T  <: Foo -- leave as is
     */
    private fun simplifyUpperConstraint(typeVariable: UnwrappedType, superType: UnwrappedType): Boolean {
        @Suppress("NAME_SHADOWING")
        val typeVariable = typeVariable.lowerIfFlexible()

        @Suppress("NAME_SHADOWING")
        val superType = if (typeVariable is DefinitelyNotNullType) superType.makeNullableAsSpecified(true) else superType

        addUpperConstraint(typeVariable.constructor, superType)

        if (typeVariable.isMarkedNullable) {
            // here is important that superType is singleClassifierType
            return superType.anyBound(this::isMyTypeVariable) ||
                isSubtypeOfByTypeChecker(typeVariable.builtIns.nullableNothingType, superType)
        }

        return true
    }

    private fun simplifyConstraintForPossibleIntersectionSubType(subType: UnwrappedType, superType: UnwrappedType): Boolean? {
        @Suppress("NAME_SHADOWING")
        val subType = subType.lowerIfFlexible()

        if (!subType.isIntersectionType) return null

        assert(!subType.isMarkedNullable) { "Intersection type should not be marked nullable!: $subType" }

        // TODO: may be we lose flexibility here
        val subIntersectionTypes = (subType.constructor as IntersectionTypeConstructor).supertypes.map { it.lowerIfFlexible() }

        val typeVariables = subIntersectionTypes.filter(this::isMyTypeVariable).takeIf { it.isNotEmpty() } ?: return null
        val notTypeVariables = subIntersectionTypes.filterNot(this::isMyTypeVariable)

        // todo: may be we can do better then that.
        if (notTypeVariables.isNotEmpty() && NewKotlinTypeChecker.isSubtypeOf(intersectTypes(notTypeVariables), superType)) {
            return true
        }

//       Consider the following example:
//      fun <T> id(x: T): T = x
//      fun <S> id2(x: S?, y: S): S = y
//
//      fun checkLeftAssoc(a: Int?) : Int {
//          return id2(id(a), 3)
//      }
//
//      fun box() : String {
//          return "OK"
//      }
//
//      here we try to add constraint {Any & T} <: S from `id(a)`
//      Previously we thought that if `Any` isn't a subtype of S => T <: S, which is wrong, now we use weaker upper constraint
//      TODO: rethink, maybe we should take nullability into account somewhere else
        if (notTypeVariables.any { NullabilityChecker.isSubtypeOfAny(it) }) {
            return typeVariables.all { simplifyUpperConstraint(it, superType.makeNullableAsSpecified(true)) }
        }

        return typeVariables.all { simplifyUpperConstraint(it, superType) }
    }

    private fun isSubtypeOfByTypeChecker(subType: UnwrappedType, superType: UnwrappedType) =
            with(NewKotlinTypeChecker) { this@TypeCheckerContextForConstraintSystem.isSubtypeOf(subType, superType) }

    private fun assertInputTypes(subType: UnwrappedType, superType: UnwrappedType) {
        fun correctSubType(subType: SimpleType) = subType.isSingleClassifierType || subType.isIntersectionType || isMyTypeVariable(subType)
        fun correctSuperType(superType: SimpleType) = superType.isSingleClassifierType || isMyTypeVariable(superType)

        assert(subType.bothBounds(::correctSubType)) {
            "Not singleClassifierType and not intersection subType: $subType"
        }
        assert(superType.bothBounds(::correctSuperType)) {
            "Not singleClassifierType superType: $superType"
        }
    }

    private inline fun UnwrappedType.bothBounds(f: (SimpleType) -> Boolean) = when (this) {
        is SimpleType -> f(this)
        is FlexibleType -> f(lowerBound) && f(upperBound)
    }

    private inline fun UnwrappedType.anyBound(f: (SimpleType) -> Boolean) = when (this) {
        is SimpleType -> f(this)
        is FlexibleType -> f(lowerBound) || f(upperBound)
    }
}