/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.descriptorUtil.hasExactAnnotation
import org.jetbrains.kotlin.resolve.descriptorUtil.hasNoInferAnnotation
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.KotlinTypeFactory.flexibleType
import org.jetbrains.kotlin.types.checker.*
import org.jetbrains.kotlin.types.model.CapturedTypeMarker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.contains

abstract class TypeCheckerContextForConstraintSystem : ClassicTypeCheckerContext(errorTypeEqualsToAnything = true, allowedTypeVariable = false) {

    abstract fun isMyTypeVariable(type: SimpleType): Boolean

    // super and sub type isSingleClassifierType
    abstract fun addUpperConstraint(typeVariable: TypeConstructor, superType: UnwrappedType)

    abstract fun addLowerConstraint(typeVariable: TypeConstructor, subType: UnwrappedType)

    override fun getLowerCapturedTypePolicy(subType: SimpleTypeMarker, superType: CapturedTypeMarker): LowerCapturedTypePolicy {
        require(subType is SimpleType)
        require(superType is NewCapturedType)
        return getLowerCapturedTypePolicy(subType, superType)
    }

    private fun getLowerCapturedTypePolicy(subType: SimpleType, superType: NewCapturedType) = when {
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
    final override fun addSubtypeConstraint(subType: KotlinTypeMarker, superType: KotlinTypeMarker): Boolean? {
        require(subType is UnwrappedType)
        require(superType is UnwrappedType)
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
        } else {
            extractTypeVariableForSubtype(subType)?.let {
                return simplifyUpperConstraint(it, superType) && (answer ?: true)
            }

            return simplifyConstraintForPossibleIntersectionSubType(subType, superType) ?: answer
        }
    }

    // extract type variable only from type like Captured(out T)
    private fun extractTypeVariableForSubtype(type: UnwrappedType): UnwrappedType? {
        if (type !is NewCapturedType) return null

        val projection = type.constructor.projection
        return if (projection.projectionKind == Variance.OUT_VARIANCE)
            projection.type.takeIf { it is SimpleType && isMyTypeVariable(it) }?.unwrap()
        else
            null
    }

    /**
     * Foo <: T -- leave as is
     *
     * T?
     *
     * Foo <: T? -- Foo & Any <: T -- Foo!! <: T
     * Foo? <: T? -- Foo? & Any <: T -- Foo!! <: T
     * (Foo..Bar) <: T? -- (Foo..Bar) & Any <: T -- (Foo..Bar)!! <: T
     *
     * T!
     *
     * Foo <: T! --
     * assert T! == (T..T?)
     *  Foo <: T?
     *  Foo <: T (optional constraint, needs to preserve nullability)
     * =>
     *  Foo & Any <: T
     *  Foo <: T
     * =>
     *  (Foo & Any .. Foo) <: T -- (Foo!! .. Foo) <: T
     *
     * => Foo <: T! -- (Foo!! .. Foo) <: T
     *
     * Foo? <: T! -- (Foo!! .. Foo?) <: T
     *
     *
     * (Foo..Bar) <: T! --
     * assert T! == (T..T?)
     *  (Foo..Bar) <: (T..T?)
     * =>
     *  Foo <: T?
     *  Bar <: T (optional constraint, needs to preserve nullability)
     * =>
     *  (Foo & Any .. Bar) <: T -- (Foo!! .. Bar) <: T
     *
     * => (Foo..Bar) <: T! -- (Foo!! .. Bar) <: T
     */
    private fun simplifyLowerConstraint(typeVariable: UnwrappedType, subType: UnwrappedType): Boolean {
        val lowerConstraint = when (typeVariable) {
            is SimpleType ->
                // Foo <: T or
                // Foo <: T? -- Foo!! <: T
                if (typeVariable.isMarkedNullable) subType.makeDefinitelyNotNullOrNotNull() else subType

            is FlexibleType -> {
                assertFlexibleTypeVariable(typeVariable)

                when (subType) {
                    is SimpleType ->
                        // Foo <: T! -- (Foo!! .. Foo) <: T
                        flexibleType(subType.makeSimpleTypeDefinitelyNotNullOrNotNull(), subType)

                    is FlexibleType ->
                        // (Foo..Bar) <: T! -- (Foo!! .. Bar) <: T
                        flexibleType(subType.lowerBound.makeSimpleTypeDefinitelyNotNullOrNotNull(), subType.upperBound)
                }
            }
        }

        addLowerConstraint(typeVariable.constructor, lowerConstraint)

        return true
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
        if (notTypeVariables.isNotEmpty() && NewKotlinTypeChecker.isSubtypeOf(intersectTypes(notTypeVariables) as KotlinType, superType)) {
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
        fun correctSubType(subType: SimpleType) =
            subType.isSingleClassifierType || subType.isIntersectionType || isMyTypeVariable(subType) || subType.isError

        fun correctSuperType(superType: SimpleType) =
            superType.isSingleClassifierType || superType.isIntersectionType || isMyTypeVariable(superType) || superType.isError

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