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

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.*
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.canHaveUndefinedNullability

object NewCommonSuperTypeCalculator {

    fun commonSuperType(types: List<UnwrappedType>): UnwrappedType {
        val maxDepth = types.maxBy { it.typeDepth() }?.typeDepth() ?: 0
        return commonSuperType(types, -maxDepth)
    }

    private fun commonSuperType(types: List<UnwrappedType>, depth: Int): UnwrappedType {
        if (types.isEmpty()) throw IllegalStateException("Empty collection for input")

        types.singleOrNull()?.let { return it }

        var thereIsFlexibleTypes = false

        val lowers = types.map {
            when (it) {
                is SimpleType -> it
                is FlexibleType -> {
                    if (it is DynamicType) return it
                    // raw types are allowed here and will be transformed to FlexibleTypes

                    thereIsFlexibleTypes = true
                    it.lowerBound
                }
            }
        }

        val lowerSuperType = commonSuperTypeForSimpleTypes(lowers, depth)
        if (!thereIsFlexibleTypes) return lowerSuperType

        val upperSuperType = commonSuperTypeForSimpleTypes(types.map { it.upperIfFlexible() }, depth)
        return KotlinTypeFactory.flexibleType(lowerSuperType, upperSuperType)
    }

    private fun commonSuperTypeForSimpleTypes(types: List<SimpleType>, depth: Int): SimpleType {
        // i.e. result type also should be marked nullable
        val notAllNotNull = types.any { !NullabilityChecker.isSubtypeOfAny(it) }
        val notNullTypes = if (notAllNotNull) types.map { it.makeNullableAsSpecified(false) } else types

        val commonSuperType = commonSuperTypeForNotNullTypes(notNullTypes, depth)
        return if (notAllNotNull)
            refineNullabilityForUndefinedNullability(types, commonSuperType) ?: commonSuperType.makeNullableAsSpecified(true)
        else
            commonSuperType
    }

    private fun refineNullabilityForUndefinedNullability(types: List<SimpleType>, commonSuperType: SimpleType): SimpleType? {
        if (!commonSuperType.unwrap().canHaveUndefinedNullability()) return null

        val actuallyNotNull = types.all { NullabilityChecker.hasPathByNotMarkedNullableNodes(it, commonSuperType.constructor) }
        return if (actuallyNotNull) commonSuperType else null
    }

    // Makes representative sample, i.e. (A, B, A) -> (A, B)
    private fun List<SimpleType>.uniquify(): List<SimpleType> {
        val uniqueTypes = arrayListOf<SimpleType>()
        for (type in this) {
            val isNewUniqueType = uniqueTypes.all { !NewKotlinTypeChecker.equalTypes(it, type) }
            if (isNewUniqueType) {
                uniqueTypes += type
            }
        }
        return uniqueTypes
    }

    // This function leaves only supertypes, i.e. A0 is a strong supertype for A iff A != A0 && A <: A0
    // Explanation: consider types (A : A0, B : B0, A0, B0), then CST(A, B, A0, B0) == CST(CST(A, A0), CST(B, B0)) == CST(A0, B0)
    private fun List<SimpleType>.filterSupertypes(): List<SimpleType> {
        val supertypes = this.toMutableList()
        val iterator = supertypes.iterator()
        while (iterator.hasNext()) {
            val potentialSubtype = iterator.next()
            val isSubtype = supertypes.any { supertype ->
                supertype !== potentialSubtype && NewKotlinTypeChecker.isSubtypeOf(potentialSubtype, supertype)
            }

            if (isSubtype) iterator.remove()
        }

        return supertypes
    }

    private fun commonSuperTypeForNotNullTypes(types: List<SimpleType>, depth: Int): SimpleType {
        if (types.size == 1) return types.single()

        val uniqueTypes = types.uniquify()
        if (uniqueTypes.size == 1) return uniqueTypes.single()

        val explicitSupertypes = uniqueTypes.filterSupertypes()
        if (explicitSupertypes.size == 1) return explicitSupertypes.single()

        return findSuperTypeConstructorsAndIntersectResult(explicitSupertypes, depth)
    }

    private fun findSuperTypeConstructorsAndIntersectResult(types: List<SimpleType>, depth: Int): SimpleType {
        return intersectTypes(allCommonSuperTypeConstructors(types).map { superTypeWithGivenConstructor(types, it, depth) })
    }

    /**
     * Note that if there is captured type C, then no one else is not subtype of C => lowerType cannot help here
     */
    private fun allCommonSuperTypeConstructors(types: List<SimpleType>): List<TypeConstructor> {
        val result = collectAllSupertypes(types.first())
        for (type in types) {
            if (type === types.first()) continue

            result.retainAll(collectAllSupertypes(type))
        }
        return result.filterNot { target ->
            result.any { other ->
                other != target && other.supertypes.any { it.constructor == target }
            }
        }
    }

    private fun collectAllSupertypes(type: SimpleType) = LinkedHashSet<TypeConstructor>().apply {
        type.anySuperTypeConstructor { add(it); false }
    }

    private fun superTypeWithGivenConstructor(
        types: List<SimpleType>,
        constructor: TypeConstructor,
        depth: Int
    ): SimpleType {
        if (constructor.parameters.isEmpty()) return KotlinTypeFactory.simpleType(
            Annotations.EMPTY,
            constructor,
            emptyList(),
            nullable = false
        )

        val typeCheckerContext = ClassicTypeCheckerContext(false)

        /**
         * Sometimes one type can have several supertypes with given type constructor, suppose A <: List<Int> and A <: List<Double>.
         * Also suppose that B <: List<String>.
         * Note that common supertype for A and B is CS(List<Int>, List<String>) & CS(List<Double>, List<String>),
         * but it is too complicated and we will return not so accurate type: CS(List<Int>, List<Double>, List<String>)
         */
        val correspondingSuperTypes = types.flatMap {
            with(NewKotlinTypeChecker) {
                typeCheckerContext.findCorrespondingSupertypes(it, constructor)
            }
        }

        val arguments = ArrayList<TypeProjection>(constructor.parameters.size)
        for ((index, parameter) in constructor.parameters.withIndex()) {
            var thereIsStar = false
            val typeProjections = correspondingSuperTypes.mapNotNull {
                it.arguments.getOrNull(index)?.let {
                    if (it.isStarProjection) {
                        thereIsStar = true
                        null
                    } else it
                }
            }

            val argument =
                if (thereIsStar || typeProjections.isEmpty()) {
                    StarProjectionImpl(parameter)
                } else {
                    calculateArgument(parameter, typeProjections, depth)
                }

            arguments.add(argument)
        }
        return KotlinTypeFactory.simpleType(Annotations.EMPTY, constructor, arguments, nullable = false)
    }

    // no star projections in arguments
    private fun calculateArgument(parameter: TypeParameterDescriptor, arguments: List<TypeProjection>, depth: Int): TypeProjection {
        if (depth > 3) {
            return StarProjectionImpl(parameter)
        }

        // Inv<A>, Inv<A> = Inv<A>
        if (parameter.variance == Variance.INVARIANT && arguments.all { it.projectionKind == Variance.INVARIANT }) {
            val first = arguments.first()
            if (arguments.all { it.type == first.type }) return first
        }

        val asOut: Boolean
        if (parameter.variance != Variance.INVARIANT) {
            asOut = parameter.variance == Variance.OUT_VARIANCE
        } else {
            val thereIsOut = arguments.any { it.projectionKind == Variance.OUT_VARIANCE }
            val thereIsIn = arguments.any { it.projectionKind == Variance.IN_VARIANCE }
            if (thereIsOut) {
                if (thereIsIn) {
                    // CS(Inv<out X>, Inv<in Y>) = Inv<*>
                    return StarProjectionImpl(parameter)
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
            val type = commonSuperType(arguments.map { it.type.unwrap() }, depth + 1)
            return if (parameter.variance != Variance.INVARIANT) return type.asTypeProjection() else TypeProjectionImpl(
                Variance.OUT_VARIANCE,
                type
            )
        } else {
            val type = intersectTypes(arguments.map { it.type.unwrap() })
            return if (parameter.variance != Variance.INVARIANT) return type.asTypeProjection() else TypeProjectionImpl(
                Variance.IN_VARIANCE,
                type
            )
        }
    }
}
