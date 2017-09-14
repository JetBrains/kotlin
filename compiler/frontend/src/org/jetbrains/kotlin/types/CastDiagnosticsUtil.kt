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

package org.jetbrains.kotlin.types

import com.google.common.collect.Maps
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedure
import org.jetbrains.kotlin.types.expressions.DataFlowAnalyzer
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

object CastDiagnosticsUtil {

    // As this method produces a warning, it must be _complete_ (not sound), i.e. every time it says "cast impossible",
    // it must be really impossible
    @JvmStatic
    fun isCastPossible(
            lhsType: KotlinType,
            rhsType: KotlinType,
            platformToKotlinClassMap: PlatformToKotlinClassMap
    ): Boolean {
        val rhsNullable = TypeUtils.isNullableType(rhsType)
        val lhsNullable = TypeUtils.isNullableType(lhsType)
        if (KotlinBuiltIns.isNullableNothing(lhsType) && !rhsNullable) return false
        if (KotlinBuiltIns.isNothing(rhsType)) return false
        if (KotlinBuiltIns.isNullableNothing(rhsType)) return lhsNullable
        if (lhsNullable && rhsNullable) return true
        if (lhsType.isError) return true
        if (isRelated(lhsType, rhsType, platformToKotlinClassMap)) return true
        // This is an oversimplification (which does not render the method incomplete):
        // we consider any type parameter capable of taking any value, which may be made more precise if we considered bounds
        if (TypeUtils.isTypeParameter(lhsType) || TypeUtils.isTypeParameter(rhsType)) return true

        if (isFinal(lhsType) || isFinal(rhsType)) return false
        if (isTrait(lhsType) || isTrait(rhsType)) return true
        return false
    }

    /**
     * Two types are related, roughly, when one of them is a subtype of the other constructing class
     *
     * Note that some types have platform-specific counterparts, i.e. kotlin.String is mapped to java.lang.String,
     * such types (and all their sub- and supertypes) are related too.
     *
     * Due to limitations in PlatformToKotlinClassMap, we only consider mapping of platform classes to Kotlin classed
     * (i.e. java.lang.String -> kotlin.String) and ignore mappings that go the other way.
     */
    private fun isRelated(a: KotlinType, b: KotlinType, platformToKotlinClassMap: PlatformToKotlinClassMap): Boolean {
        val aClasses = mapToPlatformClasses(a, platformToKotlinClassMap)
        val bClasses = mapToPlatformClasses(b, platformToKotlinClassMap)

        return aClasses.any { DescriptorUtils.isSubtypeOfClass(b, it) } || bClasses.any { DescriptorUtils.isSubtypeOfClass(a, it) }
    }

    private fun mapToPlatformClasses(
            type: KotlinType,
            platformToKotlinClassMap: PlatformToKotlinClassMap
    ): List<ClassDescriptor> {
        val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return listOf()

        return platformToKotlinClassMap.mapPlatformClass(descriptor) + descriptor
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
        val isNonReifiedTypeParameter = TypeUtils.isNonReifiedTypeParameter(subtype)
        val isUpcast = typeChecker.isSubtypeOf(supertype, subtype)

        // here we want to restrict cases such as `x is T` for x = T?, when T might have nullable upper bound
        if (isNonReifiedTypeParameter && !isUpcast) {
            // hack to save previous behavior in case when `x is T`, where T is not nullable, see IsErasedNullableTasT.kt
            val nullableToDefinitelyNotNull = !TypeUtils.isNullableType(subtype) && supertype.makeNotNullable() == subtype
            if (!nullableToDefinitelyNotNull) {
                return true
            }
        }

        // cast between T and T? is always OK
        if (supertype.isMarkedNullable || subtype.isMarkedNullable) {
            return isCastErased(TypeUtils.makeNotNullable(supertype), TypeUtils.makeNotNullable(subtype), typeChecker)
        }

        // if it is a upcast, it's never erased
        if (isUpcast) return false

        // downcasting to a non-reified type parameter is always erased
        if (isNonReifiedTypeParameter) return true

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

        val substitution: MutableMap<TypeConstructor, TypeProjection> = if (supertypeWithVariables != null) {
            // Now, let's try to unify Collection<T> and Collection<Foo> solution is a map from T to Foo
            val solution = TypeUnifier.unify(
                    TypeProjectionImpl(supertype), TypeProjectionImpl(supertypeWithVariables), variableConstructors::contains
            )
            Maps.newHashMap(solution.substitution)
        }
        else {
            // If there's no corresponding supertype, no variables are determined
            // This may be OK, e.g. in case 'Any as List<*>'
            Maps.newHashMapWithExpectedSize<TypeConstructor, TypeProjection>(variables.size)
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

    fun castIsUseless(
            expression: KtBinaryExpressionWithTypeRHS,
            context: ExpressionTypingContext,
            targetType: KotlinType,
            actualType: KotlinType
    ): Boolean {
        // Here: x as? Type <=> x as Type?
        val refinedTargetType = if (KtPsiUtil.isSafeCast(expression)) TypeUtils.makeNullable(targetType) else targetType
        val possibleTypes = DataFlowAnalyzer.getAllPossibleTypes(expression.left, actualType, context)
        return isRefinementUseless(possibleTypes, refinedTargetType, shouldCheckForExactType(expression, context.expectedType))
    }

    // It is a warning "useless cast" for `as` and a warning "redundant is" for `is`
    fun isRefinementUseless(
            possibleTypes: Collection<KotlinType>,
            targetType: KotlinType,
            shouldCheckForExactType: Boolean
    ): Boolean {
        val intersectedType = TypeIntersector.intersectTypes(possibleTypes.map { it.upperIfFlexible() }) ?: return false

        return if (shouldCheckForExactType)
            isExactTypeCast(intersectedType, targetType)
        else
            isUpcast(intersectedType, targetType)
    }

    private fun shouldCheckForExactType(expression: KtBinaryExpressionWithTypeRHS, expectedType: KotlinType): Boolean {
        if (TypeUtils.noExpectedType(expectedType)) {
            return checkExactTypeForUselessCast(expression)
        }

        // If expected type is parameterized, then cast has an effect on inference, therefore it isn't a useless cast
        // Otherwise, we are interested in situation like: `a: Any? = 1 as Int?`
        return TypeUtils.isDontCarePlaceholder(expectedType)
    }

    private fun isExactTypeCast(candidateType: KotlinType, targetType: KotlinType): Boolean {
        return candidateType == targetType && candidateType.isExtensionFunctionType == targetType.isExtensionFunctionType
    }

    private fun isUpcast(candidateType: KotlinType, targetType: KotlinType): Boolean {
        if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(candidateType, targetType)) return false

        if (candidateType.isFunctionType && targetType.isFunctionType) {
            return candidateType.isExtensionFunctionType == targetType.isExtensionFunctionType
        }

        return true
    }

    // Casting an argument or a receiver to a supertype may be useful to select an exact overload of a method.
    // Casting to a supertype in other contexts is unlikely to be useful.
    private fun checkExactTypeForUselessCast(expression: KtBinaryExpressionWithTypeRHS): Boolean {
        var parent = expression.parent
        while (parent is KtParenthesizedExpression ||
               parent is KtLabeledExpression ||
               parent is KtAnnotatedExpression) {
            parent = parent.parent
        }

        return when (parent) {
            is KtValueArgument -> true

            is KtQualifiedExpression -> {
                val receiver = parent.receiverExpression
                PsiTreeUtil.isAncestor(receiver, expression, false)
            }

            // in binary expression, left argument can be a receiver and right an argument
            // in unary expression, left argument can be a receiver
            is KtBinaryExpression, is KtUnaryExpression -> true

            // Previously we've checked that there is no expected type, therefore cast in property has an effect on inference
            is KtProperty, is KtPropertyAccessor -> true

            else -> false
        }
    }
}
