/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.types.AbstractNullabilityChecker
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import org.jetbrains.kotlin.types.model.*

abstract class AbstractTypeCheckerContextForConstraintSystem : AbstractTypeCheckerContext(), TypeSystemInferenceExtensionContext {

    override val KotlinTypeMarker.isAllowedTypeVariable: Boolean
        get() = false

    override val isErrorTypeEqualsToAnything: Boolean
        get() = true

    override val isStubTypeEqualsToAnything: Boolean
        get() = true

    abstract fun isMyTypeVariable(type: SimpleTypeMarker): Boolean

    // super and sub type isSingleClassifierType
    abstract fun addUpperConstraint(typeVariable: TypeConstructorMarker, superType: KotlinTypeMarker)

    abstract fun addLowerConstraint(typeVariable: TypeConstructorMarker, subType: KotlinTypeMarker)

    override fun getLowerCapturedTypePolicy(subType: SimpleTypeMarker, superType: CapturedTypeMarker): LowerCapturedTypePolicy {
        return when {
            isMyTypeVariable(subType) -> {
                val projection = superType.typeConstructorProjection()
                val type = projection.getType().asSimpleType()
                if (projection.getVariance() == TypeVariance.IN && type != null && isMyTypeVariable(type)) {
                    LowerCapturedTypePolicy.CHECK_ONLY_LOWER
                } else {
                    LowerCapturedTypePolicy.SKIP_LOWER
                }
            }
            subType.contains { it.anyBound(this::isMyTypeVariable) } -> LowerCapturedTypePolicy.CHECK_ONLY_LOWER
            else -> LowerCapturedTypePolicy.CHECK_SUBTYPE_AND_LOWER
        }
    }

    /**
     * todo: possible we should override this method, because otherwise OR in subtyping transformed to AND in constraint system
     * Now we cannot do this, because sometimes we have proper intersection type as lower type and if we first supertype,
     * then we can get wrong result.
     * override val sameConstructorPolicy get() = SeveralSupertypesWithSameConstructorPolicy.TAKE_FIRST_FOR_SUBTYPING
     */
    final override fun addSubtypeConstraint(subType: KotlinTypeMarker, superType: KotlinTypeMarker): Boolean? {
        val hasNoInfer = subType.isTypeVariableWithNoInfer() || superType.isTypeVariableWithNoInfer()
        if (hasNoInfer) return true

        val hasExact = subType.isTypeVariableWithExact() || superType.isTypeVariableWithExact()

        // we should strip annotation's because we have incorporation operation and they should be not affected
        val mySubType = if (hasExact) subType.removeExactAnnotation() else subType
        val mySuperType = if (hasExact) superType.removeExactAnnotation() else superType

        val result = internalAddSubtypeConstraint(mySubType, mySuperType)
        if (!hasExact) return result

        val result2 = internalAddSubtypeConstraint(mySuperType, mySubType)

        if (result == null && result2 == null) return null
        return (result ?: true) && (result2 ?: true)
    }

    private fun KotlinTypeMarker.isTypeVariableWithExact() =
        hasExactAnnotation() && anyBound(this@AbstractTypeCheckerContextForConstraintSystem::isMyTypeVariable)

    private fun KotlinTypeMarker.isTypeVariableWithNoInfer() =
        hasNoInferAnnotation() && anyBound(this@AbstractTypeCheckerContextForConstraintSystem::isMyTypeVariable)

    private fun internalAddSubtypeConstraint(subType: KotlinTypeMarker, superType: KotlinTypeMarker): Boolean? {
        assertInputTypes(subType, superType)

        var answer: Boolean? = null

        if (superType.anyBound(this::isMyTypeVariable)) {
            answer = simplifyLowerConstraint(superType, subType)
        }

        if (subType.anyBound(this::isMyTypeVariable)) {
            return simplifyUpperConstraint(subType, superType) && (answer ?: true)
        } else {
            extractTypeVariableForSubtype(subType, superType)?.let {
                return simplifyUpperConstraint(it, superType) && (answer ?: true)
            }

            return simplifyConstraintForPossibleIntersectionSubType(subType, superType) ?: answer
        }
    }

    // extract type variable only from type like Captured(out T)
    private fun extractTypeVariableForSubtype(subType: KotlinTypeMarker, superType: KotlinTypeMarker): KotlinTypeMarker? {

        val typeMarker = subType.asSimpleType()?.asCapturedType() ?: return null

        val projection = typeMarker.typeConstructorProjection()
        if (projection.isStarProjection()) return null
        if (projection.getVariance() == TypeVariance.IN) {
            val type = projection.getType().asSimpleType() ?: return null
            if (isMyTypeVariable(type)) {
                simplifyLowerConstraint(type, superType)
                if (isMyTypeVariable(superType.asSimpleType() ?: return null)) {
                    addLowerConstraint(superType.typeConstructor(), nullableAnyType())
                }
            }
            return null
        }

        return if (projection.getVariance() == TypeVariance.OUT) {
            val type = projection.getType()
            when {
                type is SimpleTypeMarker && isMyTypeVariable(type) -> type.asSimpleType()
                type is FlexibleTypeMarker && isMyTypeVariable(type.lowerBound()) -> type.asFlexibleType()?.lowerBound()
                else -> null
            }
        } else null
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
     * Foo? <: T! -- Foo? <: T
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
    private fun simplifyLowerConstraint(typeVariable: KotlinTypeMarker, subType: KotlinTypeMarker): Boolean {
        val lowerConstraint = when (typeVariable) {
            is SimpleTypeMarker ->
                // Foo <: T or
                // Foo <: T? -- Foo!! <: T
                if (typeVariable.isMarkedNullable()) subType.makeDefinitelyNotNullOrNotNull() else subType

            is FlexibleTypeMarker -> {
                assertFlexibleTypeVariable(typeVariable)

                when (subType) {
                    is SimpleTypeMarker ->
                        // Foo <: T! -- (Foo!! .. Foo) <: T
                        if (subType.isMarkedNullable()) {
                            subType // prefer nullable type to flexible one: `Foo? <: (T..T?)` => lowerConstraint = `Foo?`
                        } else {
                            createFlexibleType(subType, subType.withNullability(true))
                        }

                    is FlexibleTypeMarker ->
                        // (Foo..Bar) <: T! -- (Foo!! .. Bar) <: T
                        createFlexibleType(subType.lowerBound().makeSimpleTypeDefinitelyNotNullOrNotNull(), subType.upperBound())

                    else -> error("sealed")
                }
            }
            else -> error("sealed")
        }

        addLowerConstraint(typeVariable.typeConstructor(), lowerConstraint)

        return true
    }

    private fun assertFlexibleTypeVariable(typeVariable: FlexibleTypeMarker) {
        assert(typeVariable.lowerBound().typeConstructor() == typeVariable.upperBound().typeConstructor()) {
            "Flexible type variable ($typeVariable) should have bounds with the same type constructor, i.e. (T..T?)"
        }
    }

    /**
     * T! <: Foo <=> T <: Foo..Foo?
     * T? <: Foo <=> T <: Foo && Nothing? <: Foo
     * T  <: Foo -- leave as is
     */
    private fun simplifyUpperConstraint(typeVariable: KotlinTypeMarker, superType: KotlinTypeMarker): Boolean {
        val typeVariableLowerBound = typeVariable.lowerBoundIfFlexible()
        val simplifiedSuperType = if (typeVariableLowerBound.isDefinitelyNotNullType()) {
            superType.withNullability(true)
        } else if (typeVariable.isFlexible() && superType is SimpleTypeMarker) {
            createFlexibleType(superType, superType.withNullability(true))
        } else superType

        addUpperConstraint(typeVariableLowerBound.typeConstructor(), simplifiedSuperType)

        if (typeVariableLowerBound.isMarkedNullable()) {
            // here is important that superType is singleClassifierType
            return simplifiedSuperType.anyBound(this::isMyTypeVariable) ||
                    isSubtypeOfByTypeChecker(nullableNothingType(), simplifiedSuperType)
        }

        return true
    }

    private fun simplifyConstraintForPossibleIntersectionSubType(subType: KotlinTypeMarker, superType: KotlinTypeMarker): Boolean? {
        @Suppress("NAME_SHADOWING")
        val subType = subType.lowerBoundIfFlexible()

        if (!subType.typeConstructor().isIntersection()) return null

        assert(!subType.isMarkedNullable()) { "Intersection type should not be marked nullable!: $subType" }

        // TODO: may be we lose flexibility here
        val subIntersectionTypes = (subType.typeConstructor().supertypes()).map { it.lowerBoundIfFlexible() }

        val typeVariables = subIntersectionTypes.filter(this::isMyTypeVariable).takeIf { it.isNotEmpty() } ?: return null
        val notTypeVariables = subIntersectionTypes.filterNot(this::isMyTypeVariable)

        // todo: may be we can do better then that.
        if (notTypeVariables.isNotEmpty() &&
            AbstractTypeChecker.isSubtypeOf(
                this as TypeCheckerProviderContext,
                intersectTypes(notTypeVariables),
                superType
            )
        ) {
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
        if (notTypeVariables.any { AbstractNullabilityChecker.isSubtypeOfAny(this as TypeCheckerProviderContext, it) }) {
            return typeVariables.all { simplifyUpperConstraint(it, superType.withNullability(true)) }
        }

        return typeVariables.all { simplifyUpperConstraint(it, superType) }
    }

    private fun isSubtypeOfByTypeChecker(subType: KotlinTypeMarker, superType: KotlinTypeMarker) =
        AbstractTypeChecker.isSubtypeOf(this as AbstractTypeCheckerContext, subType, superType)

    private fun assertInputTypes(subType: KotlinTypeMarker, superType: KotlinTypeMarker) {
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        fun correctSubType(subType: SimpleTypeMarker) =
            subType.isSingleClassifierType() || subType.typeConstructor().isIntersection() || isMyTypeVariable(subType) || subType.isError() || subType.isIntegerLiteralType()

        fun correctSuperType(superType: SimpleTypeMarker) =
            superType.isSingleClassifierType() || superType.typeConstructor().isIntersection() || isMyTypeVariable(superType) || superType.isError() || superType.isIntegerLiteralType()

        assert(subType.bothBounds(::correctSubType)) {
            "Not singleClassifierType and not intersection subType: $subType"
        }
        assert(superType.bothBounds(::correctSuperType)) {
            "Not singleClassifierType superType: $superType"
        }
    }

    private inline fun KotlinTypeMarker.bothBounds(f: (SimpleTypeMarker) -> Boolean) = when (this) {
        is SimpleTypeMarker -> f(this)
        is FlexibleTypeMarker -> f(lowerBound()) && f(upperBound())
        else -> error("sealed")
    }

    private inline fun KotlinTypeMarker.anyBound(f: (SimpleTypeMarker) -> Boolean) = when (this) {
        is SimpleTypeMarker -> f(this)
        is FlexibleTypeMarker -> f(lowerBound()) || f(upperBound())
        else -> error("sealed")
    }
}
