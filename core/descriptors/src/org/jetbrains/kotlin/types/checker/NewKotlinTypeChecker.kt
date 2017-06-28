/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types.checker

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.CapturedType
import org.jetbrains.kotlin.resolve.calls.inference.CapturedTypeConstructor
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.TypeCheckerContext.SupertypesPolicy
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.SmartList

object StrictEqualityTypeChecker {
    /**
     * String! != String & A<String!> != A<String>, also A<in Nothing> != A<out Any?>
     * also A<*> != A<out Any?>
     * different error types non-equals even errorTypeEqualToAnything
     */
    fun strictEqualTypes(a: UnwrappedType, b: UnwrappedType): Boolean {
        if (a === b) return true

        if (a is SimpleType && b is SimpleType) return strictEqualTypes(a, b)
        if (a is FlexibleType && b is FlexibleType) {
            return strictEqualTypes(a.lowerBound, b.lowerBound) &&
                   strictEqualTypes(a.upperBound, b.upperBound)
        }
        return false
    }

    fun strictEqualTypes(a: SimpleType, b: SimpleType): Boolean {
        if (a.isMarkedNullable != b.isMarkedNullable
            || a.constructor != b.constructor
            || a.arguments.size != b.arguments.size
        ) {
            return false
        }
        for (i in a.arguments.indices) {
            val aArg = a.arguments[i]
            val bArg = b.arguments[i]
            if (aArg.isStarProjection != bArg.isStarProjection) return false

            // both non-star
            if (!aArg.isStarProjection) {
                if (aArg.projectionKind != bArg.projectionKind) return false
                if (!strictEqualTypes(aArg.type.unwrap(), bArg.type.unwrap())) return false
            }
        }
        return true
    }

}

object ErrorTypesAreEqualToAnything : KotlinTypeChecker {
    override fun isSubtypeOf(subtype: KotlinType, supertype: KotlinType): Boolean =
            NewKotlinTypeChecker.run { TypeCheckerContext(true).isSubtypeOf(subtype.unwrap(), supertype.unwrap()) }

    override fun equalTypes(a: KotlinType, b: KotlinType): Boolean =
            NewKotlinTypeChecker.run { TypeCheckerContext(true).equalTypes(a.unwrap(), b.unwrap()) }
}

object NewKotlinTypeChecker : KotlinTypeChecker {
    override fun isSubtypeOf(subtype: KotlinType, supertype: KotlinType): Boolean =
            TypeCheckerContext(true).isSubtypeOf(subtype.unwrap(), supertype.unwrap()) // todo fix flag errorTypeEqualsToAnything

    override fun equalTypes(a: KotlinType, b: KotlinType): Boolean =
            TypeCheckerContext(false).equalTypes(a.unwrap(), b.unwrap())

    fun TypeCheckerContext.equalTypes(a: UnwrappedType, b: UnwrappedType): Boolean {
        if (a === b) return true

        return isSubtypeOf(a, b) && isSubtypeOf(b, a)
    }

    fun TypeCheckerContext.isSubtypeOf(subType: UnwrappedType, superType: UnwrappedType): Boolean {
        val newSubType = transformToNewType(subType)
        val newSuperType = transformToNewType(superType)

        checkSubtypeForSpecialCases(newSubType.lowerIfFlexible(), newSuperType.upperIfFlexible())?.let { return it }

        // we should add constraints with flexible types, otherwise we never get flexible type as answer in constraint system
        addSubtypeConstraint(newSubType, newSuperType)?.let { return it }

        return isSubtypeOfForSingleClassifierType(newSubType.lowerIfFlexible(), newSuperType.upperIfFlexible())
    }

    fun transformToNewType(type: SimpleType): SimpleType {
        val constructor = type.constructor
        when (constructor) {
            // Type itself can be just SimpleTypeImpl, not CapturedType. see KT-16147
            is CapturedTypeConstructor -> {
                val lowerType = constructor.typeProjection.takeIf { it.projectionKind == Variance.IN_VARIANCE }?.type?.unwrap()

                // it is incorrect calculate this type directly because of recursive star projections
                if (constructor.newTypeConstructor == null) {
                    constructor.newTypeConstructor = NewCapturedTypeConstructor(constructor.typeProjection, constructor.supertypes.map { it.unwrap() })
                }
                val newCapturedType = NewCapturedType(CaptureStatus.FOR_SUBTYPING, constructor.newTypeConstructor!!,
                                                      lowerType, type.annotations, type.isMarkedNullable)
                return newCapturedType
            }

            is IntegerValueTypeConstructor -> {
                val newConstructor = IntersectionTypeConstructor(constructor.supertypes.map { TypeUtils.makeNullableAsSpecified(it, type.isMarkedNullable) })
                return KotlinTypeFactory.simpleType(type.annotations, newConstructor, listOf(), false, type.memberScope)
            }

            is IntersectionTypeConstructor -> if (type.isMarkedNullable) {
                val newSuperTypes = constructor.supertypes.map { it.makeNullable() }
                val newConstructor = IntersectionTypeConstructor(newSuperTypes)
                return KotlinTypeFactory.simpleType(type.annotations, newConstructor, listOf(), false, newConstructor.createScopeForKotlinType())
            }
        }

        return type
    }

    fun transformToNewType(type: UnwrappedType): UnwrappedType =
            when (type) {
                is SimpleType -> transformToNewType(type)
                is FlexibleType -> {
                    val newLower = transformToNewType(type.lowerBound)
                    val newUpper = transformToNewType(type.upperBound)
                    if (newLower !== type.lowerBound || newUpper !== type.upperBound) {
                        KotlinTypeFactory.flexibleType(newLower, newUpper)
                    }
                    else {
                        type
                    }
                }
            }

    private fun TypeCheckerContext.checkSubtypeForSpecialCases(subType: SimpleType, superType: SimpleType): Boolean? {
        if (subType.isError || superType.isError) {
            if (errorTypeEqualsToAnything) return true

            if (subType.isMarkedNullable && !superType.isMarkedNullable) return false

            return StrictEqualityTypeChecker.strictEqualTypes(subType.makeNullableAsSpecified(false), superType.makeNullableAsSpecified(false))
        }

        if (superType is NewCapturedType && superType.lowerType != null && isSubtypeOf(subType, superType.lowerType)) return true

        (superType.constructor as? IntersectionTypeConstructor)?.let {
            assert(!superType.isMarkedNullable) { "Intersection type should not be marked nullable!: $superType" }
            return it.supertypes.all { isSubtypeOf(subType, it.unwrap()) }
        }

        return null
    }

    private fun TypeCheckerContext.hasNothingSupertype(type: SimpleType) = // todo add tests
        anySupertype(type, KotlinBuiltIns::isNothingOrNullableNothing) {
            if (it.isClassType) {
                SupertypesPolicy.None
            }
            else {
                SupertypesPolicy.LowerIfFlexible
            }
        }

    private fun TypeCheckerContext.isSubtypeOfForSingleClassifierType(subType: SimpleType, superType: SimpleType): Boolean {
        assert(subType.isSingleClassifierType || subType.isIntersectionType || subType.isAllowedTypeVariable) {
            "Not singleClassifierType and not intersection subType: $subType"
        }
        assert(superType.isSingleClassifierType || superType.isAllowedTypeVariable) {
            "Not singleClassifierType superType: $superType"
        }

        if (!NullabilityChecker.isPossibleSubtype(this, subType, superType)) return false

        val superConstructor = superType.constructor
        val supertypesWithSameConstructor = findCorrespondingSupertypes(subType, superConstructor)
        when (supertypesWithSameConstructor.size) {
            0 -> return hasNothingSupertype(subType) // todo Nothing & Array<Number> <: Array<String>
            1 -> return isSubtypeForSameConstructor(supertypesWithSameConstructor.first().arguments, superType)

            else -> { // at least 2 supertypes with same constructors. Such case is rare
                if (supertypesWithSameConstructor.any { isSubtypeForSameConstructor(it.arguments, superType) }) return true

                val newArguments = superConstructor.parameters.mapIndexed { index, _ ->
                    val allProjections = supertypesWithSameConstructor.map {
                        it.arguments.getOrNull(index)?.takeIf { it.projectionKind == Variance.INVARIANT }?.type?.unwrap()
                        ?: error("Incorrect type: $it, subType: $subType, superType: $superType")
                    }

                    // todo discuss
                    intersectTypes(allProjections).asTypeProjection()
                }

                return isSubtypeForSameConstructor(newArguments, superType)
            }
        }
    }

    // nullability was checked earlier via nullabilityChecker
    private fun TypeCheckerContext.findCorrespondingSupertypes(
            baseType: SimpleType,
            constructor: TypeConstructor
    ): List<SimpleType> {
        fun TypeCheckerContext.collectAndFilter(classType: SimpleType, constructor: TypeConstructor) =
                selectOnlyPureKotlinSupertypes(collectAllSupertypesWithGivenTypeConstructor(classType, constructor))

        if (baseType.isClassType) {
            return collectAndFilter(baseType, constructor)
        }

        // i.e. superType is not a classType
        if (constructor !is ClassDescriptor) {
            return collectAllSupertypesWithGivenTypeConstructor(baseType, constructor)
        }

        // todo add tests
        val classTypeSupertypes = SmartList<SimpleType>()
        anySupertype(baseType, { false }) {
            if (it.isClassType) {
                classTypeSupertypes.add(it)
                SupertypesPolicy.None
            }
            else {
                SupertypesPolicy.LowerIfFlexible
            }
        }

        return classTypeSupertypes.flatMap { collectAndFilter(it, constructor) }
    }

    private fun TypeCheckerContext.collectAllSupertypesWithGivenTypeConstructor(
            baseType: SimpleType,
            constructor: TypeConstructor
    ): List<SimpleType> {

        var result: MutableList<SimpleType>? = null

        anySupertype(baseType, { false }) {
            val current = captureFromArguments(it, CaptureStatus.FOR_SUBTYPING)

            when {
                areEqualTypeConstructors(current.constructor, constructor) -> {
                    if (result == null) {
                        result = SmartList()
                    }
                    result!!.add(current)

                    SupertypesPolicy.None
                }
                current.arguments.isEmpty() -> {
                    SupertypesPolicy.LowerIfFlexible
                }
                else -> {
                    SupertypesPolicy.LowerIfFlexibleWithCustomSubstitutor(TypeConstructorSubstitution.create(current).buildSubstitutor())
                }
            }
        }

        return result ?: emptyList()
    }

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
    private fun selectOnlyPureKotlinSupertypes(supertypes: List<SimpleType>): List<SimpleType> {
        if (supertypes.size < 2) return supertypes

        val allPureSupertypes = supertypes.filter { it.arguments.all { !it.type.isFlexible() } }
        if (allPureSupertypes.isNotEmpty()) {
            return allPureSupertypes
        }
        else {
            return supertypes
        }
    }

    private fun effectiveVariance(declared: Variance, useSite: Variance): Variance? {
        if (declared == Variance.INVARIANT) return useSite
        if (useSite == Variance.INVARIANT) return declared

        // both not INVARIANT
        if (declared == useSite) return declared

        // composite In with Out
        return null
    }

    private fun TypeCheckerContext.isSubtypeForSameConstructor(
            capturedSubArguments: List<TypeProjection>,
            superType: SimpleType
    ): Boolean {
        val parameters = superType.constructor.parameters

        for (index in parameters.indices) {
            val superProjection = superType.arguments[index] // todo error index
            if (superProjection.isStarProjection) continue // A<B> <: A<*>

            val superArgumentType = superProjection.type.unwrap()
            val subArgumentType = capturedSubArguments[index].let {
                assert(it.projectionKind == Variance.INVARIANT) { "Incorrect sub argument: $it" }
                it.type.unwrap()
            }

            val variance = effectiveVariance(parameters[index].variance, superProjection.projectionKind)
                           ?: return errorTypeEqualsToAnything // todo exception?

            val correctArgument = runWithArgumentsSettings(subArgumentType) {
                when (variance) {
                    Variance.INVARIANT -> equalTypes(subArgumentType, superArgumentType)
                    Variance.OUT_VARIANCE -> isSubtypeOf(subArgumentType, superArgumentType)
                    Variance.IN_VARIANCE -> isSubtypeOf(superArgumentType, subArgumentType)
                }
            }
            if (!correctArgument) return false
        }
        return true
    }

}


object NullabilityChecker {

    // this method checks only nullability
    fun isPossibleSubtype(context: TypeCheckerContext, subType: SimpleType, superType: SimpleType): Boolean =
            context.runIsPossibleSubtype(subType, superType)

    private fun TypeCheckerContext.runIsPossibleSubtype(subType: SimpleType, superType: SimpleType): Boolean {
        // it makes for case String? & Any <: String
        assert(subType.isIntersectionType || subType.isSingleClassifierType || subType.isAllowedTypeVariable) {
            "Not singleClassifierType superType: $superType"
        }
        assert(superType.isSingleClassifierType || subType.isAllowedTypeVariable) {
            "Not singleClassifierType superType: $superType"
        }

        // superType is actually nullable
        if (superType.isMarkedNullable) return true

        // i.e. subType is not-nullable
        if (hasNotNullSupertype(subType, SupertypesPolicy.LowerIfFlexible)) return true

        // i.e subType hasn't not-null supertype, but superType has
        if (hasNotNullSupertype(superType, SupertypesPolicy.UpperIfFlexible)) return false

        // both superType and subType hasn't not-null supertype.

        /**
         * If we still don't know, it means, that superType is not classType, for example -- type parameter.
         *
         * For captured types with lower bound this function can give to you false result. Example:
         *  class A<T>, A<in Number> => \exist Q : Number <: Q. A<Q>
         *      isPossibleSubtype(Number, Q) = false.
         *      Such cases should be taken in to account in [NewKotlinTypeChecker.isSubtypeOf] (same for intersection types)
         */

        // classType cannot has special type in supertype list
        if (subType.isClassType) return false

        return hasPathByNotMarkedNullableNodes(subType, superType.constructor)
    }

    private fun TypeCheckerContext.hasNotNullSupertype(type: SimpleType, supertypesPolicy: SupertypesPolicy) =
            anySupertype(type, { it.isClassType && !it.isMarkedNullable }) {
                if (it.isMarkedNullable) SupertypesPolicy.None else supertypesPolicy
            }

    private fun TypeCheckerContext.hasPathByNotMarkedNullableNodes(start: SimpleType, end: TypeConstructor) =
            anySupertype(start, { !it.isMarkedNullable && it.constructor == end }) {
                if (it.isMarkedNullable) SupertypesPolicy.None else SupertypesPolicy.LowerIfFlexible
            }

}

/**
 * ClassType means that type constructor for this type is type for real class or interface
 */
private val SimpleType.isClassType: Boolean get() = constructor.declarationDescriptor is ClassDescriptor

/**
 * SingleClassifierType is one of the following types:
 *  - classType
 *  - type for type parameter
 *  - captured type
 *
 * Such types can contains error types in our arguments, but type constructor isn't errorTypeConstructor
 */
private val SimpleType.isSingleClassifierType: Boolean
    get() = !isError &&
            constructor.declarationDescriptor !is TypeAliasDescriptor &&
            (constructor.declarationDescriptor != null || this is CapturedType || this is NewCapturedType)

private val SimpleType.isIntersectionType: Boolean
    get() = constructor is IntersectionTypeConstructor