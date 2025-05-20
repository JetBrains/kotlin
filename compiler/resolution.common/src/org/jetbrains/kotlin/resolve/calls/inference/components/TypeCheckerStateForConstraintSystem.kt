/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.config.LanguageFeature.InferenceEnhancementsIn21
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.calls.inference.model.NoInferConstraint
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.*

abstract class TypeCheckerStateForConstraintSystem(
    val extensionTypeContext: TypeSystemInferenceExtensionContext,
    kotlinTypePreparator: AbstractTypePreparator,
    kotlinTypeRefiner: AbstractTypeRefiner
) : TypeCheckerState(
    isErrorTypeEqualsToAnything = true,
    isStubTypeEqualsToAnything = true,
    isDnnTypesEqualToFlexible = false,
    allowedTypeVariable = false,
    typeSystemContext = extensionTypeContext,
    kotlinTypePreparator,
    kotlinTypeRefiner
) {
    abstract val languageVersionSettings: LanguageVersionSettings

    @K2Only
    val constraintsWithNoInfer = mutableListOf<NoInferConstraint>()

    abstract fun isMyTypeVariable(type: RigidTypeMarker): Boolean

    // super and sub type isSingleClassifierType
    abstract fun addUpperConstraint(typeVariable: TypeConstructorMarker, superType: KotlinTypeMarker)

    abstract fun addLowerConstraint(
        typeVariable: TypeConstructorMarker,
        subType: KotlinTypeMarker,
        isFromNullabilityConstraint: Boolean = false
    )

    abstract fun addEqualityConstraint(typeVariable: TypeConstructorMarker, type: KotlinTypeMarker)

    override fun getLowerCapturedTypePolicy(subType: RigidTypeMarker, superType: CapturedTypeMarker): LowerCapturedTypePolicy =
        with(extensionTypeContext) {
            return when {
                isMyTypeVariable(subType) -> {
                    val projection = superType.typeConstructorProjection()
                    val type = projection.getType()?.asRigidType()
                    if (projection.getVariance() == TypeVariance.IN && type != null && isMyTypeVariable(type)) {
                        LowerCapturedTypePolicy.CHECK_ONLY_LOWER
                    } else {
                        LowerCapturedTypePolicy.SKIP_LOWER
                    }
                }
                subType.contains { it.anyBound(::isMyTypeVariable) } -> LowerCapturedTypePolicy.CHECK_ONLY_LOWER
                else -> LowerCapturedTypePolicy.CHECK_SUBTYPE_AND_LOWER
            }
        }

    /**
     * todo: possible we should override this method, because otherwise OR in subtyping transformed to AND in constraint system
     * Now we cannot do this, because sometimes we have proper intersection type as lower type and if we first supertype,
     * then we can get wrong result.
     * override val sameConstructorPolicy get() = SeveralSupertypesWithSameConstructorPolicy.TAKE_FIRST_FOR_SUBTYPING
     */
    final override fun addSubtypeConstraint(
        subType: KotlinTypeMarker,
        superType: KotlinTypeMarker,
        isFromNullabilityConstraint: Boolean
    ): Boolean? {
        val subTypeHasNoInfer = subType.isTypeVariableWithNoInfer()
        val superTypeHasNoInfer = superType.isTypeVariableWithNoInfer()
        if (subTypeHasNoInfer || superTypeHasNoInfer) {
            if (extensionTypeContext.isK2) {
                @OptIn(K2Only::class)
                constraintsWithNoInfer += NoInferConstraint(subType, superType)
            } else {
                return true
            }
        }

        val hasExact = subType.isTypeVariableWithExact() || superType.isTypeVariableWithExact()

        // we should strip annotation's because we have incorporation operation and they should be not affected
        val mySubType =
            if (hasExact) extractTypeForProjectedType(subType, out = true)
                ?: with(extensionTypeContext) { subType.removeExactAnnotation() } else subType
        val mySuperType =
            if (hasExact) extractTypeForProjectedType(superType, out = false)
                ?: with(extensionTypeContext) { superType.removeExactAnnotation() } else superType

        val result = internalAddSubtypeConstraint(mySubType, mySuperType, isFromNullabilityConstraint)
        if (!hasExact) return result

        val result2 = internalAddSubtypeConstraint(mySuperType, mySubType, isFromNullabilityConstraint)

        if (result == null && result2 == null) return null
        return (result ?: true) && (result2 ?: true)
    }

    private fun extractTypeForProjectedType(type: KotlinTypeMarker, out: Boolean): KotlinTypeMarker? = with(extensionTypeContext) {
        val rigidType = type.asRigidType()
        val typeMarker = rigidType?.asCapturedTypeUnwrappingDnn() ?: return null

        val projection = typeMarker.typeConstructorProjection()

        if (projection.isStarProjection()) {
            return when (out) {
                true -> rigidType.typeConstructor().supertypes().let {
                    if (it.isEmpty())
                        nullableAnyType()
                    else
                        intersectTypes(it.toList())
                }
                false -> typeMarker.lowerType()
            }
        }

        return when (projection.getVariance()) {
            TypeVariance.IN -> if (!out) typeMarker.lowerType() ?: projection.getType() else null
            TypeVariance.OUT -> if (out) projection.getType() else null
            TypeVariance.INV -> null
        }
    }

    private fun KotlinTypeMarker.isTypeVariableWithExact() =
        with(extensionTypeContext) { hasExactAnnotation() } && anyBound(this@TypeCheckerStateForConstraintSystem::isMyTypeVariable)

    private fun KotlinTypeMarker.isTypeVariableWithNoInfer() =
        with(extensionTypeContext) { hasNoInferAnnotation() } && anyBound(this@TypeCheckerStateForConstraintSystem::isMyTypeVariable)

    private fun internalAddSubtypeConstraint(
        subType: KotlinTypeMarker,
        superType: KotlinTypeMarker,
        isFromNullabilityConstraint: Boolean
    ): Boolean? {
        assertInputTypes(subType, superType)

        var answer: Boolean? = null

        if (superType.anyBound(this::isMyTypeVariable)) {
            answer = simplifyLowerConstraint(superType, subType, isFromNullabilityConstraint)
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
    private fun extractTypeVariableForSubtype(subType: KotlinTypeMarker, superType: KotlinTypeMarker): KotlinTypeMarker? =
        with(extensionTypeContext) {

            val typeMarker = subType.asRigidType()?.asCapturedTypeUnwrappingDnn() ?: return null

            val projection = typeMarker.typeConstructorProjection()
            if (projection.isStarProjection()) return null
            if (projection.getVariance() == TypeVariance.IN) {
                val type = projection.getType()?.asRigidType() ?: return null
                if (isMyTypeVariable(type)) {
                    simplifyLowerConstraint(type, superType)
                    if (isMyTypeVariable(superType.asRigidType() ?: return null)) {
                        addLowerConstraint(superType.typeConstructor(), nullableAnyType())
                    }
                }
                return null
            }

            return if (projection.getVariance() == TypeVariance.OUT) {
                val type = projection.getType()
                when {
                    type is RigidTypeMarker && isMyTypeVariable(type) -> type
                    type is FlexibleTypeMarker && isMyTypeVariable(type.lowerBound()) -> type.lowerBound()
                    else -> null
                }
            } else null
        }

    /**
     * Foo <: T -- leave as is
     *
     * T?
     *
     * Foo <: T? -- Foo & Any <: T
     * Foo? <: T? -- Foo? & Any <: T -- Foo & Any <: T
     * (Foo..Bar) <: T? -- (Foo..Bar) & Any <: T
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
     *
     * T & Any
     *
     * Foo? <: T & Any => ERROR (for K2 only)
     *
     * Foo..Bar? <: T & Any => Foo..Bar? <: T
     * Foo <: T & Any  => Foo <: T
     */
    private fun simplifyLowerConstraint(
        typeVariable: KotlinTypeMarker,
        subType: KotlinTypeMarker,
        isFromNullabilityConstraint: Boolean = false
    ): Boolean = with(extensionTypeContext) {
        val subTypeConstructor = subType.typeConstructor()
        val lowerConstraint = when (typeVariable) {
            is RigidTypeMarker ->
                when {
                    // Foo? (any type which cannot be used as dispatch receiver because of nullability) <: T & Any => ERROR (for K2 only)
                    isK2 && typeVariable.isDefinitelyNotNullType() && !subTypeConstructor.isTypeVariable() &&
                            !AbstractNullabilityChecker.isSubtypeOfAny(extensionTypeContext, subType) -> {
                        return false
                    }
                    /**
                     * Pre-2.1 logic was the following here:
                     *
                     * `Foo <: T?` (T is contained in invariant or contravariant positions of a return type) --> `Foo <: T`
                     * ```
                     * // Example
                     * fun <T> foo(x: T?): Inv<T> {}
                     * fun <K> main(z: K) { val x = foo(z) }
                     * ```
                     * `Foo <: T?` (T isn't contained there) -- `Foo!! <: T`
                     * ```
                     * // Example:
                     * fun <T> foo(x: T?) {}
                     * fun <K> main(z: K) { foo(z) }
                     * ```
                     * In 2.1 (see [org.jetbrains.kotlin.config.LanguageFeature.InferenceEnhancementsIn21])
                     * we enhanced the logic here, as a conclusion `Foo <: T?` --> `Foo <: T` might be mathematically wrong
                     * (e.g., when Foo is a type parameter with nullable upper bound, and T is a type parameter/variable with not-nullable upper bounds),
                     * thus provoking sometimes an unexpected contradiction,
                     * and it would be more correct always to conclude `Foo!! <: T` instead.
                     * However, such a conclusion would affect the inferred return type in examples like
                     * ```
                     * class Bar<T>(t: T?)
                     * fun <Foo> bar(t: Foo) = Bar(t)
                     * ```
                     *
                     * Here we can infer both `Bar<Foo>` and `Bar<Foo & Any>` without inference problems,
                     * but for us, it's better to infer `Bar<Foo>`, for user convenience and to preserve backward compatibility.
                     * It's exactly the case when we have `Foo <: T?` constraint and have to choose
                     * between `Foo!! <: T` and `Foo <: T`.
                     * While the first conclusion is mathematically correct and the second is not
                     * (it might lead to unexpected contradictions), it's better for us to consider both of them.
                     * That's why we use an inference fork here, to conclude either `Foo <: T` (default conclusion)
                     * or `Foo!! <: T` (secondary conclusion in case the default one provokes contradiction).
                     * A typical example when the default conclusion doesn't work can be found in KT-61227.
                     *
                     * We still infer `Foo!! <: T` without any forking in case
                     * "T isn't contained in invariant or contravariant positions of a return type", as pre-2.1 versions do.
                     */
                    typeVariable.isMarkedNullable() -> {
                        val typeVariableTypeConstructor = typeVariable.typeConstructor()
                        val needToMakeDefNotNull = subTypeConstructor.isTypeVariable() ||
                                typeVariableTypeConstructor !is TypeVariableTypeConstructorMarker ||
                                !typeVariableTypeConstructor.isContainedInInvariantOrContravariantPositions()

                        val resultType = if (needToMakeDefNotNull) {
                            subType.makeDefinitelyNotNullOrNotNull()
                        } else {
                            val notNullType = subType.withNullability(false)
                            if (addForkPointForDifferentDnnAndMarkedNotNullable(
                                    subType, notNullType, typeVariableTypeConstructor, isFromNullabilityConstraint
                                )
                            ) {
                                return true
                            }
                            notNullType
                        }
                        resultType.withCapturedNonNullProjection()
                    }
                    // Foo <: T => Foo <: T
                    else -> subType
                }

            is FlexibleTypeMarker -> {
                assertFlexibleTypeVariable(typeVariable)

                when (subType) {
                    is RigidTypeMarker ->
                        when {
                            useRefinedBoundsForTypeVariableInFlexiblePosition() ->
                                // Foo <: T! -- (Foo!! .. Foo) <: T
                                createTrivialFlexibleTypeOrSelf(
                                    subType.makeDefinitelyNotNullOrNotNull(),
                                )
                            // In K1 (FE1.0), there is an obsolete behavior
                            subType.isMarkedNullable() -> subType
                            else -> createTrivialFlexibleTypeOrSelf(subType)
                        }

                    is FlexibleTypeMarker ->
                        when {
                            useRefinedBoundsForTypeVariableInFlexiblePosition() ->
                                // (Foo..Bar) <: T! -- (Foo!! .. Bar?) <: T
                                createFlexibleType(
                                    subType.lowerBound().makeDefinitelyNotNullOrNotNull(),
                                    subType.upperBound().withNullability(true)
                                )
                            else ->
                                // (Foo..Bar) <: T! -- (Foo!! .. Bar) <: T
                                makeLowerBoundDefinitelyNotNullOrNotNull(subType)
                        }

                    else -> error("sealed")
                }
            }
            else -> error("sealed")
        }

        addLowerConstraint(typeVariable.typeConstructor(), lowerConstraint, isFromNullabilityConstraint)

        return true
    }

    /**
     * This function attempts to create a fork point
     * ```
     *               --> (1) Foo <: T
     * Foo <: T? --> |
     *               --> (2) Foo!! <: T
     * ```
     *
     * This is needed for situations like below
     *
     * ```
     * // Example (1)
     * // By default we want to infer Bar<B> here (for backward compatibility & better convenience), we need B <: BT constraint
     * fun <B> goBar(t: B) = Bar(t)
     * class Bar<BT>(t: T?)
     *
     * // Example (2)
     * // But now to avoid type mismatch Bar<B & Any> should be inferred as a return type, so B!! <: BT constraint is needed
     * fun <B> goBar(t: B) = Bar<B & Any>(t)
     * class Bar<BT>(t: BT?)
     * ```
     *
     * The order (1), (2) in a fork is important as we prefer a less-conservative constraint `Foo <: T` to be used.
     *
     * @return true if a fork was created, false otherwise. Does nothing (and returns false) in pre-2.1 versions.
     */
    private fun addForkPointForDifferentDnnAndMarkedNotNullable(
        subType: KotlinTypeMarker,
        notNullSubType: KotlinTypeMarker,
        typeVariableTypeConstructor: TypeConstructorMarker,
        isFromNullabilityConstraint: Boolean,
    ): Boolean = with(extensionTypeContext) {
        if (!languageVersionSettings.supportsFeature(InferenceEnhancementsIn21)) return false

        val dnnSubType = subType.makeDefinitelyNotNullOrNotNull()
        if (dnnSubType == notNullSubType) return false

        runForkingPoint {
            for (variant in listOf(notNullSubType, dnnSubType).map { it.withCapturedNonNullProjection() }) {
                fork {
                    addLowerConstraint(typeVariableTypeConstructor, variant, isFromNullabilityConstraint)
                    true
                }
            }
        }

        return true
    }

    private fun KotlinTypeMarker.withCapturedNonNullProjection(): KotlinTypeMarker =
        if (this is CapturedTypeMarker) {
            // TODO: KT-71134 (consider getting rid of withNotNullProjection)
            with(extensionTypeContext) { withNotNullProjection() }
        } else {
            this
        }

    private fun assertFlexibleTypeVariable(typeVariable: FlexibleTypeMarker) = with(typeSystemContext) {
        assert(typeVariable.lowerBound().typeConstructor() == typeVariable.upperBound().typeConstructor()) {
            "Flexible type variable ($typeVariable) should have bounds with the same type constructor, i.e. (T..T?)"
        }
    }

    /**
     * T! <: Foo <=> T <: Foo & Any..Foo?
     * T? <: Foo <=> T <: Foo && Nothing? <: Foo
     * T  <: Foo -- leave as is
     * T & Any <: Foo <=> T <: Foo?
     */
    private fun simplifyUpperConstraint(typeVariable: KotlinTypeMarker, superType: KotlinTypeMarker): Boolean = with(extensionTypeContext) {
        val typeVariableLowerBound = typeVariable.lowerBoundIfFlexible()

        val simplifiedSuperType = when {
            typeVariable.isFlexible() && useRefinedBoundsForTypeVariableInFlexiblePosition() ->
                createFlexibleType(
                    superType.lowerBoundIfFlexible().makeDefinitelyNotNullOrNotNull(),
                    superType.upperBoundIfFlexible().withNullability(true)
                )

            typeVariableLowerBound.isDefinitelyNotNullType() -> {
                superType.withNullability(true)
            }

            typeVariable.isFlexible() && superType is RigidTypeMarker ->
                createTrivialFlexibleTypeOrSelf(superType)

            else -> superType
        }

        addUpperConstraint(typeVariableLowerBound.typeConstructor(), simplifiedSuperType)

        if (typeVariableLowerBound.isMarkedNullable()) {
            // here is important that superType is singleClassifierType
            return simplifiedSuperType.anyBound(::isMyTypeVariable) ||
                    isSubtypeOfByTypeChecker(nullableNothingType(), simplifiedSuperType)
        }

        return true
    }

    private fun simplifyConstraintForPossibleIntersectionSubType(subType: KotlinTypeMarker, superType: KotlinTypeMarker): Boolean? =
        with(extensionTypeContext) {
            @Suppress("NAME_SHADOWING")
            val subType = subType.lowerBoundIfFlexible()

            if (!subType.typeConstructor().isIntersection()) return null

            assert(!subType.isMarkedNullable()) { "Intersection type should not be marked nullable!: $subType" }

            // TODO: may be we lose flexibility here
            val subIntersectionTypes = (subType.typeConstructor().supertypes()).map { it.lowerBoundIfFlexible() }

            val typeVariables = subIntersectionTypes.filter(::isMyTypeVariable).takeIf { it.isNotEmpty() } ?: return null
            val notTypeVariables = subIntersectionTypes.filterNot(::isMyTypeVariable)

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
        AbstractTypeChecker.isSubtypeOf(this as TypeCheckerState, subType, superType)

    private fun assertInputTypes(subType: KotlinTypeMarker, superType: KotlinTypeMarker) = with(typeSystemContext) {
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        fun correctSubType(subType: RigidTypeMarker) =
            subType.isSingleClassifierType() || subType.typeConstructor()
                .isIntersection() || isMyTypeVariable(subType) || subType.isError() || subType.isIntegerLiteralType()

        fun correctSuperType(superType: RigidTypeMarker) =
            superType.isSingleClassifierType() || superType.typeConstructor()
                .isIntersection() || isMyTypeVariable(superType) || superType.isError() || superType.isIntegerLiteralType()

        assert(subType.bothBounds(::correctSubType)) {
            "Not singleClassifierType and not intersection subType: $subType"
        }
        assert(superType.bothBounds(::correctSuperType)) {
            "Not singleClassifierType superType: $superType"
        }
    }

    private inline fun KotlinTypeMarker.bothBounds(f: (RigidTypeMarker) -> Boolean) = when (this) {
        is RigidTypeMarker -> f(this)
        is FlexibleTypeMarker -> with(typeSystemContext) { f(lowerBound()) && f(upperBound()) }
        else -> error("sealed")
    }

    private inline fun KotlinTypeMarker.anyBound(f: (RigidTypeMarker) -> Boolean) = when (this) {
        is RigidTypeMarker -> f(this)
        is FlexibleTypeMarker -> with(typeSystemContext) { f(lowerBound()) || f(upperBound()) }
        else -> error("sealed")
    }
}
