/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.builtins.functions.AllowedToUsedOnlyInK1
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator.commonSuperType
import org.jetbrains.kotlin.resolve.calls.inference.model.AssertionsOnly
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.util.concurrent.ConcurrentHashMap

private typealias FunctionTypeForRigidTypeApproximation =
        context(TypeApproximatorConfiguration, AbstractTypeApproximator.Cache) (RigidTypeMarker, Int) -> KotlinTypeMarker?

typealias TypeApproximatorCachesPerConfiguration = MutableMap<TypeApproximatorConfiguration, AbstractTypeApproximator.Cache>

abstract class AbstractTypeApproximator(
    val ctx: TypeSystemInferenceExtensionContext,
    protected val languageVersionSettings: LanguageVersionSettings,
) : TypeSystemInferenceExtensionContext by ctx {

    class ApproximationResult(val type: KotlinTypeMarker?)

    /**
     * With this flag enabled:
     * - Track currently being approximated types, so we could catch recursion instead of using the controversial `depth > 3` condition
     * - Put computed results to relevant caches to reuse them
     * - Mark some places that previously were workarounds for caching/recursion prevention as obsolete
     */
    private val capturedTypeApproximationReworked: Boolean =
        languageVersionSettings.supportsFeature(LanguageFeature.CapturedTypeApproximationReworked)

    // Those caches are only used prior to 2.2 (without CapturedTypeApproximationReworked)
    private val cacheForIncorporationConfigToSuperDirection = ConcurrentHashMap<KotlinTypeMarker, ApproximationResult>()
    private val cacheForIncorporationConfigToSubtypeDirection = ConcurrentHashMap<KotlinTypeMarker, ApproximationResult>()

    private val referenceApproximateToSuperType: FunctionTypeForRigidTypeApproximation
        get() = { type, depth -> approximateSimpleToSuperType(type, depth) }
    private val referenceApproximateToSubType: FunctionTypeForRigidTypeApproximation
        get() = { type, depth -> approximateSimpleToSubType(type, depth) }

    companion object {
        // This value is only used prior to 2.2 (without CapturedTypeApproximationReworked)
        const val CACHE_FOR_INCORPORATION_MAX_SIZE = 500
    }

    class Cache {
        val resultsForSupertype = mutableMapOf<CapturedTypeMarker, ApproximationResult>()
        val resultsForSubtype = mutableMapOf<CapturedTypeMarker, ApproximationResult>()

        // We assume that no approximation cycles should be met when approximating to a type's lower bound
        // Currently, the known sources of approximation cycles are
        // - captured types with recursive bounds
        // - recursive local types
        val typesBeingApproximatedToSupertype = mutableSetOf<RigidTypeMarker>()

        // Non-trivial lower bounds are always brought via explicitly specified/inferred `in` projection where no recursion should happen.
        @AssertionsOnly
        val typesBeingApproximatedToSubtype = mutableSetOf<RigidTypeMarker>()

        operator fun plusAssign(other: Cache) {
            resultsForSupertype += other.resultsForSupertype
            resultsForSubtype += other.resultsForSubtype

            @OptIn(AssertionsOnly::class)
            check(other.typesBeingApproximatedToSupertype.isEmpty() && other.typesBeingApproximatedToSubtype.isEmpty()) {
                "Combination of caches/Constraint storages is not expected to happen during type approximation"
            }
        }
    }

    // null means that this input type is the result, i.e. input type not contains not-allowed kind of types
    // type <: resultType
    fun approximateToSuperType(
        type: KotlinTypeMarker,
        conf: TypeApproximatorConfiguration,
        caches: TypeApproximatorCachesPerConfiguration? = null,
    ): KotlinTypeMarker? {
        return approximateEntryPoint(type, conf, caches) { type, depth -> approximateToSuperType(type, depth) }
    }

    // resultType <: type
    fun approximateToSubType(
        type: KotlinTypeMarker,
        conf: TypeApproximatorConfiguration,
        caches: TypeApproximatorCachesPerConfiguration? = null,
    ): KotlinTypeMarker? {
        return approximateEntryPoint(type, conf, caches) { type, depth -> approximateToSubType(type, depth) }
    }

    private inline fun approximateEntryPoint(
        type: KotlinTypeMarker,
        conf: TypeApproximatorConfiguration,
        caches: TypeApproximatorCachesPerConfiguration?,
        approximateTo: context(TypeApproximatorConfiguration, Cache) (KotlinTypeMarker, Int) -> KotlinTypeMarker?,
    ): KotlinTypeMarker? {
        return context(conf, caches?.getOrPut(conf, ::Cache) ?: Cache()) {
            try {
                approximateTo(type, -type.typeDepthForApproximation())
            } catch (e: StackOverflowError) {
                throw RuntimeException("StackOverflowError during type approximation for ${type.renderForDebugInfo()}", e)
            }
        }
    }

    protected open fun KotlinTypeMarker.renderForDebugInfo(): String = toString()

    fun clearCache() {
        cacheForIncorporationConfigToSubtypeDirection.clear()
        cacheForIncorporationConfigToSuperDirection.clear()
    }

    context(conf: TypeApproximatorConfiguration)
    private fun checkExceptionalCases(
        type: KotlinTypeMarker, depth: Int, toSuper: Boolean
    ): ApproximationResult? {
        return when {
            type.isSpecial() ->
                null.toApproximationResult()

            type.isError() ->
                // todo -- fix builtIns. Now builtIns here is DefaultBuiltIns
                (if (!conf.approximateErrorTypes) null else type.defaultResult(toSuper)).toApproximationResult()

            // Limiting approximation depth is obsolete
            !capturedTypeApproximationReworked && depth > 3 ->
                type.defaultResult(toSuper).toApproximationResult()

            else -> null
        }
    }

    private fun KotlinTypeMarker?.toApproximationResult(): ApproximationResult = ApproximationResult(this)

    context(conf: TypeApproximatorConfiguration)
    private inline fun cachedValue(
        type: KotlinTypeMarker,
        toSuper: Boolean,
        approximate: () -> KotlinTypeMarker?
    ): KotlinTypeMarker? {
        // Approximator depends on a configuration, so cache should take it into account
        // Here, we cache only types for configuration "from incorporation", which is used most intensively
        // More predictable caches are used since capturedTypeApproximationReworked
        if (capturedTypeApproximationReworked || conf !is TypeApproximatorConfiguration.IncorporationConfiguration) return approximate()

        val cache = if (toSuper) cacheForIncorporationConfigToSuperDirection else cacheForIncorporationConfigToSubtypeDirection

        if (cache.size > CACHE_FOR_INCORPORATION_MAX_SIZE) return approximate()

        return cache.getOrPut(type, { approximate().toApproximationResult() }).type
    }

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateToSuperType(type: KotlinTypeMarker, depth: Int): KotlinTypeMarker? {
        checkExceptionalCases(type, depth, toSuper = true)?.let { return it.type }

        return cachedValue(type, toSuper = true) {
            approximateTo(
                AbstractTypeChecker.prepareType(ctx, type),
                { upperBound() },
                referenceApproximateToSuperType,
                depth
            )
        }
    }

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateToSubType(type: KotlinTypeMarker, depth: Int): KotlinTypeMarker? {
        checkExceptionalCases(type, depth, toSuper = false)?.let { return it.type }

        return cachedValue(type, toSuper = false) {
            approximateTo(
                AbstractTypeChecker.prepareType(ctx, type),
                { lowerBound() },
                referenceApproximateToSubType,
                depth
            )
        }
    }

    // Don't call this method directly, it should be used only in approximateToSuperType/approximateToSubType (use these methods instead)
    // This method contains detailed implementation only for type approximation, it doesn't check exceptional cases and doesn't use cache
    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateTo(
        type: KotlinTypeMarker,
        bound: FlexibleTypeMarker.() -> RigidTypeMarker,
        approximateTo: FunctionTypeForRigidTypeApproximation,
        depth: Int
    ): KotlinTypeMarker? {
        when (type) {
            is RigidTypeMarker -> return approximateTo(type, depth)
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
                    val lowerResult = approximateTo(lowerBound, depth)

                    if (isTriviallyFlexible(type)) {
                        return lowerResult?.let {
                            createTrivialFlexibleTypeOrSelf(it)
                        }
                    }

                    val upperBound = type.upperBound()
                    val upperResult = if (!type.isRawType() && !shouldApproximateUpperBoundSeparately(lowerBound, upperBound)) {
                        // We skip approximating the upper bound if the type constructors match as an optimization.
                        lowerResult?.withNullability(upperBound.isMarkedNullable())
                    } else {
                        approximateTo(upperBound, depth)
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
                    return type.bound().let { approximateTo(it, depth) ?: it }
                }
            }
            else -> error("sealed")
        }
    }

    context(conf: TypeApproximatorConfiguration)
    private fun shouldApproximateUpperBoundSeparately(
        lowerBound: RigidTypeMarker,
        upperBound: RigidTypeMarker,
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
                upperBound.getArgumentOrNull(0).let { it is CapturedTypeMarker && conf.shouldApproximateCapturedType(it) }
    }

    context(conf: TypeApproximatorConfiguration)
    private fun KotlinTypeMarker.requiresLocalOrAnonymousApproximation(
        constructor: TypeConstructorMarker = typeConstructor()
    ): Boolean {
        return conf.approximateLocalTypes && conf.shouldApproximateLocalType(ctx, this) && constructor.isLocalType() ||
                conf.approximateAnonymous && constructor.isAnonymous()
    }

    context(conf: TypeApproximatorConfiguration, cache: Cache)
    private fun approximateLocalTypes(
        type: RigidTypeMarker,
        toSuper: Boolean,
        depth: Int,
    ): RigidTypeMarker? {
        if (!toSuper) return null
        if (!conf.approximateLocalTypes && !conf.approximateAnonymous) return null

        val constructor = type.typeConstructor()
        if (!type.requiresLocalOrAnonymousApproximation(constructor)) return null
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
                if (!currentType.requiresLocalOrAnonymousApproximation(currentConstructor)) {
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
        if (ctx.isK2) {
            cache.typesBeingApproximatedToSupertype += type
            (approximateTo(result, true, depth) as? RigidTypeMarker)?.let { result = it }
            cache.typesBeingApproximatedToSupertype -= type
        }

        return result
    }

    private fun isIntersectionTypeEffectivelyNothing(constructor: IntersectionTypeConstructorMarker): Boolean {
        // We consider intersection as Nothing only if one of it's component is a primitive number type
        // It's intentional we're not trying to prove population of some type as it was in OI

        return constructor.supertypes().any {
            !it.isMarkedNullable() && it.isSignedOrUnsignedNumberType()
        }
    }

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateIntersectionType(
        type: RigidTypeMarker,
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
                    conf.intersectionStrategy == @OptIn(AllowedToUsedOnlyInK1::class) TypeApproximatorConfiguration.IntersectionStrategy.TO_UPPER_BOUND_IF_SUPERTYPE)
        ) {
            return approximateToSuperType(upperBoundForApproximation, depth) ?: upperBoundForApproximation
        }

        var thereIsApproximation = false
        val newTypes = typeConstructor.supertypes().map {
            val newType = if (toSuper) approximateToSuperType(it, depth) else approximateToSubType(it, depth)
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
                intersectTypes(newTypes, upperBoundForApproximation, toSuper, depth)
            }
            TypeApproximatorConfiguration.IntersectionStrategy.TO_FIRST -> if (toSuper) newTypes.first() else return type.defaultResult(toSuper = false)
            // commonSupertypeCalculator should handle flexible types correctly
            TypeApproximatorConfiguration.IntersectionStrategy.TO_COMMON_SUPERTYPE,

            @OptIn(AllowedToUsedOnlyInK1::class)
            TypeApproximatorConfiguration.IntersectionStrategy.TO_UPPER_BOUND_IF_SUPERTYPE -> {
                if (!toSuper) return type.defaultResult(toSuper = false)
                val resultType = commonSuperType(newTypes)
                approximateToSuperType(resultType, depth) ?: resultType
            }
        }

        return if (type.isMarkedNullable()) baseResult.withNullability(true) else baseResult
    }

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun intersectTypes(
        newTypes: List<KotlinTypeMarker>,
        upperBoundForApproximation: KotlinTypeMarker?,
        toSuper: Boolean,
        depth: Int,
    ): KotlinTypeMarker {
        val intersectionType = intersectTypes(newTypes)

        if (upperBoundForApproximation == null) {
            return intersectionType
        }

        val alternativeTypeApproximated = if (toSuper) {
            approximateToSuperType(upperBoundForApproximation, depth)
        } else {
            approximateToSubType(upperBoundForApproximation, depth)
        } ?: upperBoundForApproximation

        return createTypeWithUpperBoundForIntersectionResult(intersectionType, alternativeTypeApproximated)
    }


    context(conf: TypeApproximatorConfiguration, cache: Cache)
    private fun approximateCapturedType(
        capturedType: CapturedTypeMarker,
        toSuper: Boolean,
        depth: Int,
    ): KotlinTypeMarker? {
        val currentlyBeingApproximated = when {
            toSuper -> cache.typesBeingApproximatedToSupertype
            // We only track potential loops in lower bounds to raise an assertion
            else -> @OptIn(AssertionsOnly::class) cache.typesBeingApproximatedToSubtype
        }
        val computedResults = when {
            toSuper -> cache.resultsForSupertype
            else -> cache.resultsForSubtype
        }

        computedResults[capturedType]?.let { return it.type }

        if (capturedTypeApproximationReworked && !currentlyBeingApproximated.add(capturedType)) {
            if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
                error("Captured types loop should be handled at approximateParametrizedType")
            }

            return createErrorType(
                "Captured types loop should be handled at approximateParametrizedType",
                capturedType,
            )
        }

        val result = doApproximateCapturedType(capturedType, toSuper, depth)

        if (capturedTypeApproximationReworked) {
            currentlyBeingApproximated.remove(capturedType)
        }

        if (!capturedTypeApproximationReworked) return result
        // There's no really much sense to store something beside FROM_EXPRESSION,
        // which might be quite long-living even surviving between the calls
        if (capturedType.captureStatus() != CaptureStatus.FROM_EXPRESSION) return result

        computedResults[capturedType] = ApproximationResult(result)

        return result
    }

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun doApproximateCapturedType(
        capturedType: CapturedTypeMarker,
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
            approximateToSuperType(baseSuperType, depth)
        }
        val approximatedSubType by lazy(LazyThreadSafetyMode.NONE) { approximateToSubType(baseSubType, depth) }

        if (!conf.shouldApproximateCapturedType(capturedType)) {
            /**
             * Here everything is ok if bounds for this captured type should not be approximated.
             * But. If such bounds contains some unauthorized types, then we cannot leave this captured type "as is".
             * And we cannot create new capture type, because meaning of new captured type is not clear.
             * So, we will just approximate such types
             *
             * TODO remove workaround when we can create captured types with external identity KT-65228.
             * todo handle flexible types
             */
            if (capturedTypeApproximationReworked || approximatedSuperType == null && approximatedSubType == null) {
                // Avoid avoiding approximation bounds of a captured type while one shouldn't be approximated itself doesn't look
                // universally correct. Though by construction of different captured kinds, currently it's only relevant
                // to a situation when some FOR_INCORPORATION is put into another captured type with a kind FROM_EXPRESSION and that
                // case is handled via IncorporationConfiguration.
                // TODO: consider replacing content of the captured types together with KT-65228
                return null
            }
        }
        val baseResult = if (toSuper) approximatedSuperType ?: baseSuperType else approximatedSubType ?: baseSubType

        // C = in Int, Int <: C => Int? <: C?
        // C = out Number, C <: Number => C? <: Number?
        return when {
            capturedType.isMarkedNullable() -> baseResult.withNullability(true)
            !isK2 && @OptIn(AllowedToUsedOnlyInK1::class) capturedType.isProjectionNotNull() ->
                baseResult.withNullability(false)
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
        // Recursion is being handled via approximateParametrizedType
        if (capturedTypeApproximationReworked) return this
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

    // In fact is used only as dummy argument of createStarProjection,
    // can never be found as a property value etc.
    private object TypeParameterMarkerStubForK2StarProjection : TypeParameterMarker

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateSimpleToSuperType(type: RigidTypeMarker, depth: Int) =
        approximateTo(type, toSuper = true, depth = depth)

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateSimpleToSubType(type: RigidTypeMarker, depth: Int) =
        approximateTo(type, toSuper = false, depth = depth)

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateTo(
        type: RigidTypeMarker,
        toSuper: Boolean,
        depth: Int
    ): KotlinTypeMarker? {
        if (type.argumentsCount() != 0) {
            return approximateParametrizedType(type, toSuper, depth + 1)
        }

        val definitelyNotNullType = type.asDefinitelyNotNullType()
        if (definitelyNotNullType != null) {
            return approximateDefinitelyNotNullType(definitelyNotNullType, toSuper, depth)
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
            return approximateCapturedType(capturedType, toSuper, depth)
        }

        if (typeConstructor.isIntersection()) {
            return approximateIntersectionType(type, toSuper, depth)
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

        return approximateLocalTypes(type, toSuper, depth) // simple classifier type
    }

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateDefinitelyNotNullType(
        type: DefinitelyNotNullTypeMarker,
        toSuper: Boolean,
        depth: Int
    ): KotlinTypeMarker? {
        val originalType = type.original()
        val approximatedOriginalType =
            if (toSuper) approximateToSuperType(originalType, depth) else approximateToSubType(originalType, depth)
        val typeWithErasedNullability = originalType.withNullability(false)

        // Approximate T!! into T if T is already not-null (has not-null upper bounds)
        if (originalType.typeConstructor().isTypeParameterTypeConstructor() && !typeWithErasedNullability.isNullableType()) {
            return typeWithErasedNullability
        }

        return approximatedOriginalType?.makeDefinitelyNotNullOrNotNull(preserveAttributes = true)
    }

    private fun isApproximateDirectionToSuper(effectiveVariance: TypeVariance, toSuper: Boolean) =
        when (effectiveVariance) {
            TypeVariance.OUT -> toSuper
            TypeVariance.IN -> !toSuper
            TypeVariance.INV -> throw AssertionError("Incorrect variance $effectiveVariance")
        }

    context(conf: TypeApproximatorConfiguration, cache: Cache)
    private fun approximateParametrizedType(
        type: RigidTypeMarker,
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

            val effectiveVariance =
                AbstractTypeChecker.effectiveVariance(parameter.getVariance(), argument.getVariance())
                    ?: return createApproximatedResultForInconsistentArgumentVariance(type, parameter, argument, index, toSuper)

            val simpleArgumentType = argumentType.lowerBoundIfFlexible().originalIfDefinitelyNotNullable()
            val capturedType = simpleArgumentType.asCapturedType()

            fun approximateToSuperTypeWithRecursionPrevention(): ApproximationResult? {
                if (capturedTypeApproximationReworked && simpleArgumentType in cache.typesBeingApproximatedToSupertype) {
                    if (capturedType != null && conf.shouldApproximateCapturedType(capturedType) ||
                        simpleArgumentType.requiresLocalOrAnonymousApproximation()
                    ) {
                        newArguments[index] = createStarProjection(parameter)
                    } else {
                        // Just leave the argument type as is
                    }
                    return null
                }

                return ApproximationResult(
                    approximateToSuperType(argumentType, depth)
                        ?.makeApproximatedFlexibleNotNullIfUpperBoundNotNull(argumentType, parameter)
                )
            }

            val approximatedToSubType: KotlinTypeMarker? by lazy(LazyThreadSafetyMode.NONE) {
                approximateToSubType(argumentType, depth)
            }

            if (shouldApproximateStarBasedCapturedTypeArgumentAsItsProjection(capturedType, parameter, effectiveVariance, toSuper)) {
                newArguments[index] = capturedType?.typeConstructorProjection()
                continue
            }

            when (effectiveVariance) {
                TypeVariance.OUT, TypeVariance.IN -> {
                    if (shouldApproximateIntersectionContravariantlyPlacedArgumentTypeToStar(argumentType, effectiveVariance)) {
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
                        val approximatedToSuperType = (approximateToSuperTypeWithRecursionPrevention() ?: continue).type

                        if (!isK2 &&
                            @OptIn(AllowedToUsedOnlyInK1::class) needK1SpecialHandlingForIntersectionType(argumentType, parameter)
                        ) {
                            @OptIn(AllowedToUsedOnlyInK1::class)
                            specialK1HandlingOfIntersectionType(approximatedToSuperType, parameter)
                        } else {
                            approximatedToSuperType ?: continue@loop
                        }
                    } else {
                        approximatedToSubType ?: continue@loop
                    }

                    newArguments[index] = when {
                        useStarProjectionInCaseIntersectionApproximatedWithUpperBoundViolation(
                            effectiveVariance, parameter, argumentType, approximatedArgument,
                        ) ->
                            createStarProjection(parameter)

                        parameter.getVariance() == TypeVariance.INV ->
                            createTypeArgument(approximatedArgument, effectiveVariance)

                        else ->
                            approximatedArgument.asTypeArgument()
                    }
                }
                TypeVariance.INV -> {
                    if (!toSuper) {
                        // Inv<Foo> cannot be approximated to subType
                        val toSubType = approximatedToSubType ?: continue@loop

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
                        val subType = approximateToSubType(argumentType, depth) ?: continue@loop
                        if (shouldUseSubTypeForCapturedArgument(subType, argumentType, depth)) {
                            newArguments[index] = createTypeArgument(subType, TypeVariance.IN)
                            continue@loop
                        }
                    }

                    val approximatedToSuperType = approximateToSuperTypeWithRecursionPrevention()?.type
                        ?: continue@loop // null means that this type we can leave as is

                    val isTrivialSuper = approximatedToSuperType.isTrivialSuper()
                    newArguments[index] = when {
                        isTrivialSuper && approximatedToSubType == null -> continue@loop // seems like this is never null

                        // Example with non-trivial both types approximations:
                        //  Inv<In<C>> where C = Captured(in Int)
                        //  Inv<In<C>> <: Inv<out In<Int>>
                        //  Inv<In<C>> <: Inv<in In<Any?>>
                        //
                        // So, both of the options are possible, but since such a case is rare,
                        // we will choose Inv<out In<Int>> for now
                        isTrivialSuper && approximatedToSubType?.isTrivialSub() == false ->
                            createTypeArgument(approximatedToSubType!!, TypeVariance.IN)

                        AbstractTypeChecker.equalTypes(this, argumentType, approximatedToSuperType) ->
                            approximatedToSuperType.asTypeArgument()

                        else ->
                            createTypeArgument(approximatedToSuperType, TypeVariance.OUT)
                    }
                }
            }
        }

        if (newArguments.all { it == null }) return approximateLocalTypes(type, toSuper, depth)

        val newArgumentsList = List(type.argumentsCount()) { index -> newArguments[index] ?: type.getArgument(index) }
        val approximatedType = type.replaceArguments(newArgumentsList)
        return approximateLocalTypes(approximatedType, toSuper, depth) ?: approximatedType
    }

    context(conf: TypeApproximatorConfiguration)
    private fun useStarProjectionInCaseIntersectionApproximatedWithUpperBoundViolation(
        effectiveVariance: TypeVariance,
        parameter: TypeParameterMarker,
        argumentType: KotlinTypeMarker,
        approximatedArgument: KotlinTypeMarker,
    ): Boolean {
        if (conf.intersectionStrategy == TypeApproximatorConfiguration.IntersectionStrategy.ALLOWED) return false
        if (effectiveVariance != TypeVariance.OUT) return false
        if (!argumentType.typeConstructor().isIntersection()) return false

        var shouldReplaceWithStar = false
        for (upperBoundIndex in 0 until parameter.upperBoundCount()) {
            if (!AbstractTypeChecker.isSubtypeOf(ctx, approximatedArgument, parameter.getUpperBound(upperBoundIndex))) {
                shouldReplaceWithStar = true
                break
            }
        }

        return shouldReplaceWithStar
    }

    /**
     * This functions checks if it is semantically correct to approximate Captured(*) type argument just as star projection.
     * Generally, it shouldn't drastically change semantics, but the result like List<*> is much clearer than List<out Any?>,
     * especially when the type parameter's upper bound is not that trivial.
     *
     * NB: It doesn't 100% prevent from loops when approximating recursive types, for that see
     * local fun approximateToSuperTypeWithRecursionPrevention at [approximateParametrizedType]
     */
    context(conf: TypeApproximatorConfiguration)
    private fun shouldApproximateStarBasedCapturedTypeArgumentAsItsProjection(
        capturedType: CapturedTypeMarker?,
        parameter: TypeParameterMarker,
        effectiveVariance: TypeVariance,
        toSuper: Boolean,
    ): Boolean {
        if (capturedType?.typeConstructorProjection()?.isStarProjection() != true) return false
        // SomeClass<Captured(*)> cannot be approximated to subtype as SomeClass<*>
        if (!toSuper) return false
        // We should leave the captured type as is
        if (isK2 && !conf.shouldApproximateCapturedType(capturedType)) return false

        // In<Captured(*)> is nicer to approximate to In<*> than In<Nothing>, independently
        // of the relation between type parameters (see below).
        // It's not a critical thing, though, both options are correct
        if (capturedTypeApproximationReworked &&
            capturedType.lowerType()?.isTrivialSub() != false &&
            parameter.getVariance() == TypeVariance.IN
        ) {
            return true
        }

        // In case like
        // `class A<T : CharSequence>` and C1 = Captured(*) from A<*>
        // List<C1> should be approximated to List<out CharSequence> rather than List<*>
        if (capturedType.typeParameter() != parameter) return false

        if (effectiveVariance == TypeVariance.OUT || effectiveVariance == TypeVariance.INV) return true

        // In<Captured(*)> from In<*> is reasonable to approximate as In<*>
        // Though, we temporarily leave it for only for `capturedTypeApproximationReworked` until the relevant LF is removed
        // Potentially, this line and the one above might be replaced with just `return true`
        return capturedTypeApproximationReworked /* || (effectiveVariance == TypeVariance.IN) -- always true */
    }

    context(conf: TypeApproximatorConfiguration)
    private fun shouldApproximateIntersectionContravariantlyPlacedArgumentTypeToStar(
        argumentType: KotlinTypeMarker,
        effectiveVariance: TypeVariance,
    ): Boolean {
        return conf.approximateIntersectionTypesInContravariantPositions && effectiveVariance == TypeVariance.IN &&
                argumentType.typeConstructor().isIntersection() &&
                isIntersectionTypeEffectivelyNothing(argumentType.typeConstructor() as IntersectionTypeConstructorMarker)
    }

    @AllowedToUsedOnlyInK1
    private fun specialK1HandlingOfIntersectionType(
        approximatedType: KotlinTypeMarker?,
        parameter: TypeParameterMarker,
    ): KotlinTypeMarker {
        val intersectedUpperBounds = intersectTypes(parameter.getUpperBounds())
        return if (approximatedType == null || !AbstractTypeChecker.isSubtypeOf(ctx, approximatedType, intersectedUpperBounds)) {
            intersectedUpperBounds
        } else {
            approximatedType
        }
    }

    @AllowedToUsedOnlyInK1
    context(conf: TypeApproximatorConfiguration)
    private fun needK1SpecialHandlingForIntersectionType(
        argumentType: KotlinTypeMarker,
        parameter: TypeParameterMarker,
    ): Boolean {
        if (conf.intersectionStrategy != TypeApproximatorConfiguration.IntersectionStrategy.TO_UPPER_BOUND_IF_SUPERTYPE) return false
        if (!argumentType.typeConstructor().isIntersection()) return false
        return parameter.getUpperBounds().all { AbstractTypeChecker.isSubtypeOf(ctx, argumentType, it) }
    }

    context(conf: TypeApproximatorConfiguration)
    private fun createApproximatedResultForInconsistentArgumentVariance(
        type: RigidTypeMarker,
        parameter: TypeParameterMarker,
        argument: TypeArgumentMarker,
        index: Int,
        toSuper: Boolean,
    ): RigidTypeMarker {
        if (conf.approximateErrorTypes) return type.defaultResult(toSuper)

        return createErrorType(
            "Inconsistent type: $type ($index parameter has declared variance: ${parameter.getVariance()}, " +
                    "but argument variance is ${argument.getVariance()})",
            type
        )
    }

    context(conf: TypeApproximatorConfiguration)
    private fun KotlinTypeMarker.makeApproximatedFlexibleNotNullIfUpperBoundNotNull(
        nonApproximatedType: KotlinTypeMarker,
        parameter: TypeParameterMarker,
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

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun shouldUseSubTypeForCapturedArgument(
        subType: KotlinTypeMarker,
        capturedArgumentType: KotlinTypeMarker,
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
            approximateToSubType(capturedArgumentType.withNullability(false), depth)
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
