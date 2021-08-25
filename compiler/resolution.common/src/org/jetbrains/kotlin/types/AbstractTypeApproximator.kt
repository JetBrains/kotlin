/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator.commonSuperType
import org.jetbrains.kotlin.types.model.*
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractTypeApproximator(
    val ctx: TypeSystemInferenceExtensionContext,
    protected val languageVersionSettings: LanguageVersionSettings,
) : TypeSystemInferenceExtensionContext by ctx {

    private class ApproximationResult(val type: KotlinTypeMarker?)

    private val cacheForIncorporationConfigToSuperDirection = ConcurrentHashMap<KotlinTypeMarker, ApproximationResult>()
    private val cacheForIncorporationConfigToSubtypeDirection = ConcurrentHashMap<KotlinTypeMarker, ApproximationResult>()

    private val referenceApproximateToSuperType: (SimpleTypeMarker, TypeApproximatorConfiguration, Int) -> KotlinTypeMarker?
        get() = this::approximateSimpleToSuperType
    private val referenceApproximateToSubType: (SimpleTypeMarker, TypeApproximatorConfiguration, Int) -> KotlinTypeMarker?
        get() = this::approximateSimpleToSubType

    companion object {
        const val CACHE_FOR_INCORPORATION_MAX_SIZE = 500
    }

    // null means that this input type is the result, i.e. input type not contains not-allowed kind of types
    // type <: resultType
    fun approximateToSuperType(type: KotlinTypeMarker, conf: TypeApproximatorConfiguration): KotlinTypeMarker? =
        approximateToSuperType(type, conf, -type.typeDepth())

    // resultType <: type
    fun approximateToSubType(type: KotlinTypeMarker, conf: TypeApproximatorConfiguration): KotlinTypeMarker? =
        approximateToSubType(type, conf, -type.typeDepth())

    fun clearCache() {
        cacheForIncorporationConfigToSubtypeDirection.clear()
        cacheForIncorporationConfigToSuperDirection.clear()
    }

    private fun checkExceptionalCases(
        type: KotlinTypeMarker, depth: Int, conf: TypeApproximatorConfiguration, toSuper: Boolean
    ): ApproximationResult? {
        return when {
            type.isSpecial() ->
                null.toApproximationResult()

            type.isError() ->
                // todo -- fix builtIns. Now builtIns here is DefaultBuiltIns
                (if (conf.errorType) null else type.defaultResult(toSuper)).toApproximationResult()

            depth > 3 ->
                type.defaultResult(toSuper).toApproximationResult()

            else -> null
        }
    }

    private fun KotlinTypeMarker?.toApproximationResult(): ApproximationResult = ApproximationResult(this)

    private inline fun cachedValue(
        type: KotlinTypeMarker,
        conf: TypeApproximatorConfiguration,
        toSuper: Boolean,
        approximate: () -> KotlinTypeMarker?
    ): KotlinTypeMarker? {
        // Approximator depends on a configuration, so cache should take it into account
        // Here, we cache only types for configuration "from incorporation", which is used most intensively
        if (conf !is TypeApproximatorConfiguration.IncorporationConfiguration) return approximate()

        val cache = if (toSuper) cacheForIncorporationConfigToSuperDirection else cacheForIncorporationConfigToSubtypeDirection

        if (cache.size > CACHE_FOR_INCORPORATION_MAX_SIZE) return approximate()

        return cache.getOrPut(type, { approximate().toApproximationResult() }).type
    }

    private fun approximateToSuperType(type: KotlinTypeMarker, conf: TypeApproximatorConfiguration, depth: Int): KotlinTypeMarker? {
        checkExceptionalCases(type, depth, conf, toSuper = true)?.let { return it.type }

        return cachedValue(type, conf, toSuper = true) {
            approximateTo(
                AbstractTypeChecker.prepareType(ctx, type), conf, { upperBound() },
                referenceApproximateToSuperType, depth
            )
        }
    }

    private fun approximateToSubType(type: KotlinTypeMarker, conf: TypeApproximatorConfiguration, depth: Int): KotlinTypeMarker? {
        checkExceptionalCases(type, depth, conf, toSuper = false)?.let { return it.type }

        return cachedValue(type, conf, toSuper = false) {
            approximateTo(
                AbstractTypeChecker.prepareType(ctx, type), conf, { lowerBound() },
                referenceApproximateToSubType, depth
            )
        }
    }

    // Don't call this method directly, it should be used only in approximateToSuperType/approximateToSubType (use these methods instead)
    // This method contains detailed implementation only for type approximation, it doesn't check exceptional cases and doesn't use cache
    private fun approximateTo(
        type: KotlinTypeMarker,
        conf: TypeApproximatorConfiguration,
        bound: FlexibleTypeMarker.() -> SimpleTypeMarker,
        approximateTo: (SimpleTypeMarker, TypeApproximatorConfiguration, depth: Int) -> KotlinTypeMarker?,
        depth: Int
    ): KotlinTypeMarker? {
        when (type) {
            is SimpleTypeMarker -> return approximateTo(type, conf, depth)
            is FlexibleTypeMarker -> {
                if (type.isDynamic()) {
                    return if (conf.dynamic) null else type.bound()
                } else if (type.asRawType() != null) {
                    return if (conf.rawType) null else type.bound()
                }

//              TODO: Restore check
//              TODO: currently we can lose information about enhancement, should be fixed later
//              assert(type is FlexibleTypeImpl || type is FlexibleTypeWithEnhancement) {
//                  "Unexpected subclass of FlexibleType: ${type::class.java.canonicalName}, type = $type"
//              }

                if (conf.flexible) {
                    /**
                     * Let inputType = L_1..U_1; resultType = L_2..U_2
                     * We should create resultType such as inputType <: resultType.
                     * It means that if A <: inputType, then A <: U_1. And, because inputType <: resultType,
                     * A <: resultType => A <: U_2. I.e. for every type A such A <: U_1, A <: U_2 => U_1 <: U_2.
                     *
                     * Similar for L_1 <: L_2: Let B : resultType <: B. L_2 <: B and L_1 <: B.
                     * I.e. for every type B such as L_2 <: B, L_1 <: B. For example B = L_2.
                     */
                    val lowerBound = type.lowerBound()
                    val upperBound = type.upperBound()

                    val lowerResult = approximateTo(lowerBound, conf, depth)

                    val upperResult = if (type !is RawTypeMarker && lowerBound.typeConstructor() == upperBound.typeConstructor())
                        lowerResult?.withNullability(upperBound.isMarkedNullable())
                    else
                        approximateTo(upperBound, conf, depth)
                    if (lowerResult == null && upperResult == null) return null

                    /**
                     * If C <: L..U then C <: L.
                     * inputType.lower <: lowerResult => inputType.lower <: lowerResult?.lowerIfFlexible()
                     * i.e. this type is correct. We use this type, because this type more flexible.
                     *
                     * If U_1 <: U_2.lower .. U_2.upper, then we know only that U_1 <: U_2.upper.
                     */
                    return createFlexibleType(
                        lowerResult?.lowerBoundIfFlexible() ?: lowerBound,
                        upperResult?.upperBoundIfFlexible() ?: upperBound
                    )
                } else {
                    return type.bound().let { approximateTo(it, conf, depth) ?: it }
                }
            }
            else -> error("sealed")
        }
    }

    private fun approximateLocalTypes(type: SimpleTypeMarker, conf: TypeApproximatorConfiguration, toSuper: Boolean): SimpleTypeMarker? {
        if (!toSuper) return null
        if (!conf.localTypes) return null
        val constructor = type.typeConstructor()
        val needApproximate = conf.localTypes && constructor.isLocalType()
        if (!needApproximate) return null
        val superConstructor = constructor.supertypes().first().typeConstructor()
        val typeCheckerContext = newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )
        return AbstractTypeChecker.findCorrespondingSupertypes(typeCheckerContext, type, superConstructor).first()
            .withNullability(type.isMarkedNullable())
    }

    private fun isIntersectionTypeEffectivelyNothing(constructor: IntersectionTypeConstructorMarker): Boolean {
        // We consider intersection as Nothing only if one of it's component is a primitive number type
        // It's intentional we're not trying to prove population of some type as it was in OI

        return constructor.supertypes().any {
            !it.isMarkedNullable() && it.isSignedOrUnsignedNumberType()
        }
    }

    private fun approximateIntersectionType(
        type: SimpleTypeMarker,
        conf: TypeApproximatorConfiguration,
        toSuper: Boolean,
        depth: Int
    ): KotlinTypeMarker? {
        val typeConstructor = type.typeConstructor()
        assert(typeConstructor.isIntersection()) {
            "Should be intersection type: $type, typeConstructor class: ${typeConstructor::class.java.canonicalName}"
        }
        assert(typeConstructor.supertypes().isNotEmpty()) {
            "Supertypes for intersection type should not be empty: $type"
        }

        var thereIsApproximation = false
        val newTypes = typeConstructor.supertypes().map {
            val newType = if (toSuper) approximateToSuperType(it, conf, depth) else approximateToSubType(it, conf, depth)
            if (newType != null) {
                thereIsApproximation = true
                newType
            } else it
        }

        /**
         * For case ALLOWED:
         * A <: A', B <: B' => A & B <: A' & B'
         *
         * For other case -- it's impossible to find some type except Nothing as subType for intersection type.
         */
        val baseResult = when (conf.intersection) {
            TypeApproximatorConfiguration.IntersectionStrategy.ALLOWED -> if (!thereIsApproximation) return null else intersectTypes(newTypes)
            TypeApproximatorConfiguration.IntersectionStrategy.TO_FIRST -> if (toSuper) newTypes.first() else return type.defaultResult(toSuper = false)
            // commonSupertypeCalculator should handle flexible types correctly
            TypeApproximatorConfiguration.IntersectionStrategy.TO_COMMON_SUPERTYPE,
            TypeApproximatorConfiguration.IntersectionStrategy.TO_UPPER_BOUND_IF_SUPERTYPE -> {
                if (!toSuper) return type.defaultResult(toSuper = false)
                val resultType = commonSuperType(newTypes)
                approximateToSuperType(resultType, conf) ?: resultType
            }
        }

        return if (type.isMarkedNullable()) baseResult.withNullability(true) else baseResult
    }

    private fun approximateCapturedType(
        type: CapturedTypeMarker,
        conf: TypeApproximatorConfiguration,
        toSuper: Boolean,
        depth: Int
    ): KotlinTypeMarker? {
        val supertypes = type.typeConstructor().supertypes()
        val baseSuperType = when (supertypes.size) {
            0 -> nullableAnyType() // Let C = in Int, then superType for C and C? is Any?
            1 -> supertypes.single()

            // Consider the following example:
            // A.getA()::class.java, where `getA()` returns some class from Java
            // From `::class` we are getting type KClass<Cap<out A!>>, where Cap<out A!> have two supertypes:
            // - Any (from declared upper bound of type parameter for KClass)
            // - (A..A?) -- from A!, projection type of captured type

            // Now, after approximation we were getting type `KClass<out A>`, because { Any & (A..A?) } = A,
            // but in old inference type was equal to `KClass<out A!>`.

            // Important note that from the point of type system first type is more specific:
            // Here, approximation of KClass<Cap<out A!>> is a type KClass<T> such that KClass<Cap<out A!>> <: KClass<out T> =>
            // So, the the more specific type for T would be "some non-null (because of declared upper bound type) subtype of A", which is `out A`

            // But for now, to reduce differences in behaviour of old and new inference, we'll approximate such types to `KClass<out A!>`

            // Once NI will be more stabilized, we'll use more specific type

            else -> {
                val projection = type.typeConstructorProjection()
                if (projection.isStarProjection()) intersectTypes(supertypes.toList())
                else projection.getType()
            }
        }
        val baseSubType = type.lowerType() ?: nothingType()

        if (!conf.capturedType(ctx, type)) {
            /**
             * Here everything is ok if bounds for this captured type should not be approximated.
             * But. If such bounds contains some unauthorized types, then we cannot leave this captured type "as is".
             * And we cannot create new capture type, because meaning of new captured type is not clear.
             * So, we will just approximate such types
             *
             * todo handle flexible types
             */
            if (approximateToSuperType(baseSuperType, conf, depth) == null && approximateToSubType(baseSubType, conf, depth) == null) {
                return null
            }
        }
        val baseResult = if (toSuper) approximateToSuperType(baseSuperType, conf, depth) ?: baseSuperType else approximateToSubType(
            baseSubType,
            conf,
            depth
        ) ?: baseSubType

        // C = in Int, Int <: C => Int? <: C?
        // C = out Number, C <: Number => C? <: Number?
        return when {
            type.isMarkedNullable() -> baseResult.withNullability(true)
            type.isProjectionNotNull() -> baseResult.withNullability(false)
            else -> baseResult
        }
    }

    private fun approximateSimpleToSuperType(type: SimpleTypeMarker, conf: TypeApproximatorConfiguration, depth: Int) =
        approximateTo(type, conf, toSuper = true, depth = depth)

    private fun approximateSimpleToSubType(type: SimpleTypeMarker, conf: TypeApproximatorConfiguration, depth: Int) =
        approximateTo(type, conf, toSuper = false, depth = depth)

    private fun approximateTo(
        type: SimpleTypeMarker,
        conf: TypeApproximatorConfiguration,
        toSuper: Boolean,
        depth: Int
    ): KotlinTypeMarker? {
        if (type.argumentsCount() != 0) {
            return approximateParametrizedType(type, conf, toSuper, depth + 1)
        }

        val definitelyNotNullType = type.asDefinitelyNotNullType()
        if (definitelyNotNullType != null) {
            return approximateDefinitelyNotNullType(definitelyNotNullType, conf, toSuper, depth)
        }

        val typeConstructor = type.typeConstructor()

        if (typeConstructor.isCapturedTypeConstructor()) {
            val capturedType = type.asCapturedType()
            require(capturedType != null) {
                // KT-16147
                "Type is inconsistent -- somewhere we create type with typeConstructor = $typeConstructor " +
                        "and class: ${type::class.java.canonicalName}. type.toString() = $type"
            }
            return approximateCapturedType(capturedType, conf, toSuper, depth)
        }

        if (typeConstructor.isIntersection()) {
            return approximateIntersectionType(type, conf, toSuper, depth)
        }

        if (typeConstructor is TypeVariableTypeConstructorMarker) {
            return if (conf.typeVariable(typeConstructor)) null else type.defaultResult(toSuper)
        }

        if (typeConstructor.isIntegerLiteralTypeConstructor()) {
            return if (conf.integerLiteralType)
                typeConstructor.getApproximatedIntegerLiteralType().withNullability(type.isMarkedNullable())
            else
                null
        }

        return approximateLocalTypes(type, conf, toSuper) // simple classifier type
    }

    private fun approximateDefinitelyNotNullType(
        type: DefinitelyNotNullTypeMarker,
        conf: TypeApproximatorConfiguration,
        toSuper: Boolean,
        depth: Int
    ): KotlinTypeMarker? {
        val originalType = type.original()
        val approximatedOriginalType =
            if (toSuper) approximateToSuperType(originalType, conf, depth) else approximateToSubType(originalType, conf, depth)
        val typeWithErasedNullability = originalType.withNullability(false)

        // Approximate T!! into T if T is already not-null (has not-null upper bounds)
        if (originalType.typeConstructor().isTypeParameterTypeConstructor() && !typeWithErasedNullability.isNullableType()) {
            return typeWithErasedNullability
        }

        return if (conf.definitelyNotNullType || languageVersionSettings.supportsFeature(LanguageFeature.DefinitelyNotNullTypeParameters)) {
            approximatedOriginalType?.makeDefinitelyNotNullOrNotNull()
        } else {
            if (toSuper)
                (approximatedOriginalType ?: originalType).withNullability(false)
            else
                type.defaultResult(toSuper)
        }
    }

    private fun isApproximateDirectionToSuper(effectiveVariance: TypeVariance, toSuper: Boolean) =
        when (effectiveVariance) {
            TypeVariance.OUT -> toSuper
            TypeVariance.IN -> !toSuper
            TypeVariance.INV -> throw AssertionError("Incorrect variance $effectiveVariance")
        }

    private fun approximateParametrizedType(
        type: SimpleTypeMarker,
        conf: TypeApproximatorConfiguration,
        toSuper: Boolean,
        depth: Int
    ): SimpleTypeMarker? {
        val typeConstructor = type.typeConstructor()
        if (typeConstructor.parametersCount() != type.argumentsCount()) {
            return if (conf.errorType) {
                createErrorType("Inconsistent type: $type (parameters.size = ${typeConstructor.parametersCount()}, arguments.size = ${type.argumentsCount()})")
            } else type.defaultResult(toSuper)
        }

        val newArguments = arrayOfNulls<TypeArgumentMarker?>(type.argumentsCount())

        loop@ for (index in 0 until type.argumentsCount()) {
            val parameter = typeConstructor.getParameter(index)
            val argument = type.getArgument(index)

            if (argument.isStarProjection()) continue

            val effectiveVariance = AbstractTypeChecker.effectiveVariance(parameter.getVariance(), argument.getVariance())

            val argumentType = newArguments[index]?.getType() ?: argument.getType()

            val capturedType = argumentType.lowerBoundIfFlexible().asCapturedType()
            val capturedStarProjectionOrNull =
                capturedType?.typeConstructorProjection()?.takeIf { it.isStarProjection() }

            if (capturedStarProjectionOrNull != null &&
                (effectiveVariance == TypeVariance.OUT || effectiveVariance == TypeVariance.INV) &&
                toSuper &&
                capturedType.typeParameter() == parameter
            ) {
                newArguments[index] = capturedStarProjectionOrNull
                continue@loop
            }

            when (effectiveVariance) {
                null -> {
                    return if (conf.errorType) {
                        createErrorType(
                            "Inconsistent type: $type ($index parameter has declared variance: ${parameter.getVariance()}, " +
                                    "but argument variance is ${argument.getVariance()})"
                        )
                    } else type.defaultResult(toSuper)
                }
                TypeVariance.OUT, TypeVariance.IN -> {
                    if (
                        conf.intersectionTypesInContravariantPositions &&
                        effectiveVariance == TypeVariance.IN &&
                        argumentType.typeConstructor().isIntersection()
                    ) {
                        val argumentTypeConstructor = argumentType.typeConstructor()
                        if (argumentTypeConstructor.isIntersection() && isIntersectionTypeEffectivelyNothing(argumentTypeConstructor as IntersectionTypeConstructorMarker)) {
                            newArguments[index] = createStarProjection(parameter)
                            continue@loop
                        }
                    }

                    /**
                     * Out<Foo> <: Out<superType(Foo)>
                     * Inv<out Foo> <: Inv<out superType(Foo)>

                     * In<Foo> <: In<subType(Foo)>
                     * Inv<in Foo> <: Inv<in subType(Foo)>
                     */
                    val approximatedArgument = if (isApproximateDirectionToSuper(effectiveVariance, toSuper)) {
                        val approximatedType = approximateToSuperType(argumentType, conf, depth)
                        if (conf.intersection == TypeApproximatorConfiguration.IntersectionStrategy.TO_UPPER_BOUND_IF_SUPERTYPE
                            && argumentType.typeConstructor().isIntersection()
                            && parameter.getUpperBounds().all { AbstractTypeChecker.isSubtypeOf(ctx, argumentType, it) }
                        ) {
                            val intersectedUpperBounds = intersectTypes(parameter.getUpperBounds())
                            if (approximatedType == null
                                || !AbstractTypeChecker.isSubtypeOf(ctx, approximatedType, intersectedUpperBounds)
                            ) {
                                intersectedUpperBounds
                            } else {
                                approximatedType
                            }
                        } else {
                            approximatedType ?: continue@loop
                        }
                    } else {
                        approximateToSubType(argumentType, conf, depth) ?: continue@loop
                    }

                    if (
                        conf.intersection != TypeApproximatorConfiguration.IntersectionStrategy.ALLOWED &&
                        effectiveVariance == TypeVariance.OUT &&
                        argumentType.typeConstructor().isIntersection()
                    ) {
                        var shouldReplaceWithStar = false
                        for (upperBoundIndex in 0 until parameter.upperBoundCount()) {
                            if (!AbstractTypeChecker.isSubtypeOf(ctx, approximatedArgument, parameter.getUpperBound(upperBoundIndex))) {
                                shouldReplaceWithStar = true
                                break
                            }
                        }
                        if (shouldReplaceWithStar) {
                            newArguments[index] = createStarProjection(parameter)
                            continue@loop
                        }
                    }

                    if (parameter.getVariance() == TypeVariance.INV) {
                        newArguments[index] = createTypeArgument(approximatedArgument, effectiveVariance)
                    } else {
                        newArguments[index] = approximatedArgument.asTypeArgument()
                    }
                }
                TypeVariance.INV -> {
                    if (!toSuper) {
                        // Inv<Foo> cannot be approximated to subType
                        val toSubType = approximateToSubType(argumentType, conf, depth) ?: continue@loop

                        // Inv<Foo!> is supertype for Inv<Foo?>
                        if (!AbstractTypeChecker.equalTypes(
                                this,
                                argumentType,
                                toSubType
                            )
                        ) return type.defaultResult(toSuper)

                        // also Captured(out Nothing) = Nothing
                        newArguments[index] = toSubType.asTypeArgument()
                        continue@loop
                    }

                    /**
                     * Example with non-trivial both type approximations:
                     * Inv<In<C>> where C = in Int
                     * Inv<In<C>> <: Inv<out In<Int>>
                     * Inv<In<C>> <: Inv<in In<Any?>>
                     *
                     * So such case is rare and we will chose Inv<out In<Int>> for now.
                     *
                     * Note that for case Inv<C> we will chose Inv<in Int>, because it is more informative then Inv<out Any?>.
                     * May be we should do the same for deeper types, but not now.
                     */
                    if (argumentType.typeConstructor().isCapturedTypeConstructor()) {
                        val subType = approximateToSubType(argumentType, conf, depth) ?: continue@loop
                        if (!subType.isTrivialSub()) {
                            newArguments[index] = createTypeArgument(subType, TypeVariance.IN)
                            continue@loop
                        }
                    }

                    val approximatedSuperType =
                        approximateToSuperType(argumentType, conf, depth) ?: continue@loop // null means that this type we can leave as is
                    if (approximatedSuperType.isTrivialSuper()) {
                        val approximatedSubType =
                            approximateToSubType(argumentType, conf, depth) ?: continue@loop // seems like this is never null
                        if (!approximatedSubType.isTrivialSub()) {
                            newArguments[index] = createTypeArgument(approximatedSubType, TypeVariance.IN)
                            continue@loop
                        }
                    }

                    if (AbstractTypeChecker.equalTypes(this, argumentType, approximatedSuperType)) {
                        newArguments[index] = approximatedSuperType.asTypeArgument()
                    } else {
                        newArguments[index] = createTypeArgument(approximatedSuperType, TypeVariance.OUT)
                    }
                }
            }
        }

        if (newArguments.all { it == null }) return approximateLocalTypes(type, conf, toSuper)

        val newArgumentsList = List(type.argumentsCount()) { index -> newArguments[index] ?: type.getArgument(index) }
        val approximatedType = type.replaceArguments(newArgumentsList)
        return approximateLocalTypes(approximatedType, conf, toSuper) ?: approximatedType
    }

    private fun KotlinTypeMarker.defaultResult(toSuper: Boolean) = if (toSuper) nullableAnyType() else {
        if (this is SimpleTypeMarker && isMarkedNullable()) nullableNothingType() else nothingType()
    }

    // Any? or Any!
    private fun KotlinTypeMarker.isTrivialSuper() = upperBoundIfFlexible().isNullableAny()

    // Nothing or Nothing!
    private fun KotlinTypeMarker.isTrivialSub() = lowerBoundIfFlexible().isNothing()

    override fun CapturedTypeMarker.typeParameter(): TypeParameterMarker? {
        with(ctx) {
            return typeParameter()
        }
    }
}
