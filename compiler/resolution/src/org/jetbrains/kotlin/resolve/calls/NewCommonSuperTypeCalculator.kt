/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.calls

import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.AbstractFlexibilityChecker.hasDifferentFlexibilityAtDepth
import org.jetbrains.kotlin.types.AbstractNullabilityChecker.hasPathByNotMarkedNullableNodes
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext
import org.jetbrains.kotlin.types.model.*

object NewCommonSuperTypeCalculator {
    // TODO: Bridge for old calls
    fun commonSuperType(types: List<UnwrappedType>): UnwrappedType {
        return SimpleClassicTypeSystemContext.commonSuperType(types) as UnwrappedType
    }

    fun TypeSystemCommonSuperTypesContext.commonSuperType(types: List<KotlinTypeMarker>): KotlinTypeMarker {
        val maxDepth = types.maxBy { it.typeDepth() }?.typeDepth() ?: 0
        return commonSuperType(types, -maxDepth, true)
    }

    private fun TypeSystemCommonSuperTypesContext.commonSuperType(
        types: List<KotlinTypeMarker>,
        depth: Int,
        isTopLevelType: Boolean = false
    ): KotlinTypeMarker {
        if (types.isEmpty()) throw IllegalStateException("Empty collection for input")

        types.singleOrNull()?.let { return it }

        var thereIsFlexibleTypes = false

        val lowers = types.map {
            when (it) {
                is SimpleTypeMarker -> {
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

        val contextStubTypesEqualToAnything = newBaseTypeCheckerContext(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true)
        val contextStubTypesNotEqual = newBaseTypeCheckerContext(errorTypesEqualToAnything = false, stubTypesEqualToAnything = false)

        val lowerSuperType = commonSuperTypeForSimpleTypes(lowers, depth, contextStubTypesEqualToAnything, contextStubTypesNotEqual)
        if (!thereIsFlexibleTypes) return lowerSuperType

        val upperSuperType = commonSuperTypeForSimpleTypes(
            types.map { it.upperBoundIfFlexible() }, depth, contextStubTypesEqualToAnything, contextStubTypesNotEqual
        )

        if (!isTopLevelType) {
            val nonStubTypes =
                types.filter { !isStubRelatedType(it.lowerBoundIfFlexible()) && !isStubRelatedType(it.upperBoundIfFlexible()) }
            val equalToEachOtherTypes = nonStubTypes.filter { potentialCommonSuperType ->
                nonStubTypes.all {
                    AbstractTypeChecker.equalTypes(this, it, potentialCommonSuperType)
                }
            }

            if (equalToEachOtherTypes.isNotEmpty()) {
                // TODO: merge flexibilities of type arguments instead of select the first suitable type
                return equalToEachOtherTypes.first()
            }
        }

        return createFlexibleType(lowerSuperType, upperSuperType)
    }

    private fun TypeSystemCommonSuperTypesContext.commonSuperTypeForSimpleTypes(
        types: List<SimpleTypeMarker>,
        depth: Int,
        contextStubTypesEqualToAnything: AbstractTypeCheckerContext,
        contextStubTypesNotEqual: AbstractTypeCheckerContext
    ): SimpleTypeMarker {
        // i.e. result type also should be marked nullable
        val notAllNotNull =
            types.any { !isStubRelatedType(it) && !AbstractNullabilityChecker.isSubtypeOfAny(contextStubTypesEqualToAnything, it) }
        val notNullTypes = if (notAllNotNull) types.map { it.withNullability(false) } else types

        val commonSuperType = commonSuperTypeForNotNullTypes(notNullTypes, depth, contextStubTypesEqualToAnything, contextStubTypesNotEqual)
        return if (notAllNotNull)
            refineNullabilityForUndefinedNullability(types, commonSuperType) ?: commonSuperType.withNullability(true)
        else
            commonSuperType
    }

    private fun TypeSystemCommonSuperTypesContext.refineNullabilityForUndefinedNullability(
        types: List<SimpleTypeMarker>,
        commonSuperType: SimpleTypeMarker
    ): SimpleTypeMarker? {
        if (!commonSuperType.canHaveUndefinedNullability()) return null

        val actuallyNotNull =
            types.all { hasPathByNotMarkedNullableNodes(it, commonSuperType.typeConstructor()) }
        return if (actuallyNotNull) commonSuperType else null
    }

    // Makes representative sample, i.e. (A, B, A) -> (A, B)
    private fun TypeSystemCommonSuperTypesContext.uniquify(
        types: List<SimpleTypeMarker>,
        contextStubTypesNotEqual: AbstractTypeCheckerContext
    ): List<SimpleTypeMarker> {
        val uniqueTypes = arrayListOf<SimpleTypeMarker>()
        for (type in types) {
            val isNewUniqueType = uniqueTypes.all {
                val equalsModuloFlexibility = AbstractTypeChecker.equalTypes(contextStubTypesNotEqual, it, type) &&
                        !it.typeConstructor().isIntegerLiteralTypeConstructor()

                !equalsModuloFlexibility || hasDifferentFlexibilityAtDepth(listOf(it, type))
            }
            if (isNewUniqueType) {
                uniqueTypes += type
            }
        }
        return uniqueTypes
    }

    // This function leaves only supertypes, i.e. A0 is a strong supertype for A iff A != A0 && A <: A0
    // Explanation: consider types (A : A0, B : B0, A0, B0), then CST(A, B, A0, B0) == CST(CST(A, A0), CST(B, B0)) == CST(A0, B0)
    private fun TypeSystemCommonSuperTypesContext.filterSupertypes(
        list: List<SimpleTypeMarker>,
        contextStubTypesNotEqual: AbstractTypeCheckerContext
    ): List<SimpleTypeMarker> {
        val supertypes = list.toMutableList()
        val iterator = supertypes.iterator()
        while (iterator.hasNext()) {
            val potentialSubtype = iterator.next()
            val isSubtype = supertypes.any { supertype ->
                supertype !== potentialSubtype &&
                        AbstractTypeChecker.isSubtypeOf(contextStubTypesNotEqual, potentialSubtype, supertype) &&
                        !hasDifferentFlexibilityAtDepth(listOf(potentialSubtype, supertype))
            }

            if (isSubtype) iterator.remove()
        }

        return supertypes
    }

    /*
    * Common Supertype calculator works with proper types and stub types (which is a replacement for non-proper types)
    * Also, there are two invariant related to stub types:
    *  - resulting type should be only proper type
    *  - one of the input types is definitely proper type
    * */
    private fun TypeSystemCommonSuperTypesContext.commonSuperTypeForNotNullTypes(
        types: List<SimpleTypeMarker>,
        depth: Int,
        contextStubTypesEqualToAnything: AbstractTypeCheckerContext,
        contextStubTypesNotEqual: AbstractTypeCheckerContext
    ): SimpleTypeMarker {
        if (types.size == 1) return types.single()

        val nonStubTypes = types.filter { !isStubRelatedType(it) }
        if (nonStubTypes.size == 1) return nonStubTypes.single()

        assert(nonStubTypes.isNotEmpty()) {
            "There should be at least one non-stub type to compute common supertype but there are: $types"
        }

        val uniqueTypes = uniquify(nonStubTypes, contextStubTypesNotEqual)
        if (uniqueTypes.size == 1) return uniqueTypes.single()

        val explicitSupertypes = filterSupertypes(uniqueTypes, contextStubTypesNotEqual)
        if (explicitSupertypes.size == 1) return explicitSupertypes.single()
        findErrorTypeInSupertypesIfItIsNeeded(explicitSupertypes, contextStubTypesEqualToAnything)?.let { return it }

        findCommonIntegerLiteralTypesSuperType(explicitSupertypes)?.let { return it }

        return findSuperTypeConstructorsAndIntersectResult(explicitSupertypes, depth, contextStubTypesEqualToAnything)
    }

    private fun TypeSystemCommonSuperTypesContext.isStubRelatedType(type: SimpleTypeMarker): Boolean {
        return type.isStubType() || isCapturedStubType(type)
    }

    private fun TypeSystemCommonSuperTypesContext.isCapturedStubType(type: SimpleTypeMarker): Boolean {
        val projectedType =
            type.asCapturedType()?.typeConstructor()?.projection()?.takeUnless { it.isStarProjection() }?.getType() ?: return false
        return projectedType.asSimpleType()?.isStubType() == true
    }

    private fun TypeSystemCommonSuperTypesContext.findErrorTypeInSupertypesIfItIsNeeded(
        types: List<SimpleTypeMarker>,
        contextStubTypesEqualToAnything: AbstractTypeCheckerContext
    ): SimpleTypeMarker? {
        if (isErrorTypeAllowed) return null
        for (type in types) {
            collectAllSupertypes(type, contextStubTypesEqualToAnything).firstOrNull { it.isError() }?.let { return it.toErrorType() }
        }
        return null
    }

    private fun TypeSystemCommonSuperTypesContext.findSuperTypeConstructorsAndIntersectResult(
        types: List<SimpleTypeMarker>,
        depth: Int,
        contextStubTypesEqualToAnything: AbstractTypeCheckerContext
    ): SimpleTypeMarker =
        intersectTypes(
            allCommonSuperTypeConstructors(types, contextStubTypesEqualToAnything)
                .map { superTypeWithGivenConstructor(types, it, depth) }
        )

    /**
     * Note that if there is captured type C, then no one else is not subtype of C => lowerType cannot help here
     */
    private fun TypeSystemCommonSuperTypesContext.allCommonSuperTypeConstructors(
        types: List<SimpleTypeMarker>,
        contextStubTypesEqualToAnything: AbstractTypeCheckerContext
    ): List<TypeConstructorMarker> {
        val result = collectAllSupertypes(types.first(), contextStubTypesEqualToAnything)
        for (type in types) {
            if (type === types.first()) continue

            result.retainAll(collectAllSupertypes(type, contextStubTypesEqualToAnything))
        }
        return result.filterNot { target ->
            result.any { other ->
                other != target && other.supertypes().any { it.typeConstructor() == target }
            }
        }
    }

    private fun TypeSystemCommonSuperTypesContext.collectAllSupertypes(
        type: SimpleTypeMarker,
        contextStubTypesEqualToAnything: AbstractTypeCheckerContext
    ) =
        LinkedHashSet<TypeConstructorMarker>().apply {
            contextStubTypesEqualToAnything.anySupertype(
                type,
                { add(it.typeConstructor()); false },
                { AbstractTypeCheckerContext.SupertypesPolicy.LowerIfFlexible }
            )
        }

    private fun TypeSystemCommonSuperTypesContext.superTypeWithGivenConstructor(
        types: List<SimpleTypeMarker>,
        constructor: TypeConstructorMarker,
        depth: Int
    ): SimpleTypeMarker {
        if (constructor.parametersCount() == 0) return createSimpleType(
            constructor,
            emptyList(),
            nullable = false
        )

        val typeCheckerContext = newBaseTypeCheckerContext(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true)

        /**
         * Sometimes one type can have several supertypes with given type constructor, suppose A <: List<Int> and A <: List<Double>.
         * Also suppose that B <: List<String>.
         * Note that common supertype for A and B is CS(List<Int>, List<String>) & CS(List<Double>, List<String>),
         * but it is too complicated and we will return not so accurate type: CS(List<Int>, List<Double>, List<String>)
         */
        val correspondingSuperTypes = types.flatMap {
            with(AbstractTypeChecker) {
                typeCheckerContext.findCorrespondingSupertypes(it, constructor)
            }
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

                when {
                    typeArgument.isStarProjection() -> {
                        thereIsStar = true
                        null
                    }

                    typeArgument.getType().lowerBoundIfFlexible().isStubType() -> null

                    else -> typeArgument
                }
            }

            // This is used for folding recursive types like Inv<Inv<*>> into Inv<*>
            fun collapseRecursiveArgumentIfPossible(argument: TypeArgumentMarker): TypeArgumentMarker {
                if (argument.isStarProjection()) return argument
                val argumentType = argument.getType().asSimpleType()
                val argumentConstructor = argumentType?.typeConstructor()
                return if (argument.getVariance() == TypeVariance.OUT && argumentConstructor == constructor && argumentType.asArgumentList()[index].isStarProjection()) {
                    createStarProjection(parameter)
                } else {
                    argument
                }
            }

            val argument =
                if (thereIsStar || typeProjections.isEmpty()) {
                    createStarProjection(parameter)
                } else {
                    collapseRecursiveArgumentIfPossible(calculateArgument(parameter, typeProjections, depth))
                }

            arguments.add(argument)
        }
        return createSimpleType(constructor, arguments, nullable = false)
    }

    private fun TypeSystemCommonSuperTypesContext.uncaptureFromSubtyping(typeArgument: TypeArgumentMarker): TypeArgumentMarker {
        val capturedType = typeArgument.getType().asSimpleType()?.asCapturedType() ?: return typeArgument
        if (capturedType.captureStatus() != CaptureStatus.FOR_SUBTYPING) return typeArgument

        return capturedType.typeConstructor().projection()
    }

    // no star projections in arguments
    private fun TypeSystemCommonSuperTypesContext.calculateArgument(
        parameter: TypeParameterMarker,
        arguments: List<TypeArgumentMarker>,
        depth: Int
    ): TypeArgumentMarker {
        if (depth > 3) {
            return createStarProjection(parameter)
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
                    return createStarProjection(parameter)
                } else {
                    asOut = true
                }
            } else {
                asOut = !thereIsIn
            }
        }

        // CS(Out<X>, Out<Y>) = Out<CS(X, Y)>
        // CS(In<X>, In<Y>) = In<X & Y>
        if (asOut) {
            val argumentTypes = arguments.map { it.getType() }
            val parameterIsNotInv = parameter.getVariance() != TypeVariance.INV

            if (parameterIsNotInv) {
                return commonSuperType(argumentTypes, depth + 1).asTypeArgument()
            }

            val equalToEachOtherType = arguments.firstOrNull { potentialSuperType ->
                arguments.all { AbstractTypeChecker.equalTypes(this, it.getType(), potentialSuperType.getType()) }
            }

            return if (equalToEachOtherType == null) {
                createTypeArgument(commonSuperType(argumentTypes, depth + 1), TypeVariance.OUT)
            } else {
                createTypeArgument(equalToEachOtherType.getType(), TypeVariance.INV)
            }
        } else {
            val type = intersectTypes(arguments.map { it.getType() })

            return if (parameter.getVariance() != TypeVariance.INV) type.asTypeArgument() else createTypeArgument(
                type,
                TypeVariance.IN
            )
        }
    }
}
