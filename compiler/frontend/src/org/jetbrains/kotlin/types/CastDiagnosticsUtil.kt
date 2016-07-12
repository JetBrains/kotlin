/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types

import com.google.common.base.Predicates
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedure
import org.jetbrains.kotlin.builtins.KotlinBuiltIns

object CastDiagnosticsUtil {

    // As this method produces a warning, it must be _complete_ (not sound), i.e. every time it says "cast impossible",
    // it must be really impossible
    @JvmStatic
    fun isCastPossible(
            lhsType: KotlinType,
            rhsType: KotlinType,
            platformToKotlinClassMap: PlatformToKotlinClassMap): Boolean {
        if (KotlinBuiltIns.isNullableNothing(lhsType) && !TypeUtils.isNullableType(rhsType)) return false
        if (isRelated(lhsType, rhsType, platformToKotlinClassMap)) return true
        // This is an oversimplification (which does not render the method incomplete):
        // we consider any type parameter capable of taking any value, which may be made more precise if we considered bounds
        if (TypeUtils.isTypeParameter(lhsType) || TypeUtils.isTypeParameter(rhsType)) return true
        if (isFinal(lhsType) || isFinal(rhsType)) return false
        if (isTrait(lhsType) || isTrait(rhsType)) return true
        return false
    }

    /**
     * Two types are related, roughly, when one is a subtype or supertype of the other.
     *
     *
     * Note that some types have platform-specific counterparts, i.e. kotlin.String is mapped to java.lang.String,
     * such types (and all their sub- and supertypes) are related too.
     *
     *
     * Due to limitations in PlatformToKotlinClassMap, we only consider mapping of platform classes to Kotlin classed
     * (i.e. java.lang.String -> kotlin.String) and ignore mappings that go the other way.
     */
    private fun isRelated(a: KotlinType, b: KotlinType, platformToKotlinClassMap: PlatformToKotlinClassMap): Boolean {
        val aTypes = mapToPlatformIndependentTypes(TypeUtils.makeNotNullable(a), platformToKotlinClassMap)
        val bTypes = mapToPlatformIndependentTypes(TypeUtils.makeNotNullable(b), platformToKotlinClassMap)

        for (aType in aTypes) {
            for (bType in bTypes) {
                if (KotlinTypeChecker.DEFAULT.isSubtypeOf(aType, bType)) return true
                if (KotlinTypeChecker.DEFAULT.isSubtypeOf(bType, aType)) return true
            }
        }

        return false
    }

    private fun mapToPlatformIndependentTypes(
            type: KotlinType,
            platformToKotlinClassMap: PlatformToKotlinClassMap): List<KotlinType> {
        val descriptor = type.constructor.declarationDescriptor
        if (descriptor !is ClassDescriptor) return listOf(type)

        val kotlinClasses = platformToKotlinClassMap.mapPlatformClass(descriptor)
        if (kotlinClasses.isEmpty()) return listOf(type)

        val result = Lists.newArrayListWithCapacity<KotlinType>(2)
        result.add(type)
        for (classDescriptor in kotlinClasses) {
            val kotlinType = TypeUtils.substituteProjectionsForParameters(classDescriptor, type.arguments)
            result.add(kotlinType)
        }

        return result
    }

    private fun isFinal(type: KotlinType) = !TypeUtils.canHaveSubtypes(KotlinTypeChecker.DEFAULT, type)

    private fun isTrait(type: KotlinType) =
            type.constructor.declarationDescriptor.let { it is ClassDescriptor && it.kind == ClassKind.INTERFACE }

    /**
     * Check if cast from supertype to subtype is erased.
     * It is an error in "is" statement and warning in "as".
     */
    @JvmStatic
    fun isCastErased(supertype: KotlinType, subtype: KotlinType, typeChecker: KotlinTypeChecker): Boolean {
        // cast between T and T? is always OK
        if (supertype.isMarkedNullable || subtype.isMarkedNullable) {
            return isCastErased(TypeUtils.makeNotNullable(supertype), TypeUtils.makeNotNullable(subtype), typeChecker)
        }

        // if it is a upcast, it's never erased
        if (typeChecker.isSubtypeOf(supertype, subtype)) return false

        // downcasting to a non-reified type parameter is always erased
        if (TypeUtils.isNonReifiedTypeParameter(subtype)) return true

        // Check that we are actually casting to a generic type
        // NOTE: this does not account for 'as Array<List<T>>'
        if (allParametersReified(subtype)) return false

        val staticallyKnownSubtype = findStaticallyKnownSubtype(supertype, subtype.constructor).resultingType ?: return true

        // If the substitution failed, it means that the result is an impossible type, e.g. something like Out<in Foo>
        // In this case, we can't guarantee anything, so the cast is considered to be erased

        // If the type we calculated is a subtype of the cast target, it's OK to use the cast target instead.
        // If not, it's wrong to use it
        return !typeChecker.isSubtypeOf(staticallyKnownSubtype, subtype)
    }

    /**
     * Remember that we are trying to cast something of type `supertype` to `subtype`.

     * Since at runtime we can only check the class (type constructor), the rest of the subtype should be known statically, from supertype.
     * This method reconstructs all static information that can be obtained from supertype.

     * Example 1:
     * supertype = Collection
     * subtype = List<...>
     * result = List, all arguments are inferred

     * Example 2:
     * supertype = Any
     * subtype = List<...>
     * result = List<*>, some arguments were not inferred, replaced with '*'
     */
    @JvmStatic
    fun findStaticallyKnownSubtype(supertype: KotlinType, subtypeConstructor: TypeConstructor): TypeReconstructionResult {
        assert(!supertype.isMarkedNullable) { "This method only makes sense for non-nullable types" }

        // Assume we are casting an expression of type Collection<Foo> to List<Bar>
        // First, let's make List<T>, where T is a type variable
        val descriptor = subtypeConstructor.declarationDescriptor ?: error("Can't create default type for " + subtypeConstructor)
        val subtypeWithVariables = descriptor.defaultType

        // Now, let's find a supertype of List<T> that is a Collection of something,
        // in this case it will be Collection<T>
        val supertypeWithVariables = TypeCheckingProcedure.findCorrespondingSupertype(subtypeWithVariables, supertype)

        val variables = subtypeWithVariables.constructor.parameters
        val variableConstructors = variables.map { descriptor -> descriptor.typeConstructor }.toSet()

        val substitution: MutableMap<TypeConstructor, TypeProjection>
        if (supertypeWithVariables != null) {
            // Now, let's try to unify Collection<T> and Collection<Foo> solution is a map from T to Foo
            val solution = TypeUnifier.unify(
                    TypeProjectionImpl(supertype), TypeProjectionImpl(supertypeWithVariables),
                    Predicates.`in`(variableConstructors))
            substitution = Maps.newHashMap(solution.substitution)
        }
        else {
            // If there's no corresponding supertype, no variables are determined
            // This may be OK, e.g. in case 'Any as List<*>'
            substitution = Maps.newHashMapWithExpectedSize<TypeConstructor, TypeProjection>(variables.size)
        }

        // If some of the parameters are not determined by unification, it means that these parameters are lost,
        // let's put stars instead, so that we can only cast to something like List<*>, e.g. (a: Any) as List<*>
        var allArgumentsInferred = true
        for (variable in variables) {
            val value = substitution[variable.typeConstructor]
            if (value == null) {
                substitution.put(
                        variable.typeConstructor,
                        TypeUtils.makeStarProjection(variable))
                allArgumentsInferred = false
            }
        }

        // At this point we have values for all type parameters of List
        // Let's make a type by substituting them: List<T> -> List<Foo>
        val substituted = TypeSubstitutor.create(substitution).substitute(subtypeWithVariables, Variance.INVARIANT)

        return TypeReconstructionResult(substituted, allArgumentsInferred)
    }

    private fun allParametersReified(subtype: KotlinType) = subtype.constructor.parameters.all { it.isReified }
}
