/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.types.TypeCheckerState.LowerCapturedTypePolicy.*
import org.jetbrains.kotlin.types.TypeCheckerState.SupertypesPolicy
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.SmartSet
import java.util.*

/**
 * Context that defines how type-checker operates, stores type-checker state,
 * created by [TypeCheckerProviderContext.newTypeCheckerState] in most cases
 *
 * Stateful and shouldn't be reused
 *
 * Once some type-checker operation is performed using a [TypeCheckerProviderContext], for example a [AbstractTypeChecker.isSubtypeOf],
 * new instance of particular [TypeCheckerState] should be created, with properly specified type system context
 */
abstract class TypeCheckerState {
    abstract val kotlinTypePreparator: AbstractTypePreparator
    abstract val kotlinTypeRefiner: AbstractTypeRefiner

    abstract val typeSystemContext: TypeSystemContext

    abstract fun substitutionSupertypePolicy(type: SimpleTypeMarker): SupertypesPolicy

    @OptIn(TypeRefinement::class)
    fun refineType(type: KotlinTypeMarker): KotlinTypeMarker {
        return kotlinTypeRefiner.refineType(type)
    }

    fun prepareType(type: KotlinTypeMarker): KotlinTypeMarker {
        return kotlinTypePreparator.prepareType(type)
    }

    open fun customIsSubtypeOf(subType: KotlinTypeMarker, superType: KotlinTypeMarker): Boolean = true

    abstract val isErrorTypeEqualsToAnything: Boolean

    abstract val isStubTypeEqualsToAnything: Boolean

    protected var argumentsDepth = 0

    internal inline fun <T> runWithArgumentsSettings(subArgument: KotlinTypeMarker, f: TypeCheckerState.() -> T): T {
        if (argumentsDepth > 100) {
            error("Arguments depth is too high. Some related argument: $subArgument")
        }

        argumentsDepth++
        val result = f()
        argumentsDepth--
        return result
    }

    open fun getLowerCapturedTypePolicy(subType: SimpleTypeMarker, superType: CapturedTypeMarker): LowerCapturedTypePolicy =
        CHECK_SUBTYPE_AND_LOWER

    open fun addSubtypeConstraint(
        subType: KotlinTypeMarker,
        superType: KotlinTypeMarker,
        isFromNullabilityConstraint: Boolean = false
    ): Boolean? = null

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
        assert(!supertypesLocked) {
            "Supertypes were locked for ${this::class}"
        }
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
            val supertypes = with(typeSystemContext) { current.typeConstructor().supertypes() }
            for (supertype in supertypes) {
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
        abstract fun transformType(state: TypeCheckerState, type: KotlinTypeMarker): SimpleTypeMarker

        object None : SupertypesPolicy() {
            override fun transformType(state: TypeCheckerState, type: KotlinTypeMarker) =
                throw UnsupportedOperationException("Should not be called")
        }

        object UpperIfFlexible : SupertypesPolicy() {
            override fun transformType(state: TypeCheckerState, type: KotlinTypeMarker) =
                with(state.typeSystemContext) { type.upperBoundIfFlexible() }
        }

        object LowerIfFlexible : SupertypesPolicy() {
            override fun transformType(state: TypeCheckerState, type: KotlinTypeMarker) =
                with(state.typeSystemContext) { type.lowerBoundIfFlexible() }
        }

        abstract class DoCustomTransform : SupertypesPolicy()
    }

    abstract val KotlinTypeMarker.isAllowedTypeVariable: Boolean

    @JvmName("isAllowedTypeVariableBridge")
    fun isAllowedTypeVariable(type: KotlinTypeMarker): Boolean = type.isAllowedTypeVariable
}

object AbstractTypeChecker {
    @JvmField
    var RUN_SLOW_ASSERTIONS = false

    fun prepareType(
        context: TypeCheckerProviderContext,
        type: KotlinTypeMarker,
        stubTypesEqualToAnything: Boolean = true
    ) = context.newTypeCheckerState(true, stubTypesEqualToAnything).prepareType(type)

    fun isSubtypeOf(
        context: TypeCheckerProviderContext,
        subType: KotlinTypeMarker,
        superType: KotlinTypeMarker,
        stubTypesEqualToAnything: Boolean = true
    ): Boolean {
        return isSubtypeOf(context.newTypeCheckerState(true, stubTypesEqualToAnything), subType, superType)
    }

    fun isSubtypeOfClass(
        state: TypeCheckerState,
        typeConstructor: TypeConstructorMarker,
        superConstructor: TypeConstructorMarker
    ): Boolean {
        if (typeConstructor == superConstructor) return true
        with(state.typeSystemContext) {
            for (superType in typeConstructor.supertypes()) {
                if (isSubtypeOfClass(state, superType.typeConstructor(), superConstructor)) {
                    return true
                }
            }
        }
        return false
    }

    fun equalTypes(
        context: TypeCheckerProviderContext,
        a: KotlinTypeMarker,
        b: KotlinTypeMarker,
        stubTypesEqualToAnything: Boolean = true
    ): Boolean {
        return equalTypes(context.newTypeCheckerState(false, stubTypesEqualToAnything), a, b)
    }

    fun isSubtypeOf(
        state: TypeCheckerState,
        subType: KotlinTypeMarker,
        superType: KotlinTypeMarker,
        isFromNullabilityConstraint: Boolean = false
    ): Boolean {
        if (subType === superType) return true

        if (!state.customIsSubtypeOf(subType, superType)) return false

        return completeIsSubTypeOf(state, subType, superType, isFromNullabilityConstraint)
    }

    fun equalTypes(state: TypeCheckerState, a: KotlinTypeMarker, b: KotlinTypeMarker): Boolean =
        with(state.typeSystemContext) {
            if (a === b) return true

            if (isCommonDenotableType(a) && isCommonDenotableType(b)) {
                val refinedA = state.prepareType(state.refineType(a))
                val refinedB = state.prepareType(state.refineType(b))
                val simpleA = refinedA.lowerBoundIfFlexible()
                if (!areEqualTypeConstructors(refinedA.typeConstructor(), refinedB.typeConstructor())) return false
                if (simpleA.argumentsCount() == 0) {
                    if (refinedA.hasFlexibleNullability() || refinedB.hasFlexibleNullability()) return true

                    return simpleA.isMarkedNullable() == refinedB.lowerBoundIfFlexible().isMarkedNullable()
                }
            }

            return isSubtypeOf(state, a, b) && isSubtypeOf(state, b, a)
        }


    private fun completeIsSubTypeOf(
        state: TypeCheckerState,
        subType: KotlinTypeMarker,
        superType: KotlinTypeMarker,
        isFromNullabilityConstraint: Boolean
    ): Boolean = with(state.typeSystemContext) {
        val preparedSubType = state.prepareType(state.refineType(subType))
        val preparedSuperType = state.prepareType(state.refineType(superType))

        checkSubtypeForSpecialCases(state, preparedSubType.lowerBoundIfFlexible(), preparedSuperType.upperBoundIfFlexible())?.let {
            state.addSubtypeConstraint(preparedSubType, preparedSuperType, isFromNullabilityConstraint)
            return it
        }

        // we should add constraints with flexible types, otherwise we never get flexible type as answer in constraint system
        state.addSubtypeConstraint(preparedSubType, preparedSuperType, isFromNullabilityConstraint)?.let { return it }

        return isSubtypeOfForSingleClassifierType(state, preparedSubType.lowerBoundIfFlexible(), preparedSuperType.upperBoundIfFlexible())
    }

    private fun checkSubtypeForIntegerLiteralType(
        state: TypeCheckerState,
        subType: SimpleTypeMarker,
        superType: SimpleTypeMarker
    ): Boolean? = with(state.typeSystemContext) {
        if (!subType.isIntegerLiteralType() && !superType.isIntegerLiteralType()) return null

        fun isTypeInIntegerLiteralType(integerLiteralType: SimpleTypeMarker, type: SimpleTypeMarker, checkSupertypes: Boolean): Boolean =
            integerLiteralType.possibleIntegerTypes().any { possibleType ->
                (possibleType.typeConstructor() == type.typeConstructor()) || (checkSupertypes && isSubtypeOf(state, type, possibleType))
            }

        fun isIntegerLiteralTypeInIntersectionComponents(type: SimpleTypeMarker): Boolean {
            val typeConstructor = type.typeConstructor()

            return typeConstructor is IntersectionTypeConstructorMarker
                    && typeConstructor.supertypes().any { it.asSimpleType()?.isIntegerLiteralType() == true }
        }

        when {
            subType.isIntegerLiteralType() && superType.isIntegerLiteralType() -> {
                return true
            }

            subType.isIntegerLiteralType() -> {
                if (isTypeInIntegerLiteralType(subType, superType, checkSupertypes = false)) {
                    return true
                }
            }

            superType.isIntegerLiteralType() -> {
                // Here we also have to check supertypes for intersection types: { Int & String } <: IntegerLiteralTypes
                if (isIntegerLiteralTypeInIntersectionComponents(subType)
                    || isTypeInIntegerLiteralType(superType, subType, checkSupertypes = true)
                ) {
                    return true
                }
            }
        }
        return null
    }

    private fun hasNothingSupertype(state: TypeCheckerState, type: SimpleTypeMarker): Boolean = with(state.typeSystemContext) {
        val typeConstructor = type.typeConstructor()
        if (typeConstructor.isClassTypeConstructor()) {
            return typeConstructor.isNothingConstructor()
        }
        return state.anySupertype(type, { it.typeConstructor().isNothingConstructor() }) {
            if (it.isClassType()) {
                SupertypesPolicy.None
            } else {
                SupertypesPolicy.LowerIfFlexible
            }
        }
    }

    private fun isSubtypeOfForSingleClassifierType(
        state: TypeCheckerState,
        subType: SimpleTypeMarker,
        superType: SimpleTypeMarker
    ): Boolean = with(state.typeSystemContext) {
        if (RUN_SLOW_ASSERTIONS) {
            assert(subType.isSingleClassifierType() || subType.typeConstructor().isIntersection() || state.isAllowedTypeVariable(subType)) {
                "Not singleClassifierType and not intersection subType: $subType"
            }
            assert(superType.isSingleClassifierType() || state .isAllowedTypeVariable(superType)) {
                "Not singleClassifierType superType: $superType"
            }
        }

        if (!AbstractNullabilityChecker.isPossibleSubtype(state, subType, superType)) return false

        checkSubtypeForIntegerLiteralType(state, subType.lowerBoundIfFlexible(), superType.upperBoundIfFlexible())?.let {
            state.addSubtypeConstraint(subType, superType)
            return it
        }

        val superConstructor = superType.typeConstructor()

        if (areEqualTypeConstructors(subType.typeConstructor(), superConstructor) && superConstructor.parametersCount() == 0) return true
        if (superType.typeConstructor().isAnyConstructor()) return true

        val supertypesWithSameConstructor = findCorrespondingSupertypes(state, subType, superConstructor)
            .map { state.prepareType(it).asSimpleType() ?: it }
        when (supertypesWithSameConstructor.size) {
            0 -> return hasNothingSupertype(state, subType) // todo Nothing & Array<Number> <: Array<String>
            1 -> return state.isSubtypeForSameConstructor(supertypesWithSameConstructor.first().asArgumentList(), superType)

            else -> { // at least 2 supertypes with same constructors. Such case is rare
                val newArguments = ArgumentList(superConstructor.parametersCount())
                var anyNonOutParameter = false
                for (index in 0 until superConstructor.parametersCount()) {
                    anyNonOutParameter = anyNonOutParameter || superConstructor.getParameter(index).getVariance() != TypeVariance.OUT
                    if (anyNonOutParameter) continue
                    val allProjections = supertypesWithSameConstructor.map {
                        it.getArgumentOrNull(index)?.takeIf { it.getVariance() == TypeVariance.INV }?.getType()
                            ?: error("Incorrect type: $it, subType: $subType, superType: $superType")
                    }

                    // todo discuss
                    val intersection = intersectTypes(allProjections).asTypeArgument()
                    newArguments.add(intersection)
                }

                if (!anyNonOutParameter && state.isSubtypeForSameConstructor(newArguments, superType)) return true

                // TODO: rethink this; now components order in intersection type affects semantic due to run subtyping (which can add constraints) only until the first successful candidate
                return supertypesWithSameConstructor.any { state.isSubtypeForSameConstructor(it.asArgumentList(), superType) }
            }
        }
    }

    private fun TypeSystemContext.isTypeVariableAgainstStarProjectionForSelfType(
        subArgumentType: KotlinTypeMarker,
        superArgumentType: KotlinTypeMarker,
        selfConstructor: TypeConstructorMarker
    ): Boolean {
        val simpleSubArgumentType = subArgumentType.asSimpleType()

        if (simpleSubArgumentType !is CapturedTypeMarker || simpleSubArgumentType.isOldCapturedType()
            || !simpleSubArgumentType.typeConstructor().projection().isStarProjection()
        ) return false
        // Only 'for subtyping' captured types are approximated before adding constraints (see ConstraintInjector.addNewIncorporatedConstraint)
        // that can lead to adding problematic constraints like UPPER(Nothing) given by CapturedType(*) <: TypeVariable(A)
        if (simpleSubArgumentType.captureStatus() != CaptureStatus.FOR_SUBTYPING) return false

        val typeVariableConstructor = superArgumentType.typeConstructor() as? TypeVariableTypeConstructorMarker ?: return false

        return typeVariableConstructor.typeParameter?.hasRecursiveBounds(selfConstructor) == true
    }

    fun TypeCheckerState.isSubtypeForSameConstructor(
        capturedSubArguments: TypeArgumentListMarker,
        superType: SimpleTypeMarker
    ): Boolean = with(this.typeSystemContext) {
        // No way to check, as no index sometimes
        //if (capturedSubArguments === superType.arguments) return true

        val superTypeConstructor = superType.typeConstructor()

        // Sometimes we can get two classes from different modules with different counts of type parameters
        // So for such situations we assume that those types are not sub type of each other
        val argumentsCount = capturedSubArguments.size()
        val parametersCount = superTypeConstructor.parametersCount()
        if (argumentsCount != parametersCount || argumentsCount != superType.argumentsCount()) {
            return false
        }

        for (index in 0 until parametersCount) {
            val superProjection = superType.getArgument(index) // todo error index

            if (superProjection.isStarProjection()) continue // A<B> <: A<*>

            val superArgumentType = superProjection.getType()
            val subArgumentType = capturedSubArguments[index].let {
                assert(it.getVariance() == TypeVariance.INV) { "Incorrect sub argument: $it" }
                it.getType()
            }

            val variance = effectiveVariance(superTypeConstructor.getParameter(index).getVariance(), superProjection.getVariance())
                ?: return isErrorTypeEqualsToAnything // todo exception?

            val isTypeVariableAgainstStarProjectionForSelfType = if (variance == TypeVariance.INV) {
                isTypeVariableAgainstStarProjectionForSelfType(subArgumentType, superArgumentType, superTypeConstructor) ||
                        isTypeVariableAgainstStarProjectionForSelfType(superArgumentType, subArgumentType, superTypeConstructor)
            } else false

            /*
             * We don't check subtyping between types like CapturedType(*) and TypeVariable(E) if the corresponding type parameter forms self type, for instance, Enum<E: Enum<E>>.
             * It can return false and produce unwanted constraints like UPPER(Nothing) (by CapturedType(*) <:> TypeVariable(E)) in the type inference context
             * due to approximation captured types.
             * Instead this type check we move on self-type level anyway: checking CapturedType(out Enum<*>) against TypeVariable(E).
             * This subtyping can already be successful and not add unwanted constraints in the type inference context.
             */
            if (isTypeVariableAgainstStarProjectionForSelfType)
                continue

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

    private fun TypeSystemContext.isCommonDenotableType(type: KotlinTypeMarker): Boolean =
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

    private fun TypeSystemContext.isStubTypeSubtypeOfAnother(a: SimpleTypeMarker, b: SimpleTypeMarker): Boolean {
        val originalA = a.asDefinitelyNotNullType()?.original() ?: a
        val originalB = b.asDefinitelyNotNullType()?.original() ?: b

        if (originalA.typeConstructor() !== originalB.typeConstructor()) return false
        if (!a.isDefinitelyNotNullType() && b.isDefinitelyNotNullType()) return false
        if (a.isMarkedNullable() && !b.isMarkedNullable()) return false

        return true // A!! == B!!, A? == B? or A == B
    }

    private fun checkSubtypeForSpecialCases(
        state: TypeCheckerState,
        subType: SimpleTypeMarker,
        superType: SimpleTypeMarker
    ): Boolean? = with(state.typeSystemContext) {
        if (subType.isError() || superType.isError()) {
            if (state.isErrorTypeEqualsToAnything) return true

            if (subType.isMarkedNullable() && !superType.isMarkedNullable()) return false

            return AbstractStrictEqualityTypeChecker.strictEqualTypes(
                this,
                subType.withNullability(false),
                superType.withNullability(false)
            )
        }

        if (subType.isStubTypeForBuilderInference() && superType.isStubTypeForBuilderInference())
            return isStubTypeSubtypeOfAnother(subType, superType) || state.isStubTypeEqualsToAnything

        if (subType.isStubType() || superType.isStubType())
            return state.isStubTypeEqualsToAnything

        // superType might be a definitely notNull type (see KT-42824)
        val superOriginalType = superType.asDefinitelyNotNullType()?.original() ?: superType
        val superTypeCaptured = superOriginalType.asCapturedType()
        val lowerType = superTypeCaptured?.lowerType()
        if (superTypeCaptured != null && lowerType != null) {
            // If superType is nullable, e.g., to check if Foo? a subtype of Captured<in Foo>?, we check the LHS, Foo?,
            // against the nullable version of the lower type of RHS. See KT-42825
            val nullableLowerType = if (superType.isMarkedNullable()) {
                lowerType.withNullability(true)
            } else {
                if (superType.isDefinitelyNotNullType()) lowerType.makeDefinitelyNotNullOrNotNull() else lowerType
            }
            when (state.getLowerCapturedTypePolicy(subType, superTypeCaptured)) {
                CHECK_ONLY_LOWER -> return isSubtypeOf(state, subType, nullableLowerType)
                CHECK_SUBTYPE_AND_LOWER -> if (isSubtypeOf(state, subType, nullableLowerType)) return true
                SKIP_LOWER -> Unit
            }
        }

        val superTypeConstructor = superType.typeConstructor()
        if (superTypeConstructor.isIntersection()) {
            assert(!superType.isMarkedNullable()) { "Intersection type should not be marked nullable!: $superType" }

            return superTypeConstructor.supertypes().all { isSubtypeOf(state, subType, it) }
        }

        /*
         * We handle cases like CapturedType(out Bar) <: Foo<CapturedType(out Bar)> separately here.
         * If Foo is a self type i.g. Foo<E: Foo<E>>, then argument for E will certainly be subtype of Foo<same_argument_for_E>,
         * so if CapturedType(out Bar) is the same as a type of Foo's argument and Foo is a self type, then subtyping should return true.
         * If we don't handle this case separately, subtyping may not converge due to the nature of the capturing.
         */
        val subTypeConstructor = subType.typeConstructor()
        if (subType is CapturedTypeMarker
            || (subTypeConstructor.isIntersection() && subTypeConstructor.supertypes().all { it is CapturedTypeMarker })) {
            val typeParameter =
                state.typeSystemContext.getTypeParameterForArgumentInBaseIfItEqualToTarget(baseType = superType, targetType = subType)
            if (typeParameter != null && typeParameter.hasRecursiveBounds(superType.typeConstructor())) {
                return true
            }
        }

        return null
    }

    private fun TypeSystemContext.getTypeParameterForArgumentInBaseIfItEqualToTarget(
        baseType: KotlinTypeMarker,
        targetType: KotlinTypeMarker
    ): TypeParameterMarker? {
        for (i in 0 until baseType.argumentsCount()) {
            val typeArgument = baseType.getArgument(i).takeIf { !it.isStarProjection() }?.getType() ?: continue
            val areBothTypesCaptured =
                typeArgument.lowerBoundIfFlexible().isCapturedType() && targetType.lowerBoundIfFlexible().isCapturedType()

            if (typeArgument == targetType || (areBothTypesCaptured && typeArgument.typeConstructor() == targetType.typeConstructor())) {
                return baseType.typeConstructor().getParameter(i)
            }

            getTypeParameterForArgumentInBaseIfItEqualToTarget(typeArgument, targetType)?.let { return it }
        }

        return null
    }

    private fun collectAllSupertypesWithGivenTypeConstructor(
        state: TypeCheckerState,
        subType: SimpleTypeMarker,
        superConstructor: TypeConstructorMarker
    ): List<SimpleTypeMarker> = with(state.typeSystemContext) {
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

        state.anySupertype(subType, { false }) {

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
                    state.substitutionSupertypePolicy(current)
                }
            }
        }

        return result
    }

    private fun collectAndFilter(
        state: TypeCheckerState,
        classType: SimpleTypeMarker,
        constructor: TypeConstructorMarker
    ) =
        selectOnlyPureKotlinSupertypes(state, collectAllSupertypesWithGivenTypeConstructor(state, classType, constructor))


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
    private fun selectOnlyPureKotlinSupertypes(
        state: TypeCheckerState,
        supertypes: List<SimpleTypeMarker>
    ): List<SimpleTypeMarker> = with(state.typeSystemContext) {
        if (supertypes.size < 2) return supertypes

        val allPureSupertypes = supertypes.filter {
            it.asArgumentList().all(this) { it.getType().asFlexibleType() == null }
        }
        return if (allPureSupertypes.isNotEmpty()) allPureSupertypes else supertypes
    }


    // nullability was checked earlier via nullabilityChecker
    // should be used only if you really sure that it is correct
    fun findCorrespondingSupertypes(
        state: TypeCheckerState,
        subType: SimpleTypeMarker,
        superConstructor: TypeConstructorMarker
    ): List<SimpleTypeMarker> = with(state.typeSystemContext) {
        if (subType.isClassType()) {
            return collectAndFilter(state, subType, superConstructor)
        }

        // i.e. superType is not a classType
        if (!superConstructor.isClassTypeConstructor() && !superConstructor.isIntegerLiteralTypeConstructor()) {
            return collectAllSupertypesWithGivenTypeConstructor(state, subType, superConstructor)
        }

        // todo add tests
        val classTypeSupertypes = SmartList<SimpleTypeMarker>()
        state.anySupertype(subType, { false }) {
            if (it.isClassType()) {
                classTypeSupertypes.add(it)
                SupertypesPolicy.None
            } else {
                SupertypesPolicy.LowerIfFlexible
            }
        }

        return classTypeSupertypes.flatMap { collectAndFilter(state, it, superConstructor) }
    }
}


object AbstractNullabilityChecker {
    // this method checks only nullability
    fun isPossibleSubtype(state: TypeCheckerState, subType: SimpleTypeMarker, superType: SimpleTypeMarker): Boolean =
        runIsPossibleSubtype(state, subType, superType)

    fun isSubtypeOfAny(context: TypeCheckerProviderContext, type: KotlinTypeMarker): Boolean =
        AbstractNullabilityChecker.isSubtypeOfAny(
            context.newTypeCheckerState(
                errorTypesEqualToAnything = false,
                stubTypesEqualToAnything = true
            ),
            type
        )

    fun isSubtypeOfAny(state: TypeCheckerState, type: KotlinTypeMarker): Boolean =
        with(state.typeSystemContext) {
            state.hasNotNullSupertype(type.lowerBoundIfFlexible(), SupertypesPolicy.LowerIfFlexible)
        }

    private fun runIsPossibleSubtype(state: TypeCheckerState, subType: SimpleTypeMarker, superType: SimpleTypeMarker): Boolean =
        with(state.typeSystemContext) {
            if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
                // it makes for case String? & Any <: String
                assert(
                    subType.isSingleClassifierType() || subType.typeConstructor().isIntersection() || state.isAllowedTypeVariable(
                        subType
                    )
                ) {
                    "Not singleClassifierType and not intersection subType: $subType"
                }
                assert(superType.isSingleClassifierType() || state.isAllowedTypeVariable(superType)) {
                    "Not singleClassifierType superType: $superType"
                }
            }

            // superType is actually nullable
            if (superType.isMarkedNullable()) return true

            // i.e. subType is definitely not null
            if (subType.isDefinitelyNotNullType()) return true

            // i.e. subType is captured type, projection of which is marked not-null
            if (subType is CapturedTypeMarker && subType.isProjectionNotNull()) return true

            // i.e. subType is not-nullable
            if (state.hasNotNullSupertype(subType, SupertypesPolicy.LowerIfFlexible)) return true

            // i.e. subType hasn't not-null supertype and isn't definitely not-null, but superType is definitely not-null
            if (superType.isDefinitelyNotNullType()) return false

            // i.e subType hasn't not-null supertype, but superType has
            if (state.hasNotNullSupertype(superType, SupertypesPolicy.UpperIfFlexible)) return false

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

            return hasPathByNotMarkedNullableNodes(state, subType, superType.typeConstructor())
        }

    fun TypeCheckerState.hasNotNullSupertype(type: SimpleTypeMarker, supertypesPolicy: SupertypesPolicy) =
        with(typeSystemContext) {
            anySupertype(type, {
                (it.isClassType() && !it.isMarkedNullable()) || it.isDefinitelyNotNullType()
            }) {
                if (it.isMarkedNullable()) SupertypesPolicy.None else supertypesPolicy
            }
        }

    fun TypeCheckerProviderContext.hasPathByNotMarkedNullableNodes(start: SimpleTypeMarker, end: TypeConstructorMarker) =
        hasPathByNotMarkedNullableNodes(
            newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true), start, end
        )

    fun hasPathByNotMarkedNullableNodes(state: TypeCheckerState, start: SimpleTypeMarker, end: TypeConstructorMarker) =
        with(state.typeSystemContext) {
            state.anySupertype(
                start,
                { isApplicableAsEndNode(state, it, end) },
                { if (it.isMarkedNullable()) SupertypesPolicy.None else SupertypesPolicy.LowerIfFlexible }
            )
        }

    private fun isApplicableAsEndNode(state: TypeCheckerState, type: SimpleTypeMarker, end: TypeConstructorMarker): Boolean =
        with(state.typeSystemContext) {
            if (type.isNothing()) return true
            if (type.isMarkedNullable()) return false

            if (state.isStubTypeEqualsToAnything && type.isStubType()) return true

            return areEqualTypeConstructors(type.typeConstructor(), end)
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
