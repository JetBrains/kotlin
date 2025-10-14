/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls

import org.jetbrains.kotlin.types.AbstractFlexibilityChecker
import org.jetbrains.kotlin.types.AbstractNullabilityChecker
import org.jetbrains.kotlin.types.AbstractNullabilityChecker.hasPathByNotMarkedNullableNodes
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.model.*

object NewCommonSuperTypeCalculator {
    context(c: TypeSystemCommonSuperTypesContext)
    fun commonSuperType(types: List<KotlinTypeMarker>): KotlinTypeMarker {
        // Skip computation if the list has only one element.
        // It's not only an optimization, but it's required to not mess up attributes.
        // See compiler/testData/diagnostics/foreignAnnotationsTests/tests/jsr305/nullabilityWarnings/kt65193.kt
        // When the type is flexible, calling replaceCustomAttributes(unionTypeAttributes(types)) will take the attributes of the
        // lower bound which will mess up `EnhancedTypeForWarningAttribute`s.
        // It's not a problem if the list has multiple entries, because `EnhancedTypeForWarningAttribute.union` returns null,
        // meaning that whenever we union multiple types, we remove any `EnhancedTypeForWarningAttribute`.
        types.singleOrNull()?.let { return it }

        val maxDepth = types.maxOfOrNull { it.typeDepth() } ?: 0
        return commonSuperType(types, -maxDepth, true).replaceCustomAttributes(c.unionTypeAttributes(types))
    }

    context(c: TypeSystemCommonSuperTypesContext)
    private fun commonSuperType(
        types: List<KotlinTypeMarker>,
        depth: Int,
        isTopLevelType: Boolean = false,
    ): KotlinTypeMarker {
        if (types.isEmpty()) throw IllegalStateException("Empty collection for input")

        types.singleOrNull()?.let { return it }

        var thereIsFlexibleTypes = false

        val lowers = types.map {
            when (it) {
                is RigidTypeMarker -> {
                    if (it.isCapturedDynamic()) return it

                    it
                }
                is FlexibleTypeMarker -> {
                    if (it.isDynamic()) return it
                    // raw types are allowed here and will be transformed to FlexibleTypes

                    thereIsFlexibleTypes = true
                    it.lowerBound()
                }
                else -> error("sealed")
            }
        }

        val stateStubTypesEqualToAnything = c.newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true)
        val stateStubTypesNotEqual = c.newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = false)

        val lowerSuperType = commonSuperTypeForSimpleTypes(lowers, depth, stateStubTypesEqualToAnything, stateStubTypesNotEqual)
        if (!thereIsFlexibleTypes) return lowerSuperType

        val upperSuperType = commonSuperTypeForSimpleTypes(
            types.map { it.upperBoundIfFlexible() }, depth, stateStubTypesEqualToAnything, stateStubTypesNotEqual
        )

        if (!isTopLevelType) {
            val nonStubTypes =
                types.filter { !it.lowerBoundIfFlexible().isTypeVariable() && !it.upperBoundIfFlexible().isTypeVariable() }
            val equalToEachOtherTypes = nonStubTypes.filter { potentialCommonSuperType ->
                nonStubTypes.all {
                    AbstractTypeChecker.equalTypes(c, it, potentialCommonSuperType)
                }
            }

            if (equalToEachOtherTypes.isNotEmpty()) {
                // TODO: merge flexibilities of type arguments instead of select the first suitable type
                return equalToEachOtherTypes.first()
            }
        }

        return c.createFlexibleType(lowerSuperType, upperSuperType)
    }

    context(c: TypeSystemCommonSuperTypesContext)
    private fun commonSuperTypeForSimpleTypes(
        types: List<RigidTypeMarker>,
        depth: Int,
        stateStubTypesEqualToAnything: TypeCheckerState,
        stateStubTypesNotEqual: TypeCheckerState,
    ): RigidTypeMarker {
        if (types.any { it.isError() }) {
            return c.createErrorType("CST(${types.joinToString()}", delegatedType = null)
        }

        // i.e. result type also should be marked nullable
        val allNotNull = types.all {
            it.isTypeVariable() || it.isNotNullStubTypeForBuilderInference() || AbstractNullabilityChecker.isSubtypeOfAny(
                stateStubTypesEqualToAnything,
                it
            )
        }
        val notNullTypes = if (!allNotNull) types.map { it.withNullability(false) } else types

        val commonSuperType =
            commonSuperTypeForNotNullTypes(notNullTypes, depth, stateStubTypesEqualToAnything, stateStubTypesNotEqual)
        return if (!allNotNull)
            refineNullabilityForUndefinedNullability(types, commonSuperType) ?: commonSuperType.withNullability(true)
        else
            commonSuperType
    }

    context(c: TypeSystemCommonSuperTypesContext)
    private fun refineNullabilityForUndefinedNullability(
        types: List<RigidTypeMarker>,
        commonSuperType: RigidTypeMarker,
    ): RigidTypeMarker? {
        if (!commonSuperType.canHaveUndefinedNullability()) return null

        val actuallyNotNull =
            types.all { c.hasPathByNotMarkedNullableNodes(it, commonSuperType.typeConstructor()) }
        return if (actuallyNotNull) commonSuperType else null
    }

    // Makes representative sample, i.e. (A, B, A) -> (A, B)
    context(c: TypeSystemCommonSuperTypesContext)
    private fun uniquify(
        types: List<RigidTypeMarker>,
        stateStubTypesNotEqual: TypeCheckerState,
    ): List<RigidTypeMarker> {
        val uniqueTypes = arrayListOf<RigidTypeMarker>()
        for (type in types) {
            val isNewUniqueType = uniqueTypes.all {
                val equalsModuloFlexibility = AbstractTypeChecker.equalTypes(stateStubTypesNotEqual, it, type) &&
                        !it.typeConstructor().isIntegerLiteralTypeConstructor()

                !equalsModuloFlexibility || AbstractFlexibilityChecker.hasDifferentFlexibilityAtDepth(listOf(it, type))
            }
            if (isNewUniqueType) {
                uniqueTypes += type
            }
        }
        return uniqueTypes
    }

    // This function leaves only supertypes, i.e. A0 is a strong supertype for A iff A != A0 && A <: A0
    // Explanation: consider types (A : A0, B : B0, A0, B0), then CST(A, B, A0, B0) == CST(CST(A, A0), CST(B, B0)) == CST(A0, B0)
    context(c: TypeSystemCommonSuperTypesContext)
    private fun filterSupertypes(
        list: List<RigidTypeMarker>,
        stateStubTypesNotEqual: TypeCheckerState,
    ): List<RigidTypeMarker> {
        val supertypes = list.toMutableList()
        val iterator = supertypes.iterator()
        while (iterator.hasNext()) {
            val potentialSubtype = iterator.next()
            val isSubtype = supertypes.any { supertype ->
                supertype !== potentialSubtype &&
                        AbstractTypeChecker.isSubtypeOf(stateStubTypesNotEqual, potentialSubtype, supertype) &&
                        !AbstractFlexibilityChecker.hasDifferentFlexibilityAtDepth(listOf(potentialSubtype, supertype))
            }

            if (isSubtype) iterator.remove()
        }

        return supertypes
    }

    context(c: TypeSystemCommonSuperTypesContext)
    private fun commonSuperTypeForBuilderInferenceStubTypes(
        stubTypes: List<RigidTypeMarker>,
        stateStubTypesNotEqual: TypeCheckerState,
    ): RigidTypeMarker {
        require(stubTypes.isNotEmpty()) { "There should be stub types to compute common super type on them" }

        var areAllDefNotNull = true
        var areThereAnyNullable = false
        val typesToUniquify = buildList {
            for (stubType in stubTypes) {
                when {
                    stubType is DefinitelyNotNullTypeMarker -> add(stubType.original())
                    stubType.isMarkedNullable() -> {
                        areThereAnyNullable = true
                        areAllDefNotNull = false
                        add(stubType.withNullability(false))
                    }
                    else -> {
                        areAllDefNotNull = false
                        add(stubType)
                    }
                }
            }
        }

        return uniquify(typesToUniquify, stateStubTypesNotEqual).singleOrNull()?.let {
            when {
                areAllDefNotNull -> it.makeDefinitelyNotNullOrNotNull()
                areThereAnyNullable -> it.withNullability(true)
                else -> it
            }
        } ?: c.nullableAnyType()
    }

    /*
    * Common Supertype calculator works with proper types and stub types (which is a replacement for non-proper types)
    * Also, there are two invariant related to stub types:
    *  - resulting type should be only proper type
    *  - one of the input types is definitely proper type
    * */
    context(c: TypeSystemCommonSuperTypesContext)
    private fun commonSuperTypeForNotNullTypes(
        types: List<RigidTypeMarker>,
        depth: Int,
        stateStubTypesEqualToAnything: TypeCheckerState,
        stateStubTypesNotEqual: TypeCheckerState,
    ): RigidTypeMarker {
        if (types.size == 1) return types.single()

        val nonTypeVariables = types.filter { !it.isStubTypeForVariableInSubtypingOrCaptured() }

        assert(nonTypeVariables.isNotEmpty()) {
            "There should be at least one non-stub type to compute common supertype but there are: $types"
        }

        val (builderInferenceStubTypes, nonStubTypes) = nonTypeVariables.partition { it.isStubTypeForBuilderInference() }
        val areAllNonStubTypesNothing =
            nonStubTypes.isNotEmpty() && nonStubTypes.all { it.isNothing() }

        if (builderInferenceStubTypes.isNotEmpty() && (nonStubTypes.isEmpty() || areAllNonStubTypesNothing)) {
            return commonSuperTypeForBuilderInferenceStubTypes(builderInferenceStubTypes, stateStubTypesNotEqual)
        }

        val uniqueTypes = uniquify(nonStubTypes, stateStubTypesNotEqual)
        if (uniqueTypes.size == 1) return uniqueTypes.single()

        val explicitSupertypes = filterSupertypes(uniqueTypes, stateStubTypesNotEqual)
        if (explicitSupertypes.size == 1) return explicitSupertypes.single()
        findErrorTypeInSupertypes(explicitSupertypes, stateStubTypesEqualToAnything)?.let { return it }

        c.findCommonIntegerLiteralTypesSuperType(explicitSupertypes)?.let { return it }

        return findSuperTypeConstructorsAndIntersectResult(explicitSupertypes, depth, stateStubTypesEqualToAnything)
    }

    context(c: TypeSystemCommonSuperTypesContext)
    private fun RigidTypeMarker.isTypeVariable(): Boolean {
        return isStubTypeForVariableInSubtyping() || this.isCapturedTypeVariable()
    }

    context(c: TypeSystemCommonSuperTypesContext)
    private fun RigidTypeMarker.isNotNullStubTypeForBuilderInference(): Boolean {
        return isStubTypeForBuilderInference() && !isMarkedNullable()
    }

    context(c: TypeSystemCommonSuperTypesContext)
    private fun RigidTypeMarker.isCapturedTypeVariable(): Boolean {
        val projectedType =
            asCapturedTypeUnwrappingDnn()?.typeConstructor()?.projection()?.takeUnless {
                it.isStarProjection()
            }?.getType() ?: return false
        return projectedType.asRigidType()?.isStubTypeForVariableInSubtyping() == true
    }

    context(c: TypeSystemCommonSuperTypesContext)
    private fun findErrorTypeInSupertypes(
        types: List<RigidTypeMarker>,
        stateStubTypesEqualToAnything: TypeCheckerState,
    ): SimpleTypeMarker? {
        for (type in types) {
            collectAllSupertypes(type, stateStubTypesEqualToAnything).firstOrNull { it.isError() }?.let { return it.toErrorType() }
        }
        return null
    }

    context(c: TypeSystemCommonSuperTypesContext)
    private fun findSuperTypeConstructorsAndIntersectResult(
        types: List<RigidTypeMarker>,
        depth: Int,
        stateStubTypesEqualToAnything: TypeCheckerState,
    ): RigidTypeMarker =
        c.intersectTypes(
            allCommonSuperTypeConstructors(types, stateStubTypesEqualToAnything)
                .map { superTypeWithGivenConstructor(types, it, depth) }
        )

    /**
     * Note that if there is captured type C, then no one else is not subtype of C => lowerType cannot help here
     */
    context(c: TypeSystemCommonSuperTypesContext)
    private fun allCommonSuperTypeConstructors(
        types: List<RigidTypeMarker>,
        stateStubTypesEqualToAnything: TypeCheckerState,
    ): List<TypeConstructorMarker> {
        val result = collectAllSupertypes(types.first(), stateStubTypesEqualToAnything)
        // retain all super constructors of the first type that are present in the supertypes of all other types
        for (type in types) {
            if (type === types.first()) continue

            result.retainAll(collectAllSupertypes(type, stateStubTypesEqualToAnything))
        }
        // remove all constructors that have subtype(s) with constructors from the resulting set - they are less precise
        return result.filterNot { target ->
            result.any { other ->
                other != target && other.supertypes().any { it.typeConstructor() == target }
            }
        }
    }

    context(c: TypeSystemCommonSuperTypesContext)
    private fun collectAllSupertypes(
        type: RigidTypeMarker,
        stateStubTypesEqualToAnything: TypeCheckerState,
    ) =
        LinkedHashSet<TypeConstructorMarker>().apply {
            stateStubTypesEqualToAnything.anySupertype(
                type,
                { add(it.typeConstructor()); false },
                { TypeCheckerState.SupertypesPolicy.LowerIfFlexible }
            )
        }

    context(c: TypeSystemCommonSuperTypesContext)
    private fun superTypeWithGivenConstructor(
        types: List<RigidTypeMarker>,
        constructor: TypeConstructorMarker,
        depth: Int,
    ): SimpleTypeMarker {
        if (constructor.parametersCount() == 0) return c.createSimpleType(
            constructor,
            emptyList(),
            nullable = false
        )

        val typeCheckerContext = c.newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true)

        /**
         * Sometimes one type can have several supertypes with given type constructor, suppose A <: List<Int> and A <: List<Double>.
         * Also suppose that B <: List<String>.
         * Note that common supertype for A and B is CS(List<Int>, List<String>) & CS(List<Double>, List<String>),
         * but it is too complicated and we will return not so accurate type: CS(List<Int>, List<Double>, List<String>)
         */
        val correspondingSuperTypes = types.flatMap {
            AbstractTypeChecker.findCorrespondingSupertypes(typeCheckerContext, it, constructor)
        }

        val arguments = ArrayList<TypeArgumentMarker>(constructor.parametersCount())
        for (index in 0 until constructor.parametersCount()) {
            val parameter = constructor.getParameter(index)
            var thereIsStar = false
            val typeProjections = correspondingSuperTypes.mapNotNull {
                val typeArgumentFromSupertype = it.getArgumentOrNull(index) ?: return@mapNotNull null

                // We have to uncapture types with status FOR_SUBTYPING because such captured types are creating during
                // `findCorrespondingSupertypes` call. Normally, we shouldn't create intermediate captured types here, it's needed only
                // to check subtyping. It'll be fixed but for a while we do this uncapturing here
                val typeArgument = uncaptureFromSubtyping(typeArgumentFromSupertype)

                val type = typeArgument.getType()
                when {
                    type == null -> {
                        thereIsStar = true
                        null
                    }

                    type.lowerBoundIfFlexible().isStubTypeForVariableInSubtyping() -> null

                    else -> typeArgument
                }
            }

            // This is used for folding recursive types like Inv<Inv<*>> into Inv<*>
            fun collapseRecursiveArgumentIfPossible(argument: TypeArgumentMarker): TypeArgumentMarker {
                val argumentType = argument.getType()?.asRigidType() ?: return argument
                val argumentConstructor = argumentType.typeConstructor()
                return if (argument.getVariance() == TypeVariance.OUT && argumentConstructor == constructor && argumentType.asArgumentList()[index].isStarProjection()) {
                    c.createStarProjection(parameter)
                } else {
                    argument
                }
            }

            val argument =
                if (thereIsStar || typeProjections.isEmpty() || checkRecursion(types, typeProjections, parameter)) {
                    c.createStarProjection(parameter)
                } else {
                    collapseRecursiveArgumentIfPossible(calculateArgument(parameter, typeProjections, depth))
                }

            arguments.add(argument)
        }
        return c.createSimpleType(constructor, arguments, nullable = false, isExtensionFunction = types.all { it.isExtensionFunction() })
    }

    context(c: TypeSystemCommonSuperTypesContext)
    private fun uncaptureFromSubtyping(typeArgument: TypeArgumentMarker): TypeArgumentMarker {
        val capturedType = typeArgument.getType()?.asRigidType()?.asCapturedTypeUnwrappingDnn() ?: return typeArgument
        if (capturedType.captureStatus() != CaptureStatus.FOR_SUBTYPING) return typeArgument

        return capturedType.typeConstructor().projection()
    }

    /**
     * This function returns true in case of detected recursion in type arguments.
     *
     * For situations with self type arguments (or similar ones), the call of this function
     * prevents too deep type argument analysis during super type calculation.
     * Typical examples use something like this interface in hierarchy:
     * ```
     * interface Some<T : Some<T>>
     * ```
     * From point of view of this function we have here something like `Some<CapturedType>` in [originalTypesForCst],
     * and the captured type in argument has the same `Some<CapturedType>` as its constructor supertype.
     *
     * See also the test 'multirecursion.kt' and comment to the fix of [KT-38544](https://youtrack.jetbrains.com/issue/KT-38544):
     * for single super type constructor create star projection argument when types for that argument are equal to the original types.
     * Captured star projections are replaced with their corresponding supertypes during this check.
     * The check is skipped for contravariant parameters, for which recursive cst calculation never happens.
     */
    context(c: TypeSystemCommonSuperTypesContext)
    private fun checkRecursion(
        originalTypesForCst: List<RigidTypeMarker>,
        typeArgumentsForSuperConstructorParameter: List<TypeArgumentMarker>,
        parameter: TypeParameterMarker,
    ): Boolean {
        if (parameter.getVariance() == TypeVariance.IN)
            return false // arguments for contravariant parameters are intersected, recursion should not be possible

        val originalTypesSet = originalTypesForCst.mapTo(mutableSetOf()) { it.originalIfDefinitelyNotNullable() }
        val typeArgumentsTypeSet = typeArgumentsForSuperConstructorParameter.mapTo(mutableSetOf()) {
            // star projections shouldn't happen because we checked in superTypeWithGivenConstructor.
            it.getType()!!.lowerBoundIfFlexible().originalIfDefinitelyNotNullable()
        }

        if (originalTypesSet.size != typeArgumentsTypeSet.size)
            return false

        // only needed in case of captured star projections in argument types
        val originalTypeConstructorSet by lazy { typeConstructorsWithExpandedStarProjections(originalTypesSet).toSet() }

        for (argumentType in typeArgumentsTypeSet) {
            if (argumentType in originalTypesSet) continue

            var starProjectionFound = false
            for (supertype in supertypesIfCapturedStarProjection(argumentType).orEmpty()) {
                if (supertype.lowerBoundIfFlexible().typeConstructor() !in originalTypeConstructorSet)
                    return false
                else starProjectionFound = true
            }

            if (!starProjectionFound)
                return false
        }
        return true
    }

    context(c: TypeSystemCommonSuperTypesContext)
    private fun typeConstructorsWithExpandedStarProjections(types: Set<SimpleTypeMarker>) =
        sequence {
            for (type in types) {
                if (isCapturedStarProjection(type)) {
                    for (supertype in supertypesIfCapturedStarProjection(type).orEmpty()) {
                        yield(supertype.lowerBoundIfFlexible().typeConstructor())
                    }
                } else {
                    yield(type.typeConstructor())
                }
            }
        }

    context(c: TypeSystemCommonSuperTypesContext)
    private fun isCapturedStarProjection(type: SimpleTypeMarker): Boolean =
        type.asCapturedType()?.typeConstructor()?.projection()?.isStarProjection() == true

    context(c: TypeSystemCommonSuperTypesContext)
    private fun supertypesIfCapturedStarProjection(type: SimpleTypeMarker): Collection<KotlinTypeMarker>? {
        val constructor = type.asCapturedType()?.typeConstructor() ?: return null
        return if (constructor.projection().isStarProjection())
            constructor.supertypes()
        else null
    }

    // no star projections in arguments
    context(c: TypeSystemCommonSuperTypesContext)
    private fun calculateArgument(
        parameter: TypeParameterMarker,
        arguments: List<TypeArgumentMarker>,
        depth: Int,
    ): TypeArgumentMarker {
        if (depth > 0) {
            return c.createStarProjection(parameter)
        }

        // Inv<A>, Inv<A> = Inv<A>
        if (parameter.getVariance() == TypeVariance.INV && arguments.all { it.getVariance() == TypeVariance.INV }) {
            val first = arguments.first()
            if (arguments.all { it.getType() == first.getType() }) return first
        }

        val asOut: Boolean
        if (parameter.getVariance() != TypeVariance.INV) {
            asOut = parameter.getVariance() == TypeVariance.OUT
        } else {
            val thereIsOut = arguments.any { it.getVariance() == TypeVariance.OUT }
            val thereIsIn = arguments.any { it.getVariance() == TypeVariance.IN }
            if (thereIsOut) {
                if (thereIsIn) {
                    // CS(Inv<out X>, Inv<in Y>) = Inv<*>
                    return c.createStarProjection(parameter)
                } else {
                    asOut = true
                }
            } else {
                asOut = !thereIsIn
            }
        }

        // CS(Out<X>, Out<Y>) = Out<CS(X, Y)>
        // CS(In<X>, In<Y>) = In<X & Y>
        // CS(Inv<X>, Inv<Y>) =
        //                     Inv<X>            if X == Y with `stubTypesEqualToAnything = false`
        //                     Inv<CS(X, Y)>     if CS(X, Y) == X == Y with stubTypesEqualToAnything = true and ImprovedVarianceInCst is enabled
        //                     Inv<out CS(X, Y)> otherwise
        if (asOut) {
            val argumentTypes = arguments.map { it.getType()!! }
            val parameterIsNotInv = parameter.getVariance() != TypeVariance.INV

            if (parameterIsNotInv) {
                return commonSuperType(argumentTypes, depth + 1).asTypeArgument()
            }

            val equalToEachOtherType = arguments.firstOrNull { potentialSuperType ->
                arguments.all {
                    AbstractTypeChecker.equalTypes(c, it.getType()!!, potentialSuperType.getType()!!, stubTypesEqualToAnything = false)
                }
            }

            return if (equalToEachOtherType == null) {
                val cst = commonSuperType(argumentTypes, depth + 1)

                // If the CST is equal to all arguments with stubTypesEqualToAnything = true, we use it with INV variance.
                // This is only supported in K2,
                // where the only stub types we can encounter here are 'stub types for type variables in subtyping'
                // from ResultTypeResolver.
                val variance = if (
                    c.supportsImprovedVarianceInCst() &&
                    argumentTypes.all { AbstractTypeChecker.equalTypes(c, it, cst, stubTypesEqualToAnything = true) }
                ) {
                    TypeVariance.INV
                } else {
                    TypeVariance.OUT
                }
                c.createTypeArgument(cst, variance)
            } else {
                val thereIsNotInv = arguments.any { it.getVariance() != TypeVariance.INV }
                c.createTypeArgument(equalToEachOtherType.getType()!!, if (thereIsNotInv) TypeVariance.OUT else TypeVariance.INV)
            }
        } else {
            val type = c.intersectTypes(arguments.map { it.getType()!! })

            return if (parameter.getVariance() != TypeVariance.INV) type.asTypeArgument() else c.createTypeArgument(
                type,
                TypeVariance.IN
            )
        }
    }
}
