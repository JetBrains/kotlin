/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.isPrimitiveType
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.resolve.getSymbolByLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance

/**
 * Checks if a given collection of [ConeKotlinType] are compatible. In other words, the types are compatible if it's possible at all to
 * define a type that's a subtype of all of the given types. The compatibility of a given set of types concept is closely related to whether
 * the intersection of these types is inhabited. But it's not identical because 1) one can manually control visibility of constructors and
 * 2) there can be unused type parameters.
 *
 * The compatibility check is done recursively on the given types and all type arguments passed to each corresponding type parameters. For
 * example, consider the following two types:
 *
 * ```
 *   - ArrayList<Set<String>>
 *   - List<HashSet<Int>>
 * ```
 *
 * The checker first checks the base types `ArrayList` and `List`, and it sees no issue since `ArrayList <: List`. Next it checks the type
 * parameters bound to these base types: `T1` in `ArrayList<T1>` and `T2` in `List<T2>`. For `T1`, there is only one bound type argument
 * `Set<String>`, so it's good. For `T2`, there are two bound type arguments: `Set<String>` and `HashSet<Int>`. Now the checker recursively
 * checks whether these two types are compatible. Again, it first checks the base type `Set` and `HashSet`, and it finds no problem since
 * `HashSet <: Set`. Finally, checks the type arguments `String` and `Int` that are bound to `T` in `Set<T>`. They are incompatible since
 * `String` and `Int` are unrelated classes.
 *
 * The above example only goes over covariant type arguments. For contravariant types, the checker simply checks whether the range formed by
 * covariant and contravariant bounds is empty. For example, a range like `[Collection, List]` is empty and hence invalid because `List` is
 * not a super class/interface of `Collection`
 */
object ConeTypeCompatibilityChecker {

    private val javaClassClassId = ClassId.fromString("java/lang/Class")
    private val kotlinClassClassId = ClassId.fromString("kotlin/reflect/KClass")


    /**
     * The result returned by [ConeTypeCompatibilityChecker]. Note the order of enum entries matters.
     */
    enum class Compatibility : Comparable<Compatibility> {
        /** The given types are fully compatible. */
        COMPATIBLE,

        /** The given types may not be compatible. But the compiler would allow such comparisons. */
        SOFT_INCOMPATIBLE,

        /**
         * The given types are definitely incompatible. If the established contracts of Kotlin code are respected, values of the given
         * types can never be considered equal.
         */
        HARD_INCOMPATIBLE,
    }

    fun ConeInferenceContext.isCompatible(a: ConeKotlinType, b: ConeKotlinType): Compatibility {
        // Don't report explicit comparison with `Nothing`
        if (a.isNothing || b.isNothing) return Compatibility.COMPATIBLE
        if (a is ConeIntersectionType) {
            return a.intersectedTypes.minOf { isCompatible(it, b) }
        }
        if (b is ConeIntersectionType) {
            return b.intersectedTypes.minOf { isCompatible(a, it) }
        }

        return when (val intersectionType = intersectTypesOrNull(listOf(a, b))) {
            is ConeIntersectionType -> intersectionType.intersectedTypes.getCompatibility(this)
            else -> if (intersectionType?.isNothing == true) Compatibility.HARD_INCOMPATIBLE else Compatibility.COMPATIBLE
        }
    }

    private fun Collection<ConeKotlinType>.getCompatibility(ctx: ConeInferenceContext): Compatibility {
        // If all types are nullable, then `null` makes the given types compatible.
        if (all { with(ctx) { it.isNullableType() } }) return Compatibility.COMPATIBLE

        // Next can simply focus on the type hierarchy and don't need to worry about nullability.
        val compatibilityUpperBound = when {
            all {
                it.isPrimitive
            } -> Compatibility.SOFT_INCOMPATIBLE // TODO: remove after KT-46383 is fixed
            all {
                it.isConcreteType()
            } -> Compatibility.HARD_INCOMPATIBLE
            // If any type is not concrete, for example, type parameter, we only report warning for incompatible types.
            // This is to stay compatible with FE1.0.
            else -> Compatibility.SOFT_INCOMPATIBLE
        }
        return ctx.getCompatibility(flatMap { it.collectUpperBounds() }.toSet(), emptySet(), compatibilityUpperBound)
    }

    private fun ConeKotlinType.isConcreteType(): Boolean {
        return when (this) {
            is ConeClassLikeType -> true
            is ConeDefinitelyNotNullType -> original.isConcreteType()
            is ConeIntersectionType -> intersectedTypes.all { it.isConcreteType() }
            else -> false
        }
    }

    /**
     * @param compatibilityUpperBound the max compatibility result that can be returned by this method. For example, if this is set to
     * [Compatibility.SOFT_INCOMPATIBLE], then even if the given bounds don't match the hard way (for example, incompatible primitives) the
     * method should still return [Compatibility.SOFT_INCOMPATIBLE]. This is useful for checking type parameters since we don't want to
     * dictate what semantics a type parameter may have in user code. In other words, if user wants to compare `MyCustom<out String>` with
     * `MyCustom<out Int>`, we let them do so since we do not know what class `MyCustom` uses the type parameter for. Empty containers are
     * another example: `emptyList<Int>() == emptyList<String>()`.
     */
    private fun ConeInferenceContext.getCompatibility(
        upperBounds: Set<ConeClassLikeType>,
        lowerBounds: Set<ConeClassLikeType>,
        compatibilityUpperBound: Compatibility,
        checkedTypeParameters: MutableSet<FirTypeParameterSymbol> = mutableSetOf(),
    ): Compatibility {
        val upperBoundClasses: Set<FirClassWithSuperClasses> = upperBounds.mapNotNull { it.toFirClassWithSuperClasses(this) }.toSet()

        // Following if condition is an optimization: if we ignore the subtyping relation and treat all upper bounds as unrelated
        // classes/interfaces, yet the types are deemed compatible for sure, then we just bail out early.
        if (lowerBounds.isEmpty() &&
            (upperBounds.size < 2 ||
                    this.areClassesOrInterfacesCompatible(upperBoundClasses, compatibilityUpperBound) == Compatibility.COMPATIBLE)
        ) {
            return Compatibility.COMPATIBLE
        }

        // TODO: Due to KT-49358, we skip any checks on Java and Kotlin refection class.
        if (upperBounds.any { it.classId == javaClassClassId || it.classId == kotlinClassClassId }) return Compatibility.COMPATIBLE

        val leafClassesOrInterfaces = computeLeafClassesOrInterfaces(upperBoundClasses)
        this.areClassesOrInterfacesCompatible(leafClassesOrInterfaces, compatibilityUpperBound)?.let { return it }

        // Check if the range formed by upper bounds and lower bounds is empty.
        if (!lowerBounds.all { lowerBoundType ->
                val classesSatisfyingLowerBounds =
                    lowerBoundType.toFirClassWithSuperClasses(this)?.thisAndAllSuperClasses ?: emptySet()
                leafClassesOrInterfaces.all { it in classesSatisfyingLowerBounds }
            }
        ) {
            return compatibilityUpperBound
        }

        if (upperBounds.size < 2) return Compatibility.COMPATIBLE

        // Base types are compatible. Now we check type parameters.

        val typeArgumentMapping = mutableMapOf<FirTypeParameterSymbol, BoundTypeArguments>().apply {
            for (type in upperBounds) {
                collectTypeArgumentMapping(type, this@getCompatibility, compatibilityUpperBound)
            }
        }
        var result = Compatibility.COMPATIBLE
        val typeArgsCompatibility = typeArgumentMapping.asSequence()
            .map { (paramRef, boundTypeArguments) ->
                val (upper, lower, compatibility) = boundTypeArguments
                if (paramRef in checkedTypeParameters) {
                    // if we are already checking this type parameter, simply bail out to prevent infinite recursion.
                    Compatibility.COMPATIBLE
                } else {
                    checkedTypeParameters.add(paramRef)
                    getCompatibility(upper, lower, compatibility, checkedTypeParameters)
                }
            }
        for (compatibility in typeArgsCompatibility) {
            if (compatibility == compatibilityUpperBound) return compatibility
            if (compatibility > result) {
                result = compatibility
            }
        }
        return result
    }

    /**
     *  Puts the upper bound classes into the class hierarchy and count hows many subclasses are there for each encountered class. Then
     * output a list of leaf classes or interfaces in the class hierarchy.
     */
    private fun computeLeafClassesOrInterfaces(upperBoundClasses: Set<FirClassWithSuperClasses>): Set<FirClassWithSuperClasses> {
        val isLeaf = mutableMapOf<FirClassWithSuperClasses, Boolean>()
        upperBoundClasses.associateWithTo(isLeaf) { true }  // implementation of keysToMap actually ends up creating 2 maps so this is better
        val queue = ArrayDeque(upperBoundClasses)
        while (queue.isNotEmpty()) {
            for (superClass in queue.removeFirst().superClasses) {
                when (isLeaf[superClass]) {
                    true -> isLeaf[superClass] = false
                    false -> {
                        // nothing to be done since this super class has already been handled.
                    }
                    else -> {
                        isLeaf[superClass] = false
                        queue.addLast(superClass)
                    }
                }
            }
        }
        return isLeaf.filterValues { it }.keys
    }

    /**
     * Checks whether the given classes are compatible. In other words, check if it's possible for objects of the given classes to be
     * considered equal by [Any.equals].
     *
     * @return null if this check is inconclusive
     */
    private fun ConeInferenceContext.areClassesOrInterfacesCompatible(
        classesOrInterfaces: Collection<FirClassWithSuperClasses>,
        compatibilityUpperBound: Compatibility
    ): Compatibility? {
        val classes = classesOrInterfaces.filter { !it.isInterface }
        // Java force single inheritance, so any pair of unrelated classes are incompatible.
        if (classes.size >= 2) {
            return if (classes.any { it.getHasPredefinedEqualityContract(this) }) {
                compatibilityUpperBound
            } else {
                Compatibility.SOFT_INCOMPATIBLE
            }
        }
        val finalClass = classes.firstOrNull { it.isFinal } ?: return null
        // One final class and some other unrelated interface are not compatible
        if (classesOrInterfaces.size > classes.size) {
            return if (finalClass.getHasPredefinedEqualityContract(this)) {
                compatibilityUpperBound
            } else {
                Compatibility.SOFT_INCOMPATIBLE
            }
        }
        return null
    }

    /**
     * Collects the upper bounds as [ConeClassLikeType].
     */
    private fun ConeKotlinType?.collectUpperBounds(): Set<ConeClassLikeType> {
        if (this == null) return emptySet()
        return when (this) {
            is ConeErrorType -> emptySet() // Ignore error types
            is ConeLookupTagBasedType -> when (this) {
                is ConeClassLikeType -> setOf(this)
                is ConeTypeVariableType -> {
                    (lookupTag.originalTypeParameter as? ConeTypeParameterLookupTag)?.typeParameterSymbol.collectUpperBounds()
                }
                is ConeTypeParameterType -> lookupTag.typeParameterSymbol.collectUpperBounds()
                else -> throw IllegalStateException("missing branch for ${javaClass.name}")
            }
            is ConeDefinitelyNotNullType -> original.collectUpperBounds()
            is ConeIntersectionType -> intersectedTypes.flatMap { it.collectUpperBounds() }.toSet()
            is ConeFlexibleType -> upperBound.collectUpperBounds()
            is ConeCapturedType -> constructor.supertypes?.flatMap { it.collectUpperBounds() }?.toSet().orEmpty()
            is ConeIntegerConstantOperatorType -> setOf(getApproximatedType())
            is ConeStubType, is ConeIntegerLiteralConstantType -> throw IllegalStateException("$this should not reach here")
        }
    }

    private fun FirTypeParameterSymbol?.collectUpperBounds(): Set<ConeClassLikeType> {
        if (this == null) return emptySet()
        return resolvedBounds.flatMap { it.coneTypeSafe<ConeKotlinType>().collectUpperBounds() }.toSet()
    }

    private fun ConeKotlinType?.collectLowerBounds(): Set<ConeClassLikeType> {
        if (this == null) return emptySet()
        return when (this) {
            is ConeErrorType -> emptySet() // Ignore error types
            is ConeLookupTagBasedType -> when (this) {
                is ConeClassLikeType -> setOf(this)
                is ConeTypeVariableType -> emptySet()
                is ConeTypeParameterType -> emptySet()
                else -> throw IllegalStateException("missing branch for ${javaClass.name}")
            }
            is ConeDefinitelyNotNullType -> original.collectLowerBounds()
            is ConeIntersectionType -> intersectedTypes.flatMap { it.collectLowerBounds() }.toSet()
            is ConeFlexibleType -> lowerBound.collectLowerBounds()
            is ConeCapturedType -> constructor.supertypes?.flatMap { it.collectLowerBounds() }?.toSet().orEmpty()
            is ConeIntegerConstantOperatorType -> setOf(getApproximatedType())
            is ConeStubType, is ConeIntegerLiteralConstantType -> throw IllegalStateException("$this should not reach here")
        }
    }

    /**
     * For each type parameters appeared in the class hierarchy, collect all type arguments that eventually mapped to it. For example,
     * given type `List<String>`, the returned map contains
     *
     *   - type parameter of `List` -> upper:[`String`], lower:[]
     *   - type parameter of `Collection` -> upper:[`String`], lower:[]
     *   - type parameter of `Iterable` -> upper:[`String`], lower:[]
     *
     * If later `Collection<Int>` is passed to this method with the same receiver map, the receiver map would become:
     *
     *   - type parameter of `List` -> upper:[`String`], lower:[]
     *   - type parameter of `Collection` -> upper:[`String`, `Int`], lower:[]
     *   - type parameter of `Iterable` -> upper:[`String`, `Int`], lower:[]
     */
    private fun MutableMap<FirTypeParameterSymbol, BoundTypeArguments>.collectTypeArgumentMapping(
        coneType: ConeClassLikeType,
        ctx: ConeInferenceContext,
        compatibilityUpperBound: Compatibility
    ) {
        val queue = ArrayDeque<TypeArgumentMapping>()
        queue.addLast(coneType.toTypeArgumentMapping(ctx) ?: return)
        while (queue.isNotEmpty()) {
            val (typeParameterOwner, mapping) = queue.removeFirst()
            val superTypes = typeParameterOwner.getSuperTypes()
            for (superType in superTypes) {
                queue.addLast(superType.toTypeArgumentMapping(ctx, mapping) ?: continue)
            }
            for ((firTypeParameterRef, boundTypeArgument) in mapping) {
                this.collect(ctx, typeParameterOwner, firTypeParameterRef, boundTypeArgument, compatibilityUpperBound)
            }
        }
    }

    /** Converts type arguments in a [ConeClassLikeType] to a [TypeArgumentMapping]. */
    @OptIn(ExperimentalStdlibApi::class)
    private fun ConeClassLikeType.toTypeArgumentMapping(
        ctx: ConeInferenceContext,
        envMapping: Map<FirTypeParameterSymbol, BoundTypeArgument> = emptyMap(),
    ): TypeArgumentMapping? {
        val typeParameterOwner = getClassLikeElement(ctx) ?: return null
        val mapping = buildMap<FirTypeParameterSymbol, BoundTypeArgument> {
            typeArguments.forEachIndexed { index, coneTypeProjection ->
                val typeParameter = typeParameterOwner.getTypeParameter(index) ?: return@forEachIndexed
                var boundTypeArgument: BoundTypeArgument = when (coneTypeProjection) {
                    // Ignore star since it doesn't provide any constraints.
                    ConeStarProjection -> return@forEachIndexed
                    // Ignore contravariant projection because they induces union types. Hence, whatever type argument should always be
                    // considered compatible.
                    is ConeKotlinTypeProjectionIn -> BoundTypeArgument(coneTypeProjection.type, Variance.IN_VARIANCE)
                    is ConeKotlinTypeProjectionOut -> BoundTypeArgument(coneTypeProjection.type, Variance.OUT_VARIANCE)
                    is ConeKotlinTypeConflictingProjection -> BoundTypeArgument(coneTypeProjection.type, Variance.INVARIANT)
                    is ConeKotlinType ->
                        when (typeParameter.variance) {
                            Variance.IN_VARIANCE -> BoundTypeArgument(coneTypeProjection.type, Variance.IN_VARIANCE)
                            Variance.OUT_VARIANCE -> BoundTypeArgument(coneTypeProjection.type, Variance.OUT_VARIANCE)
                            else -> BoundTypeArgument(coneTypeProjection.type, Variance.INVARIANT)
                        }
                }
                val coneKotlinType = boundTypeArgument.type
                if (coneKotlinType is ConeTypeParameterType) {
                    val envTypeParameter = coneKotlinType.lookupTag.typeParameterSymbol
                    val envTypeArgument = envMapping[envTypeParameter]
                    if (envTypeArgument != null) {
                        boundTypeArgument = envTypeArgument
                    }
                }
                put(typeParameter, boundTypeArgument)
            }
        }
        return TypeArgumentMapping(typeParameterOwner, mapping)
    }

    private fun MutableMap<FirTypeParameterSymbol, BoundTypeArguments>.collect(
        ctx: ConeInferenceContext,
        typeParameterOwner: FirClassLikeSymbol<*>,
        parameter: FirTypeParameterSymbol,
        boundTypeArgument: BoundTypeArgument,
        compatibilityUpperBound: Compatibility,
    ) {
        computeIfAbsent(parameter) {
            // the semantic of type parameter in Enum and KClass are fixed: values of types with incompatible type parameters are always
            // incompatible.
            val compatibilityUpperBoundForTypeArg =
                if ((ctx.prohibitComparisonOfIncompatibleEnums && typeParameterOwner.classId == StandardClassIds.Enum) ||
                    (ctx.prohibitComparisonOfIncompatibleClasses && typeParameterOwner.classId == StandardClassIds.KClass)
                ) {
                    compatibilityUpperBound
                } else {
                    Compatibility.SOFT_INCOMPATIBLE
                }
            BoundTypeArguments(mutableSetOf(), mutableSetOf(), compatibilityUpperBoundForTypeArg)
        }.let {
            val type = boundTypeArgument.type
            if (boundTypeArgument.variance.allowsInPosition) {
                it.lower += type.collectLowerBounds()
            }
            if (boundTypeArgument.variance.allowsOutPosition) {
                it.upper += type.collectUpperBounds()
            }
        }
    }

    private fun FirClassLikeSymbol<*>.getSuperTypes(): List<ConeClassLikeType> {
        return when (this) {
            is FirTypeAliasSymbol -> listOfNotNull(resolvedExpandedTypeRef.coneTypeSafe())
            is FirClassSymbol<*> -> resolvedSuperTypeRefs.mapNotNull { it.coneTypeSafe() }
            else -> emptyList()
        }
    }

    private fun ConeClassLikeType.getClassLikeElement(ctx: ConeInferenceContext): FirClassLikeSymbol<*>? =
        ctx.symbolProvider.getSymbolByLookupTag(lookupTag)

    private fun FirClassLikeSymbol<*>.getTypeParameter(index: Int): FirTypeParameterSymbol? {
        return when (this) {
            is FirTypeAliasSymbol -> typeParameterSymbols[index]
            is FirClassSymbol<*> -> typeParameterSymbols[index]
            else -> return null
        }
    }

    /** A class declaration and the arguments bound to the declared type parameters. */
    private data class TypeArgumentMapping(
        val typeParameterOwner: FirClassLikeSymbol<*>,
        val mapping: Map<FirTypeParameterSymbol, BoundTypeArgument>
    )

    /** A single bound type argument to a type parameter declared in a class. */
    private data class BoundTypeArgument(val type: ConeKotlinType, val variance: Variance)

    /** Accumulated type arguments bound to a type parameter declared in a class. */
    private data class BoundTypeArguments(
        val upper: MutableSet<ConeClassLikeType>,
        val lower: MutableSet<ConeClassLikeType>,
        val compatibilityUpperBound: Compatibility
    )

    private fun ConeClassLikeType.toFirClassWithSuperClasses(ctx: ConeInferenceContext): FirClassWithSuperClasses? {
        return lookupTag.toFirClassWithSuperClasses(ctx)
    }

    private fun ConeClassLikeLookupTag.toFirClassWithSuperClasses(
        ctx: ConeInferenceContext
    ): FirClassWithSuperClasses? = when (val klass = ctx.symbolProvider.getSymbolByLookupTag(this)) {
        is FirTypeAliasSymbol -> klass.fullyExpandedClass(ctx.session)?.let { FirClassWithSuperClasses(it, ctx) }
        is FirClassSymbol<*> -> FirClassWithSuperClasses(klass, ctx)
        else -> null
    }

    private data class FirClassWithSuperClasses(val firClass: FirClassSymbol<*>, val ctx: ConeInferenceContext) {
        val isInterface: Boolean get() = firClass.isInterface

        val superClasses: Set<FirClassWithSuperClasses> by lazy {
            firClass.superConeTypes.mapNotNull { it.lookupTag.toFirClassWithSuperClasses(ctx) }.toSet()
        }

        @OptIn(ExperimentalStdlibApi::class)
        val thisAndAllSuperClasses: Set<FirClassWithSuperClasses> by lazy {
            val queue = ArrayDeque<FirClassWithSuperClasses>()
            queue.addLast(this)
            buildSet {
                add(this@FirClassWithSuperClasses)
                while (queue.isNotEmpty()) {
                    val current = queue.removeFirst()
                    val superTypes = current.superClasses
                    superTypes.filterNotTo(queue) { it in this@buildSet }
                    addAll(superTypes)
                }
            }
        }

        val isFinal: Boolean get() = firClass.isFinal

        /**
         * The following are considered to have a predefined equality contract:
         *   - enums
         *   - primitives (including unsigned integer types)
         *   - classes
         *   - strings
         *   - objects of data classes
         *   - objects of inline classes
         *   - kotlin.Unit
         */
        fun getHasPredefinedEqualityContract(ctx: ConeInferenceContext): Boolean {
            return (ctx.prohibitComparisonOfIncompatibleEnums && (firClass.isEnumClass || firClass.classId == StandardClassIds.Enum)) ||
                    firClass.isPrimitiveType() ||
                    (ctx.prohibitComparisonOfIncompatibleClasses && firClass.classId == StandardClassIds.KClass) ||
                    firClass.classId == StandardClassIds.String || firClass.classId == StandardClassIds.Unit ||
                    (firClass is FirRegularClassSymbol && (firClass.isData || firClass.isInline))
        }

        private val FirClassSymbol<*>.isFinal: Boolean
            get() {
                return when (this) {
                    is FirAnonymousObjectSymbol -> true
                    is FirRegularClassSymbol -> modality == Modality.FINAL
                    else -> error("unknown type of FirClass $this")
                }
            }
    }

    private val ConeInferenceContext.prohibitComparisonOfIncompatibleEnums: Boolean
        get() = session.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitComparisonOfIncompatibleEnums)

    private val ConeInferenceContext.prohibitComparisonOfIncompatibleClasses: Boolean
        get() = session.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitComparisonOfIncompatibleClasses)
}
