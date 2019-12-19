/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.types.AbstractTypeCheckerContext.LowerCapturedTypePolicy.*
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext.SeveralSupertypesWithSameConstructorPolicy
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext.SupertypesPolicy
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.SmartSet
import java.util.*


abstract class AbstractTypeCheckerContext : TypeSystemContext {


    abstract fun substitutionSupertypePolicy(type: SimpleTypeMarker): SupertypesPolicy

    abstract fun areEqualTypeConstructors(a: TypeConstructorMarker, b: TypeConstructorMarker): Boolean

    override fun prepareType(type: KotlinTypeMarker): KotlinTypeMarker {
        return type
    }

    open fun refineType(type: KotlinTypeMarker): KotlinTypeMarker {
        return type
    }

    abstract val isErrorTypeEqualsToAnything: Boolean

    abstract val isStubTypeEqualsToAnything: Boolean

    protected var argumentsDepth = 0


    internal inline fun <T> runWithArgumentsSettings(subArgument: KotlinTypeMarker, f: AbstractTypeCheckerContext.() -> T): T {
        if (argumentsDepth > 100) {
            error("Arguments depth is too high. Some related argument: $subArgument")
        }

        argumentsDepth++
        val result = f()
        argumentsDepth--
        return result
    }

    open fun getLowerCapturedTypePolicy(subType: SimpleTypeMarker, superType: CapturedTypeMarker): LowerCapturedTypePolicy = CHECK_SUBTYPE_AND_LOWER
    open fun addSubtypeConstraint(subType: KotlinTypeMarker, superType: KotlinTypeMarker): Boolean? = null
    open val sameConstructorPolicy get() = SeveralSupertypesWithSameConstructorPolicy.INTERSECT_ARGUMENTS_AND_CHECK_AGAIN

    enum class SeveralSupertypesWithSameConstructorPolicy {
        TAKE_FIRST_FOR_SUBTYPING,
        FORCE_NOT_SUBTYPE,
        CHECK_ANY_OF_THEM,
        INTERSECT_ARGUMENTS_AND_CHECK_AGAIN
    }

    enum class LowerCapturedTypePolicy {
        CHECK_ONLY_LOWER,
        CHECK_SUBTYPE_AND_LOWER,
        SKIP_LOWER
    }

    private var supertypesLocked = false

    var supertypesDeque: ArrayDeque<SimpleTypeMarker>? = null
        private set
    var supertypesSet: MutableSet<SimpleTypeMarker>? = null
        private set


    fun initialize() {
        assert(!supertypesLocked)
        supertypesLocked = true

        if (supertypesDeque == null) {
            supertypesDeque = ArrayDeque(4)
        }
        if (supertypesSet == null) {
            supertypesSet = SmartSet.create()
        }
    }

    fun clear() {
        supertypesDeque!!.clear()
        supertypesSet!!.clear()
        supertypesLocked = false
    }

    inline fun anySupertype(
        start: SimpleTypeMarker,
        predicate: (SimpleTypeMarker) -> Boolean,
        supertypesPolicy: (SimpleTypeMarker) -> SupertypesPolicy
    ): Boolean {
        if (predicate(start)) return true

        initialize()

        val deque = supertypesDeque!!
        val visitedSupertypes = supertypesSet!!

        deque.push(start)
        while (deque.isNotEmpty()) {
            if (visitedSupertypes.size > 1000) {
                error("Too many supertypes for type: $start. Supertypes = ${visitedSupertypes.joinToString()}")
            }
            val current = deque.pop()
            if (!visitedSupertypes.add(current)) continue

            val policy = supertypesPolicy(current).takeIf { it != SupertypesPolicy.None } ?: continue
            for (supertype in current.typeConstructor().supertypes()) {
                val newType = policy.transformType(this, supertype)
                if (predicate(newType)) {
                    clear()
                    return true
                }
                deque.add(newType)
            }
        }

        clear()
        return false
    }

    sealed class SupertypesPolicy {
        abstract fun transformType(context: AbstractTypeCheckerContext, type: KotlinTypeMarker): SimpleTypeMarker

        object None : SupertypesPolicy() {
            override fun transformType(context: AbstractTypeCheckerContext, type: KotlinTypeMarker) =
                throw UnsupportedOperationException("Should not be called")
        }

        object UpperIfFlexible : SupertypesPolicy() {
            override fun transformType(context: AbstractTypeCheckerContext, type: KotlinTypeMarker) =
                with(context) { type.upperBoundIfFlexible() }
        }

        object LowerIfFlexible : SupertypesPolicy() {
            override fun transformType(context: AbstractTypeCheckerContext, type: KotlinTypeMarker) =
                with(context) { type.lowerBoundIfFlexible() }
        }

        abstract class DoCustomTransform : SupertypesPolicy()
    }

    abstract val KotlinTypeMarker.isAllowedTypeVariable: Boolean
}

object AbstractTypeChecker {
    @JvmField
    var RUN_SLOW_ASSERTIONS = false

    fun isSubtypeOf(
        context: TypeCheckerProviderContext,
        subType: KotlinTypeMarker,
        superType: KotlinTypeMarker,
        stubTypesEqualToAnything: Boolean = true
    ): Boolean {
        return AbstractTypeChecker.isSubtypeOf(context.newBaseTypeCheckerContext(true, stubTypesEqualToAnything), subType, superType)
    }

    fun equalTypes(
        context: TypeCheckerProviderContext,
        a: KotlinTypeMarker,
        b: KotlinTypeMarker,
        stubTypesEqualToAnything: Boolean = true
    ): Boolean {
        return AbstractTypeChecker.equalTypes(context.newBaseTypeCheckerContext(false, stubTypesEqualToAnything), a, b)
    }

    fun isSubtypeOf(context: AbstractTypeCheckerContext, subType: KotlinTypeMarker, superType: KotlinTypeMarker): Boolean {
        if (subType === superType) return true
        return with(context) { completeIsSubTypeOf(prepareType(refineType(subType)), prepareType(refineType(superType))) }
    }

    fun equalTypes(context: AbstractTypeCheckerContext, a: KotlinTypeMarker, b: KotlinTypeMarker): Boolean = with(context) {
        if (a === b) return true

        if (isCommonDenotableType(a) && isCommonDenotableType(b)) {
            val refinedA = refineType(a)
            val refinedB = refineType(b)
            val simpleA = refinedA.lowerBoundIfFlexible()
            if (!areEqualTypeConstructors(refinedA.typeConstructor(), refinedB.typeConstructor())) return false
            if (simpleA.argumentsCount() == 0) {
                if (refinedA.hasFlexibleNullability() || refinedB.hasFlexibleNullability()) return true

                return simpleA.isMarkedNullable() == refinedB.lowerBoundIfFlexible().isMarkedNullable()
            }
        }

        return isSubtypeOf(context, a, b) && isSubtypeOf(context, b, a)
    }


    private fun AbstractTypeCheckerContext.completeIsSubTypeOf(subType: KotlinTypeMarker, superType: KotlinTypeMarker): Boolean {
        checkSubtypeForSpecialCases(subType.lowerBoundIfFlexible(), superType.upperBoundIfFlexible())?.let {
            addSubtypeConstraint(subType, superType)
            return it
        }

        // we should add constraints with flexible types, otherwise we never get flexible type as answer in constraint system
        addSubtypeConstraint(subType, superType)?.let { return it }

        return isSubtypeOfForSingleClassifierType(subType.lowerBoundIfFlexible(), superType.upperBoundIfFlexible())
    }

    private fun AbstractTypeCheckerContext.checkSubtypeForIntegerLiteralType(subType: SimpleTypeMarker, superType: SimpleTypeMarker): Boolean? {
        if (!subType.isIntegerLiteralType() && !superType.isIntegerLiteralType()) return null

        fun typeInIntegerLiteralType(integerLiteralType: SimpleTypeMarker, type: SimpleTypeMarker, checkSupertypes: Boolean): Boolean =
            integerLiteralType.possibleIntegerTypes().any { possibleType ->
                (possibleType.typeConstructor() == type.typeConstructor()) || (checkSupertypes && isSubtypeOf(this, type, possibleType))
            }

        when {
            subType.isIntegerLiteralType() && superType.isIntegerLiteralType() -> {
                return true
            }

            subType.isIntegerLiteralType() -> {
                if (typeInIntegerLiteralType(subType, superType, checkSupertypes = false)) {
                    return true
                }
            }

            superType.isIntegerLiteralType() -> {
                // Here we also have to check supertypes for intersection types: { Int & String } <: IntegerLiteralTypes
                if (typeInIntegerLiteralType(superType, subType, checkSupertypes = true)) {
                    return true
                }
            }
        }
        return null
    }

    private fun AbstractTypeCheckerContext.hasNothingSupertype(type: SimpleTypeMarker): Boolean {
        val typeConstructor = type.typeConstructor()
        if (typeConstructor.isClassTypeConstructor()) {
            return typeConstructor.isNothingConstructor()
        }
        return anySupertype(type, { it.typeConstructor().isNothingConstructor() }) {
            if (it.isClassType()) {
                SupertypesPolicy.None
            } else {
                SupertypesPolicy.LowerIfFlexible
            }
        }
    }

    private fun AbstractTypeCheckerContext.isSubtypeOfForSingleClassifierType(subType: SimpleTypeMarker, superType: SimpleTypeMarker): Boolean {
        if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
            assert(subType.isSingleClassifierType() || subType.typeConstructor().isIntersection() || subType.isAllowedTypeVariable) {
                "Not singleClassifierType and not intersection subType: $subType"
            }
            assert(superType.isSingleClassifierType() || superType.isAllowedTypeVariable) {
                "Not singleClassifierType superType: $superType"
            }
        }

        if (!AbstractNullabilityChecker.isPossibleSubtype(this, subType, superType)) return false

        checkSubtypeForIntegerLiteralType(subType.lowerBoundIfFlexible(), superType.upperBoundIfFlexible())?.let {
            addSubtypeConstraint(subType, superType)
            return it
        }

        val superConstructor = superType.typeConstructor()

        if (isEqualTypeConstructors(subType.typeConstructor(), superConstructor) && superConstructor.parametersCount() == 0) return true
        if (superType.typeConstructor().isAnyConstructor()) return true

        val supertypesWithSameConstructor = findCorrespondingSupertypes(subType, superConstructor)
        when (supertypesWithSameConstructor.size) {
            0 -> return hasNothingSupertype(subType) // todo Nothing & Array<Number> <: Array<String>
            1 -> return isSubtypeForSameConstructor(supertypesWithSameConstructor.first().asArgumentList(), superType)

            else -> { // at least 2 supertypes with same constructors. Such case is rare
                when (sameConstructorPolicy) {
                    SeveralSupertypesWithSameConstructorPolicy.FORCE_NOT_SUBTYPE -> return false
                    SeveralSupertypesWithSameConstructorPolicy.TAKE_FIRST_FOR_SUBTYPING -> return isSubtypeForSameConstructor(
                        supertypesWithSameConstructor.first().asArgumentList(),
                        superType
                    )

                    SeveralSupertypesWithSameConstructorPolicy.CHECK_ANY_OF_THEM,
                    SeveralSupertypesWithSameConstructorPolicy.INTERSECT_ARGUMENTS_AND_CHECK_AGAIN ->
                        if (supertypesWithSameConstructor.any { isSubtypeForSameConstructor(it.asArgumentList(), superType) }) return true
                }

                if (sameConstructorPolicy != SeveralSupertypesWithSameConstructorPolicy.INTERSECT_ARGUMENTS_AND_CHECK_AGAIN) return false


                val newArguments = ArgumentList(superConstructor.parametersCount())
                for (index in 0 until superConstructor.parametersCount()) {
                    val allProjections = supertypesWithSameConstructor.map {
                        it.getArgumentOrNull(index)?.takeIf { it.getVariance() == TypeVariance.INV }?.getType()
                            ?: error("Incorrect type: $it, subType: $subType, superType: $superType")
                    }

                    // todo discuss
                    val intersection = intersectTypes(allProjections).asTypeArgument()
                    newArguments.add(intersection)
                }

                return isSubtypeForSameConstructor(newArguments, superType)
            }
        }
    }

    fun AbstractTypeCheckerContext.isSubtypeForSameConstructor(
        capturedSubArguments: TypeArgumentListMarker,
        superType: SimpleTypeMarker
    ): Boolean {
        // No way to check, as no index sometimes
        //if (capturedSubArguments === superType.arguments) return true

        //val parameters = superType.constructor.parameters
        val superTypeConstructor = superType.typeConstructor()
        for (index in 0 until superTypeConstructor.parametersCount()) {
            val superProjection = superType.getArgument(index) // todo error index
            if (superProjection.isStarProjection()) continue // A<B> <: A<*>

            val superArgumentType = superProjection.getType()
            val subArgumentType = capturedSubArguments[index].let {
                assert(it.getVariance() == TypeVariance.INV) { "Incorrect sub argument: $it" }
                it.getType()
            }

            val variance = effectiveVariance(superTypeConstructor.getParameter(index).getVariance(), superProjection.getVariance())
                ?: return isErrorTypeEqualsToAnything // todo exception?

            val correctArgument = runWithArgumentsSettings(subArgumentType) {
                when (variance) {
                    TypeVariance.INV -> equalTypes(this, subArgumentType, superArgumentType)
                    TypeVariance.OUT -> isSubtypeOf(this, subArgumentType, superArgumentType)
                    TypeVariance.IN -> isSubtypeOf(this, superArgumentType, subArgumentType)
                }
            }
            if (!correctArgument) return false
        }
        return true
    }

    private fun AbstractTypeCheckerContext.isCommonDenotableType(type: KotlinTypeMarker): Boolean =
        type.typeConstructor().isDenotable() &&
                !type.isDynamic() && !type.isDefinitelyNotNullType() &&
                type.lowerBoundIfFlexible().typeConstructor() == type.upperBoundIfFlexible().typeConstructor()

    fun effectiveVariance(declared: TypeVariance, useSite: TypeVariance): TypeVariance? {
        if (declared == TypeVariance.INV) return useSite
        if (useSite == TypeVariance.INV) return declared

        // both not INVARIANT
        if (declared == useSite) return declared

        // composite In with Out
        return null
    }

    private fun AbstractTypeCheckerContext.checkSubtypeForSpecialCases(subType: SimpleTypeMarker, superType: SimpleTypeMarker): Boolean? {
        if (subType.isError() || superType.isError()) {
            if (isErrorTypeEqualsToAnything) return true

            if (subType.isMarkedNullable() && !superType.isMarkedNullable()) return false

            return AbstractStrictEqualityTypeChecker.strictEqualTypes(
                this,
                subType.withNullability(false),
                superType.withNullability(false)
            )
        }

        if (subType.isStubType() || superType.isStubType()) return isStubTypeEqualsToAnything

        val superTypeCaptured = superType.asCapturedType()
        val lowerType = superTypeCaptured?.lowerType()
        if (superTypeCaptured != null && lowerType != null) {
            when (getLowerCapturedTypePolicy(subType, superTypeCaptured)) {
                CHECK_ONLY_LOWER -> return isSubtypeOf(this, subType, lowerType)
                CHECK_SUBTYPE_AND_LOWER -> if (isSubtypeOf(this, subType, lowerType)) return true
                SKIP_LOWER -> Unit
            }
        }

        val superTypeConstructor = superType.typeConstructor()
        if (superTypeConstructor.isIntersection()) {
            assert(!superType.isMarkedNullable()) { "Intersection type should not be marked nullable!: $superType" }

            return superTypeConstructor.supertypes().all { isSubtypeOf(this, subType, it) }
        }

        return null
    }


    private fun AbstractTypeCheckerContext.collectAllSupertypesWithGivenTypeConstructor(
        subType: SimpleTypeMarker,
        superConstructor: TypeConstructorMarker
    ): List<SimpleTypeMarker> {
        subType.fastCorrespondingSupertypes(superConstructor)?.let {
            return it
        }

        if (!superConstructor.isClassTypeConstructor() && subType.isClassType()) return emptyList()

        if (superConstructor.isCommonFinalClassConstructor()) {
            return if (areEqualTypeConstructors(subType.typeConstructor(), superConstructor))
                listOf(captureFromArguments(subType, CaptureStatus.FOR_SUBTYPING) ?: subType)
            else
                emptyList()
        }

        val result: MutableList<SimpleTypeMarker> = SmartList()

        anySupertype(subType, { false }) {

            val current = captureFromArguments(it, CaptureStatus.FOR_SUBTYPING) ?: it

            when {
                areEqualTypeConstructors(current.typeConstructor(), superConstructor) -> {
                    result.add(current)
                    SupertypesPolicy.None
                }
                current.argumentsCount() == 0 -> {
                    SupertypesPolicy.LowerIfFlexible
                }
                else -> {
                    substitutionSupertypePolicy(current)
                }
            }
        }

        return result
    }

    private fun AbstractTypeCheckerContext.collectAndFilter(classType: SimpleTypeMarker, constructor: TypeConstructorMarker) =
        selectOnlyPureKotlinSupertypes(collectAllSupertypesWithGivenTypeConstructor(classType, constructor))


    /**
     * If we have several paths to some interface, we should prefer pure kotlin path.
     * Example:
     *
     * class MyList : AbstractList<String>(), MutableList<String>
     *
     * We should see `String` in `get` function and others, also MyList is not subtype of MutableList<String?>
     *
     * More tests: javaAndKotlinSuperType & purelyImplementedCollection folder
     */
    private fun AbstractTypeCheckerContext.selectOnlyPureKotlinSupertypes(supertypes: List<SimpleTypeMarker>): List<SimpleTypeMarker> {
        if (supertypes.size < 2) return supertypes

        val allPureSupertypes = supertypes.filter {
            it.asArgumentList().all(this) { it.getType().asFlexibleType() == null }
        }
        return if (allPureSupertypes.isNotEmpty()) allPureSupertypes else supertypes
    }


    // nullability was checked earlier via nullabilityChecker
    // should be used only if you really sure that it is correct
    fun AbstractTypeCheckerContext.findCorrespondingSupertypes(
        subType: SimpleTypeMarker,
        superConstructor: TypeConstructorMarker
    ): List<SimpleTypeMarker> {
        if (subType.isClassType()) {
            return collectAndFilter(subType, superConstructor)
        }

        // i.e. superType is not a classType
        if (!superConstructor.isClassTypeConstructor() && !superConstructor.isIntegerLiteralTypeConstructor()) {
            return collectAllSupertypesWithGivenTypeConstructor(subType, superConstructor)
        }

        // todo add tests
        val classTypeSupertypes = SmartList<SimpleTypeMarker>()
        anySupertype(subType, { false }) {
            if (it.isClassType()) {
                classTypeSupertypes.add(it)
                SupertypesPolicy.None
            } else {
                SupertypesPolicy.LowerIfFlexible
            }
        }

        return classTypeSupertypes.flatMap { collectAndFilter(it, superConstructor) }
    }
}


object AbstractNullabilityChecker {
    // this method checks only nullability
    fun isPossibleSubtype(context: AbstractTypeCheckerContext, subType: SimpleTypeMarker, superType: SimpleTypeMarker): Boolean =
        context.runIsPossibleSubtype(subType, superType)

    fun isSubtypeOfAny(context: TypeCheckerProviderContext, type: KotlinTypeMarker): Boolean =
        AbstractNullabilityChecker.isSubtypeOfAny(
            context.newBaseTypeCheckerContext(
                errorTypesEqualToAnything = false,
                stubTypesEqualToAnything = true
            ),
            type
        )

    fun isSubtypeOfAny(context: AbstractTypeCheckerContext, type: KotlinTypeMarker): Boolean =
        with(context) {
            hasNotNullSupertype(type.lowerBoundIfFlexible(), SupertypesPolicy.LowerIfFlexible)
        }

    private fun AbstractTypeCheckerContext.runIsPossibleSubtype(subType: SimpleTypeMarker, superType: SimpleTypeMarker): Boolean {
        if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
            // it makes for case String? & Any <: String
            assert(subType.isSingleClassifierType() || subType.typeConstructor().isIntersection() || subType.isAllowedTypeVariable) {
                "Not singleClassifierType and not intersection subType: $subType"
            }
            assert(superType.isSingleClassifierType() || superType.isAllowedTypeVariable) {
                "Not singleClassifierType superType: $superType"
            }
        }

        // superType is actually nullable
        if (superType.isMarkedNullable()) return true

        // i.e. subType is definitely not null
        if (subType.isDefinitelyNotNullType()) return true

        // i.e. subType is not-nullable
        if (hasNotNullSupertype(subType, SupertypesPolicy.LowerIfFlexible)) return true

        // i.e. subType hasn't not-null supertype and isn't definitely not-null, but superType is definitely not-null
        if (superType.isDefinitelyNotNullType()) return false

        // i.e subType hasn't not-null supertype, but superType has
        if (hasNotNullSupertype(superType, SupertypesPolicy.UpperIfFlexible)) return false

        // both superType and subType hasn't not-null supertype and are not definitely not null.

        /**
         * If we still don't know, it means, that superType is not classType, for example -- type parameter.
         *
         * For captured types with lower bound this function can give to you false result. Example:
         *  class A<T>, A<in Number> => \exist Q : Number <: Q. A<Q>
         *      isPossibleSubtype(Number, Q) = false.
         *      Such cases should be taken in to account in [NewKotlinTypeChecker.isSubtypeOf] (same for intersection types)
         */

        // classType cannot has special type in supertype list
        if (subType.isClassType()) return false

        return hasPathByNotMarkedNullableNodes(subType, superType.typeConstructor())
    }

    fun AbstractTypeCheckerContext.hasNotNullSupertype(type: SimpleTypeMarker, supertypesPolicy: SupertypesPolicy) =
        anySupertype(type, {
            (it.isClassType() && !it.isMarkedNullable()) || it.isDefinitelyNotNullType()
        }) {
            if (it.isMarkedNullable()) SupertypesPolicy.None else supertypesPolicy
        }

    fun TypeCheckerProviderContext.hasPathByNotMarkedNullableNodes(start: SimpleTypeMarker, end: TypeConstructorMarker) =
        newBaseTypeCheckerContext(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true)
            .hasPathByNotMarkedNullableNodes(start, end)

    fun AbstractTypeCheckerContext.hasPathByNotMarkedNullableNodes(start: SimpleTypeMarker, end: TypeConstructorMarker) =
        anySupertype(
            start,
            { isApplicableAsEndNode(it, end) },
            { if (it.isMarkedNullable()) SupertypesPolicy.None else SupertypesPolicy.LowerIfFlexible }
        )

    private fun AbstractTypeCheckerContext.isApplicableAsEndNode(type: SimpleTypeMarker, end: TypeConstructorMarker): Boolean {
        if (type.isNothing()) return true
        if (type.isMarkedNullable()) return false

        if (isStubTypeEqualsToAnything && type.isStubType()) return true

        return isEqualTypeConstructors(type.typeConstructor(), end)
    }
}


object AbstractFlexibilityChecker {
    fun TypeSystemCommonSuperTypesContext.hasDifferentFlexibilityAtDepth(types: Collection<KotlinTypeMarker>): Boolean {
        if (types.isEmpty()) return false
        if (hasDifferentFlexibility(types)) return true

        for (i in 0 until types.first().argumentsCount()) {
            val typeArgumentForOtherTypes = types.mapNotNull {
                if (it.argumentsCount() > i && !it.getArgument(i).isStarProjection()) it.getArgument(i).getType() else null
            }

            if (hasDifferentFlexibilityAtDepth(typeArgumentForOtherTypes)) return true
        }

        return false
    }

    private fun TypeSystemCommonSuperTypesContext.hasDifferentFlexibility(types: Collection<KotlinTypeMarker>): Boolean {
        val firstType = types.first()
        if (types.all { it === firstType }) return false

        return !types.all { it.isFlexible() } && !types.all { !it.isFlexible() }
    }
}
