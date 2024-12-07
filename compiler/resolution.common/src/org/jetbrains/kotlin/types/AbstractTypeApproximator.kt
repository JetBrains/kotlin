/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator.commonSuperType
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractTypeApproximator(
    val ctx: TypeSystemInferenceExtensionContext,
    protected val languageVersionSettings: LanguageVersionSettings,
) : TypeSystemInferenceExtensionContext by ctx {

    private class ApproximationResult(val type: KotlinTypeMarker?)

    private val cacheForIncorporationConfigToSuperDirection = ConcurrentHashMap<KotlinTypeMarker, ApproximationResult>()
    private val cacheForIncorporationConfigToSubtypeDirection = ConcurrentHashMap<KotlinTypeMarker, ApproximationResult>()

    private val referenceApproximateToSuperType: (RigidTypeMarker, TypeApproximatorConfiguration, Int) -> KotlinTypeMarker?
        get() = this::approximateSimpleToSuperType
    private val referenceApproximateToSubType: (RigidTypeMarker, TypeApproximatorConfiguration, Int) -> KotlinTypeMarker?
        get() = this::approximateSimpleToSubType

    companion object {
        const val CACHE_FOR_INCORPORATION_MAX_SIZE = 500
    }

    // null means that this input type is the result, i.e. input type not contains not-allowed kind of types
    // type <: resultType
    fun approximateToSuperType(type: KotlinTypeMarker, conf: TypeApproximatorConfiguration): KotlinTypeMarker? =
        approximateToSuperType(type, conf, -type.typeDepthForApproximation())

    // resultType <: type
    fun approximateToSubType(type: KotlinTypeMarker, conf: TypeApproximatorConfiguration): KotlinTypeMarker? =
        approximateToSubType(type, conf, -type.typeDepthForApproximation())

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
                (if (!conf.approximateErrorTypes) null else type.defaultResult(toSuper)).toApproximationResult()

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
                AbstractTypeChecker.prepareType(ctx, type),
                conf,
                { upperBound() },
                referenceApproximateToSuperType,
                depth
            )
        }
    }

    private fun approximateToSubType(type: KotlinTypeMarker, conf: TypeApproximatorConfiguration, depth: Int): KotlinTypeMarker? {
        checkExceptionalCases(type, depth, conf, toSuper = false)?.let { return it.type }

        return cachedValue(type, conf, toSuper = false) {
            approximateTo(
                AbstractTypeChecker.prepareType(ctx, type),
                conf,
                { lowerBound() },
                referenceApproximateToSubType,
                depth
            )
        }
    }

    // Don't call this method directly, it should be used only in approximateToSuperType/approximateToSubType (use these methods instead)
    // This method contains detailed implementation only for type approximation, it doesn't check exceptional cases and doesn't use cache
    private fun approximateTo(
        type: KotlinTypeMarker,
        conf: TypeApproximatorConfiguration,
        bound: FlexibleTypeMarker.() -> RigidTypeMarker,
        approximateTo: (RigidTypeMarker, TypeApproximatorConfiguration, depth: Int) -> KotlinTypeMarker?,
        depth: Int
    ): KotlinTypeMarker? {
        when (type) {
            is RigidTypeMarker -> return approximateTo(type, conf, depth)
            is FlexibleTypeMarker -> {
                if (type.isDynamic()) {
                    return if (!conf.approximateDynamic) null else type.bound()
                } else if (type.isRawType()) {
                    return if (!conf.approximateRawTypes) null else type.bound()
                }

//              TODO: Restore check
//              TODO: currently we can lose information about enhancement, should be fixed later
//              assert(type is FlexibleTypeImpl || type is FlexibleTypeWithEnhancement) {
//                  "Unexpected subclass of FlexibleType: ${type::class.java.canonicalName}, type = $type"
//              }

                if (!conf.approximateFlexible) {
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

                    val upperResult = if (!type.isRawType() && !shouldApproximateUpperBoundSeparately(lowerBound, upperBound, conf)) {
                        // We skip approximating the upper bound if the type constructors match as an optimization.
                        lowerResult?.withNullability(upperBound.isMarkedNullable())
                    } else {
                        approximateTo(upperBound, conf, depth)
                    }
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

    private fun shouldApproximateUpperBoundSeparately(
        lowerBound: RigidTypeMarker,
        upperBound: RigidTypeMarker,
        conf: TypeApproximatorConfiguration,
    ): Boolean {
        val upperBoundConstructor = upperBound.typeConstructor()
        if (lowerBound.typeConstructor() != upperBoundConstructor) return true

        // Flexible arrays have the shape `Array<X>..Array<out X>?`.
        // When such a type is captured, it results in `Array<X>..Array<Captured(out X)>?`, therefore it's necessary to approximate the
        // upper bound separately.
        // As an important performance optimization, we explicitly check if the type in question is an array with a captured type argument
        // that needs to be approximated.
        // This saves us from doing twice the work unnecessarily in many cases.
        return isK2 &&
                upperBoundConstructor.isArrayConstructor() &&
                upperBound.getArgumentOrNull(0).let { it is CapturedTypeMarker && conf.shouldApproximateCapturedType(ctx, it) }
    }

    private fun approximateLocalTypes(
        type: RigidTypeMarker,
        conf: TypeApproximatorConfiguration,
        toSuper: Boolean,
        depth: Int,
    ): RigidTypeMarker? {
        if (!toSuper) return null
        if (!conf.approximateLocalTypes && !conf.approximateAnonymous) return null

        fun TypeConstructorMarker.isAcceptable(conf: TypeApproximatorConfiguration): Boolean {
            return !(conf.approximateLocalTypes && isLocalType()) && !(conf.approximateAnonymous && isAnonymous())
        }

        val constructor = type.typeConstructor()
        if (constructor.isAcceptable(conf)) return null
        val typeCheckerContext = newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )

        var result: RigidTypeMarker? = null

        if (isK2) {
            // BFS for non-local/anonymous supertype:
            // search for non-local supertypes in the super types of the given type,
            // then it their respective super types, etc.
            // Ignore `Any`.
            // If no suitable type is found, return `Any`.
            val visited = mutableSetOf<RigidTypeMarker>()
            val queue = ArrayDeque<RigidTypeMarker>().apply { add(type) }

            while (queue.isNotEmpty()) {
                val currentType = queue.removeFirst()
                if (!visited.add(currentType)) continue
                val currentConstructor = currentType.typeConstructor()
                if (currentConstructor.isAcceptable(conf)) {
                    result = currentType.withNullability(type.isMarkedNullable())
                    break
                }
                currentConstructor.supertypes()
                    .flatMap { AbstractTypeChecker.findCorrespondingSupertypes(typeCheckerContext, type, it.typeConstructor()) }
                    .filterTo(queue) { !it.typeConstructor().isAnyConstructor() }
            }

            if (result == null) {
                result = ctx.anyType().withNullability(type.isMarkedNullable())
            }
        } else {
            val superConstructor = constructor.supertypes().first().typeConstructor()
            result = AbstractTypeChecker.findCorrespondingSupertypes(typeCheckerContext, type, superConstructor)
                .firstOrNull()
                ?.withNullability(type.isMarkedNullable())
        }

        if (result == null) return null

        /*
         * AbstractTypeChecker captures any projections in the super type by default, which may lead to the situation, when some local
         *   type with projection is approximated to some public type with captured (from subtyping) type argument (which is obviously
         *   incorrect)
         *
         * interface Invariant<A>
         * private fun <B> Invariant<B>.privateFunc() = object : Invariant<B> {}
         *
         * fun Invariant<in Number>.publicFunc() = privateFunc()
         *
         * Here type of `privateFunc()` is _anonymous_<in Number>, and `findCorrespondingSupertypes` for it and `Invariant` as type
         *   constructor returns `Invariant<Captured(in Number)>`
         */
        return if (ctx.isK2) {
            (approximateTo(result, TypeApproximatorConfiguration.SubtypeCapturedTypesApproximation, toSuper, depth) ?: result) as RigidTypeMarker?
        } else {
            result
        }
    }

    private fun isIntersectionTypeEffectivelyNothing(constructor: IntersectionTypeConstructorMarker): Boolean {
        // We consider intersection as Nothing only if one of it's component is a primitive number type
        // It's intentional we're not trying to prove population of some type as it was in OI

        return constructor.supertypes().any {
            !it.isMarkedNullable() && it.isSignedOrUnsignedNumberType()
        }
    }

    private fun approximateIntersectionType(
        type: RigidTypeMarker,
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

        val upperBoundForApproximation = type.getUpperBoundForApproximationOfIntersectionType()

        if (toSuper && upperBoundForApproximation != null &&
            (conf.intersectionStrategy == TypeApproximatorConfiguration.IntersectionStrategy.TO_COMMON_SUPERTYPE ||
                    conf.intersectionStrategy == TypeApproximatorConfiguration.IntersectionStrategy.TO_UPPER_BOUND_IF_SUPERTYPE)
        ) {
            return approximateToSuperType(upperBoundForApproximation, conf, depth) ?: upperBoundForApproximation
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
        val baseResult = when (conf.intersectionStrategy) {
            TypeApproximatorConfiguration.IntersectionStrategy.ALLOWED -> if (!thereIsApproximation) {
                return null
            } else {
                intersectTypes(newTypes, upperBoundForApproximation, toSuper, conf, depth)
            }
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

    private fun intersectTypes(
        newTypes: List<KotlinTypeMarker>,
        upperBoundForApproximation: KotlinTypeMarker?,
        toSuper: Boolean,
        conf: TypeApproximatorConfiguration,
        depth: Int,
    ): KotlinTypeMarker {
        val intersectionType = intersectTypes(newTypes)

        if (upperBoundForApproximation == null) {
            return intersectionType
        }

        val alternativeTypeApproximated = if (toSuper) {
            approximateToSuperType(upperBoundForApproximation, conf, depth)
        } else {
            approximateToSubType(upperBoundForApproximation, conf, depth)
        } ?: upperBoundForApproximation

        return createTypeWithUpperBoundForIntersectionResult(intersectionType, alternativeTypeApproximated)
    }

    private fun approximateCapturedType(
        capturedType: CapturedTypeMarker,
        conf: TypeApproximatorConfiguration,
        toSuper: Boolean,
        depth: Int,
    ): KotlinTypeMarker? {
        val supertypes = capturedType.typeConstructor().supertypes()
        val baseSuperType = when (supertypes.size) {
            0 -> nullableAnyType() // Let C = in Int, then superType for C and C? is Any?
            1 -> supertypes.single().replaceRecursionWithStarProjection(capturedType)

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
                val projection = capturedType.typeConstructorProjection()
                if (projection.isStarProjection()) intersectTypes(supertypes.map { it.replaceRecursionWithStarProjection(capturedType) })
                else projection.getType()!!
            }
        }
        val baseSubType = capturedType.lowerType() ?: nothingType()

        val approximatedSuperType by lazy(LazyThreadSafetyMode.NONE) {
            approximateToSuperType(baseSuperType, conf, depth)
        }
        val approximatedSubType by lazy(LazyThreadSafetyMode.NONE) { approximateToSubType(baseSubType, conf, depth) }

        if (!conf.shouldApproximateCapturedType(ctx, capturedType)) {
            /**
             * Here everything is ok if bounds for this captured type should not be approximated.
             * But. If such bounds contains some unauthorized types, then we cannot leave this captured type "as is".
             * And we cannot create new capture type, because meaning of new captured type is not clear.
             * So, we will just approximate such types
             *
             * TODO remove workaround when we can create captured types with external identity KT-65228.
             * todo handle flexible types
             */
            if (approximatedSuperType == null && approximatedSubType == null) {
                return null
            }
        }
        val baseResult = if (toSuper) approximatedSuperType ?: baseSuperType else approximatedSubType ?: baseSubType

        // C = in Int, Int <: C => Int? <: C?
        // C = out Number, C <: Number => C? <: Number?
        return when {
            capturedType.isMarkedNullable() -> baseResult.withNullability(true)
            capturedType.isProjectionNotNull() -> baseResult.withNullability(false)
            else -> baseResult
        }.let {
            when {
                // This is just a hack that is necessary to preserve compatibility with K1 where return type of the calls
                // if they contain a captured types with RAW supertype would be approximated to a regular non-raw flexible type
                // See CapturedTypeApproximationKt.approximateCapturedTypes and especially the comment
                // "// tod*: dynamic & raw type?" before it :)
                // If we don't repeat that behavior, we would stumble upon KT-56616 with hardly having any workarounds.
                isK2 && conf.convertToNonRawVersionAfterApproximationInK2 && it.isRawType() -> {
                    it.convertToNonRaw()
                }
                else -> it
            }
        }
    }

    private fun KotlinTypeMarker.replaceRecursionWithStarProjection(capturedType: CapturedTypeMarker): KotlinTypeMarker {
        // This replacement is important for resolving the code like below in K2.
        //     fun bar(y: FieldOrRef<*>) = y.field
        //     interface FieldOrRef<FF : AbstractField<FF>> { val field: FF }
        //     abstract class AbstractField<out F : AbstractField<F>>
        // During resolving the value parameter y type, K1 also builds a type for a star projection *.
        // See fun TypeParameterDescriptor.starProjectionType(): KotlinType and fun buildStarProjectionTypeByTypeParameters.
        // Thanks to it, K1 builds the star projection type as AbstractField<*> and no other approximation is needed.
        //
        // In turn, K2 never makes such a thing (K2 star projection has no associated type).
        // Instead, it resolves y.field as CapturedType(*) C (see usage one line below),
        // and the constructor of this captured type has a star projection and a supertype of `AbstractField<C>`.
        //
        // Without this replacement, the type approximator currently cannot handle such a situation properly
        // and builds AbstractField<AbstractField<AbstractField<Any?>>>.
        // The check it == type here is intended to find a recursion inside a captured type.
        // A similar replacement for baseSubType looks unnecessary, no hits in the tests.

        fun TypeArgumentMarker.unwrapForComparison(): CapturedTypeMarker? {
            return getType()?.lowerBoundIfFlexible()?.asCapturedTypeUnwrappingDnn()
        }

        return if (isK2 && getArguments().any { it.unwrapForComparison() == capturedType }) {
            replaceArguments {
                when {
                    it.unwrapForComparison() != capturedType -> it
                    // It's possible to use the stub here, because K2 star projection is an object and
                    // in fact this parameter is never used
                    else -> createStarProjection(TypeParameterMarkerStubForK2StarProjection)
                }
            }
        } else this
    }

    private object TypeParameterMarkerStubForK2StarProjection : TypeParameterMarker

    private fun approximateSimpleToSuperType(type: RigidTypeMarker, conf: TypeApproximatorConfiguration, depth: Int) =
        approximateTo(type, conf, toSuper = true, depth = depth)

    private fun approximateSimpleToSubType(type: RigidTypeMarker, conf: TypeApproximatorConfiguration, depth: Int) =
        approximateTo(type, conf, toSuper = false, depth = depth)

    private fun approximateTo(
        type: RigidTypeMarker,
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

        // DNN case is handled above
        require(type is SimpleTypeMarker)
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
            return if (!conf.shouldApproximateTypeVariableBasedType(typeConstructor, isK2)) null else type.defaultResult(toSuper)
        }

        if (typeConstructor.isIntegerLiteralConstantTypeConstructor()) {
            return runIf(conf.approximateIntegerLiteralConstantTypes) {
                // We ensure that expectedTypeForIntegerLiteralType is only used for top-level and possibly flexible ILTs.
                // Otherwise, we can accidentally approximate nested ILTs to wrong types.
                check(conf.expectedTypeForIntegerLiteralType == null || depth <= 0)
                typeConstructor.getApproximatedIntegerLiteralType(conf.expectedTypeForIntegerLiteralType)
                    .withNullability(type.isMarkedNullable())
            }
        }

        if (typeConstructor.isIntegerConstantOperatorTypeConstructor()) {
            return runIf(conf.approximateIntegerConstantOperatorTypes) {
                // We ensure that expectedTypeForIntegerLiteralType is only used for top-level and possibly flexible ILTs.
                // Otherwise, we can accidentally approximate nested ILTs to wrong types.
                check(conf.expectedTypeForIntegerLiteralType == null || depth <= 0)
                typeConstructor.getApproximatedIntegerLiteralType(conf.expectedTypeForIntegerLiteralType)
                    .withNullability(type.isMarkedNullable())
            }
        }

        return approximateLocalTypes(type, conf, toSuper, depth) // simple classifier type
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

        return if (!conf.approximateDefinitelyNotNullTypes || languageVersionSettings.supportsFeature(LanguageFeature.DefinitelyNonNullableTypes)) {
            approximatedOriginalType?.makeDefinitelyNotNullOrNotNull(preserveAttributes = true)
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
        type: RigidTypeMarker,
        conf: TypeApproximatorConfiguration,
        toSuper: Boolean,
        depth: Int
    ): RigidTypeMarker? {
        val typeConstructor = type.typeConstructor()
        if (typeConstructor.parametersCount() != type.argumentsCount()) {
            return if (!conf.approximateErrorTypes) {
                createErrorType(
                    "Inconsistent type: $type (parameters.size = ${typeConstructor.parametersCount()}, arguments.size = ${type.argumentsCount()})",
                    type
                )
            } else type.defaultResult(toSuper)
        }

        val newArguments = arrayOfNulls<TypeArgumentMarker?>(type.argumentsCount())

        loop@ for (index in 0 until type.argumentsCount()) {
            val parameter = typeConstructor.getParameter(index)
            val argument = type.getArgument(index)

            val argumentType = argument.getType() ?: continue

            val effectiveVariance = AbstractTypeChecker.effectiveVariance(parameter.getVariance(), argument.getVariance())

            val capturedType = argumentType.lowerBoundIfFlexible().asCapturedTypeUnwrappingDnn()

            val capturedStarProjectionOrNull =
                capturedType?.typeConstructorProjection()?.takeIf { it.isStarProjection() }

            if (capturedStarProjectionOrNull != null &&
                (effectiveVariance == TypeVariance.OUT || effectiveVariance == TypeVariance.INV) &&
                toSuper &&
                capturedType.typeParameter() == parameter &&
                (!isK2 || conf.shouldApproximateCapturedType(ctx, capturedType))
            ) {
                newArguments[index] = capturedStarProjectionOrNull
                continue@loop
            }

            when (effectiveVariance) {
                null -> {
                    return if (!conf.approximateErrorTypes) {
                        createErrorType(
                            "Inconsistent type: $type ($index parameter has declared variance: ${parameter.getVariance()}, " +
                                    "but argument variance is ${argument.getVariance()})",
                            type
                        )
                    } else type.defaultResult(toSuper)
                }
                TypeVariance.OUT, TypeVariance.IN -> {
                    if (
                        conf.approximateIntersectionTypesInContravariantPositions &&
                        effectiveVariance == TypeVariance.IN &&
                        argumentType.typeConstructor().isIntersection() &&
                        isIntersectionTypeEffectivelyNothing(argumentType.typeConstructor() as IntersectionTypeConstructorMarker)
                    ) {
                        newArguments[index] = createStarProjection(parameter)
                        continue@loop
                    }

                    /**
                     * Out<Foo> <: Out<superType(Foo)>
                     * Inv<out Foo> <: Inv<out superType(Foo)>

                     * In<Foo> <: In<subType(Foo)>
                     * Inv<in Foo> <: Inv<in subType(Foo)>
                     */
                    val approximatedArgument = if (isApproximateDirectionToSuper(effectiveVariance, toSuper)) {
                        val approximatedType = approximateToSuperType(argumentType, conf, depth)
                            ?.makeApproximatedFlexibleNotNullIfUpperBoundNotNull(argumentType, parameter, conf)

                        if (conf.intersectionStrategy == TypeApproximatorConfiguration.IntersectionStrategy.TO_UPPER_BOUND_IF_SUPERTYPE
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
                        conf.intersectionStrategy != TypeApproximatorConfiguration.IntersectionStrategy.ALLOWED &&
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

                    // In case of Inv<C> and C = Captured(in Int), we choose Inv<in Int> as resulting approximation
                    // NB: Inv<C> <: Inv<in Int> because Int <: C (as Int is a lower bound of the C captured type)
                    //
                    // That behavior of choosing non-trivial lower bound is crucial when there's also non-trivial upper bound,
                    // like if Inv would be declared as `interface Inv<T : CharSequence>` (see test approximationLeavesNonTrivialLowerBound.kt)
                    //
                    // In that case the next condition after that doesn't help because in case of both non-trivial bounds, it chooses the upper one
                    if (argumentType.typeConstructor().isCapturedTypeConstructor()) {
                        val subType = approximateToSubType(argumentType, conf, depth) ?: continue@loop
                        if (shouldUseSubTypeForCapturedArgument(subType, argumentType, conf, depth)) {
                            newArguments[index] = createTypeArgument(subType, TypeVariance.IN)
                            continue@loop
                        }
                    }

                    // Example with non-trivial both type approximations:
                    //  Inv<In<C>> where C = Captured(in Int)
                    //  Inv<In<C>> <: Inv<out In<Int>>
                    //  Inv<In<C>> <: Inv<in In<Any?>>
                    //
                    // So, both of the options are possible, but since such case is rare we will chose Inv<out In<Int>> for now
                    val approximatedSuperType = approximateToSuperType(argumentType, conf, depth)
                        ?.makeApproximatedFlexibleNotNullIfUpperBoundNotNull(argumentType, parameter, conf)
                        ?: continue@loop // null means that this type we can leave as is
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

        if (newArguments.all { it == null }) return approximateLocalTypes(type, conf, toSuper, depth)

        val newArgumentsList = List(type.argumentsCount()) { index -> newArguments[index] ?: type.getArgument(index) }
        val approximatedType = type.replaceArguments(newArgumentsList)
        return approximateLocalTypes(approximatedType, conf, toSuper, depth) ?: approximatedType
    }

    private fun KotlinTypeMarker.makeApproximatedFlexibleNotNullIfUpperBoundNotNull(
        nonApproximatedType: KotlinTypeMarker,
        parameter: TypeParameterMarker,
        conf: TypeApproximatorConfiguration,
    ): KotlinTypeMarker {
        if (!isK2 || !conf.approximateFlexible) {
            return this
        }

        return applyIf(
            nonApproximatedType.isFlexibleOrCapturedWithFlexibleSuperTypes() &&
                    parameter.getUpperBounds().any { b -> !b.isNullableType() }) {
            withNullability(false)
        }
    }

    private fun KotlinTypeMarker.isFlexibleOrCapturedWithFlexibleSuperTypes(): Boolean {
        return hasFlexibleNullability() ||
                (asRigidType()?.asCapturedTypeUnwrappingDnn()?.typeConstructor()?.supertypes()?.all {
                    it.hasFlexibleNullability()
                } == true)
    }

    private fun shouldUseSubTypeForCapturedArgument(
        subType: KotlinTypeMarker,
        capturedArgumentType: KotlinTypeMarker,
        conf: TypeApproximatorConfiguration,
        depth: Int,
    ): Boolean {
        if (subType.isTrivialSub()) return false
        // For K1, the result is always `!subType.isTrivialSub()` (leaving the old behavior)
        if (!isK2) return true

        // Basically, what's written further might be simplified like
        // return !approximateToSubType(capturedArgumentType.withNullability(false), conf, depth)!!.isTrivialSub()
        // But it seems that now it looks a bit more clear and probably performant, thus first two if's are basically fast paths

        // If it's not `Nothing?`, then the lower bound is indeed non-trivial
        if (!subType.lowerBoundIfFlexible().isNullableNothing()) return true

        // Here the subType is `Nothing?`, and it might be trivial only in cause the nullability is caused by nullability of captured type itself

        // If captured type is not marked as nullable, then nullability of subType came from the lower bound of the captured type.
        // Thus, the lower bound is non-trivial for sure
        if (!capturedArgumentType.isMarkedNullable()) return true

        val notMarkedNullableSubType =
            approximateToSubType(capturedArgumentType.withNullability(false), conf, depth)
                ?: error("Not-marked-nullable version of captured type approximation should also return not-null")

        return !notMarkedNullableSubType.isTrivialSub()
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
