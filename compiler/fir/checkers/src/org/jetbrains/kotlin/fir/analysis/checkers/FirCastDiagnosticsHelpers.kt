/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.platformClassMapper
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeChecker.findCorrespondingSupertypes
import org.jetbrains.kotlin.types.model.typeConstructor

fun isCastErased(supertype: ConeKotlinType, subtype: ConeKotlinType, context: CheckerContext): Boolean {
    val typeContext = context.session.typeContext

    val isNonReifiedTypeParameter = subtype.isNonReifiedTypeParameter()
    val isUpcast = isUpcast(context, supertype, subtype)

    // here we want to restrict cases such as `x is T` for x = T?, when T might have nullable upper bound
    if (isNonReifiedTypeParameter && !isUpcast) {
        // hack to save previous behavior in case when `x is T`, where T is not nullable, see IsErasedNullableTasT.kt
        val nullableToDefinitelyNotNull = !subtype.canBeNull && supertype.withNullability(ConeNullability.NOT_NULL, typeContext) == subtype
        if (!nullableToDefinitelyNotNull) {
            return true
        }
    }

    // cast between T and T? is always OK
    if (supertype.isMarkedNullable || subtype.isMarkedNullable) {
        return isCastErased(
            supertype.withNullability(ConeNullability.NOT_NULL, typeContext),
            subtype.withNullability(ConeNullability.NOT_NULL, typeContext),
            context
        )
    }

    // if it is a upcast, it's never erased
    if (isUpcast) return false

    // downcasting to a non-reified type parameter is always erased
    if (isNonReifiedTypeParameter) return true

    // Check that we are actually casting to a generic type
    // NOTE: this does not account for 'as Array<List<T>>'
    if (subtype.allParameterReified()) return false

    val staticallyKnownSubtype = findStaticallyKnownSubtype(supertype, subtype, context).first ?: return true

    // If the substitution failed, it means that the result is an impossible type, e.g. something like Out<in Foo>
    // In this case, we can't guarantee anything, so the cast is considered to be erased

    // If the type we calculated is a subtype of the cast target, it's OK to use the cast target instead.
    // If not, it's wrong to use it
    return !AbstractTypeChecker.isSubtypeOf(context.session.typeContext, staticallyKnownSubtype, subtype)
}

private fun ConeKotlinType.allParameterReified(): Boolean {
    return typeArguments.all { (it.type as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol?.isReified == true }
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
fun findStaticallyKnownSubtype(
    supertype: ConeKotlinType,
    subtype: ConeKotlinType,
    context: CheckerContext
): Pair<ConeKotlinType?, Boolean> {
    assert(!supertype.isMarkedNullable) { "This method only makes sense for non-nullable types" }

    val session = context.session
    val typeContext = session.typeContext

    // Assume we are casting an expression of type Collection<Foo> to List<Bar>
    // First, let's make List<T>, where T is a type variable
    val subtypeWithVariables = subtype.toRegularClassSymbol(session)!!
    val subtypeWithVariablesType = subtypeWithVariables.defaultType()

    // Now, let's find a supertype of List<T> that is a Collection of something,
    // in this case it will be Collection<T>
    val typeCheckerState = context.session.typeContext.newTypeCheckerState(
        errorTypesEqualToAnything = false,
        stubTypesEqualToAnything = true
    )

    fun getFirstNotIntersectedType(type: ConeKotlinType): ConeKotlinType? {
        if (type is ConeIntersectionType) {
            for (intersectionType in type.intersectedTypes) {
                val result = getFirstNotIntersectedType(intersectionType)
                if (result != null) {
                    return result
                }
            }
            return null
        }
        return type
    }

    // Obtaining not intersected type to get not null typeConstructor.
    // Not sure if it's correct
    val notIntersectedSupertype = getFirstNotIntersectedType(supertype) ?: supertype
    val supertypeWithVariables =
        findCorrespondingSupertypes(
            typeCheckerState,
            subtypeWithVariablesType,
            notIntersectedSupertype.typeConstructor(typeContext)
        ).firstOrNull()

    val variables = subtypeWithVariables.typeParameterSymbols

    val substitution = if (supertypeWithVariables != null) {
        // Now, let's try to unify Collection<T> and Collection<Foo> solution is a map from T to Foo
        val typeUnifier = TypeUnifier(session, variables)
        val unificationResult = typeUnifier.unify(supertype, supertypeWithVariables as ConeKotlinTypeProjection)
        unificationResult.substitution.toMutableMap()
    } else {
        mutableMapOf()
    }

    // If some of the parameters are not determined by unification, it means that these parameters are lost,
    // let's put stars instead, so that we can only cast to something like List<*>, e.g. (a: Any) as List<*>
    var allArgumentsInferred = true
    for (variable in variables) {
        val value = substitution[variable]
        if (value == null) {
            substitution[variable] = context.session.builtinTypes.nullableAnyType.type
            allArgumentsInferred = false
        }
    }

    // At this point we have values for all type parameters of List
    // Let's make a type by substituting them: List<T> -> List<Foo>
    val substitutor = ConeSubstitutorByMap(substitution, session)
    val substituted = substitutor.substituteOrSelf(subtypeWithVariablesType)

    return Pair(substituted, allArgumentsInferred)
}

fun ConeKotlinType.isNonReifiedTypeParameter(): Boolean {
    return this is ConeTypeParameterType && !this.lookupTag.typeParameterSymbol.isReified
}

@Suppress("UNUSED_PARAMETER")
fun shouldCheckForExactType(expression: FirTypeOperatorCall, context: CheckerContext): Boolean {
    return when (expression.operation) {
        FirOperation.IS, FirOperation.NOT_IS -> false
        // TODO: differentiate if this expression defines the enclosing thing's type
        //   e.g.,
        //   val c1 get() = 1 as Number
        //   val c2: Number get() = 1 <!USELESS_CAST!>as Number<!>
        FirOperation.AS, FirOperation.SAFE_AS -> true
        else -> throw AssertionError("Should not be here: ${expression.operation}")
    }
}

fun isRefinementUseless(
    context: CheckerContext,
    candidateType: ConeKotlinType,
    targetType: ConeKotlinType,
    shouldCheckForExactType: Boolean,
    arg: FirExpression,
): Boolean {
    return if (shouldCheckForExactType) {
        if (arg is FirFunctionCall) {
            val functionSymbol = arg.toResolvedCallableSymbol() as? FirFunctionSymbol<*>
            if (functionSymbol != null && functionSymbol.isFunctionForExpectTypeFromCastFeature()) return false
        }

        isExactTypeCast(context, candidateType, targetType)
    } else {
        isUpcast(context, candidateType, targetType)
    }
}

private fun isExactTypeCast(context: CheckerContext, candidateType: ConeKotlinType, targetType: ConeKotlinType): Boolean {
    if (!AbstractTypeChecker.equalTypes(context.session.typeContext, candidateType, targetType, stubTypesEqualToAnything = false))
        return false
    // See comments at [isUpcast] why we need to check the existence of @ExtensionFunctionType
    return candidateType.isExtensionFunctionType == targetType.isExtensionFunctionType
}

private fun isUpcast(context: CheckerContext, candidateType: ConeKotlinType, targetType: ConeKotlinType): Boolean {
    if (!AbstractTypeChecker.isSubtypeOf(context.session.typeContext, candidateType, targetType, stubTypesEqualToAnything = false))
        return false

    // E.g., foo(p1: (X) -> Y), where p1 has a functional type whose receiver type is X and return type is Y.
    // For bar(p2: X.() -> Y), p2 has the same functional type (with same receiver and return types).
    // The only difference is the existence of type annotation, @ExtensionFunctionType,
    //   which indicates that the annotated type represents an extension function.
    // If one casts p1 to p2 (or vice versa), it is _not_ up cast, i.e., not redundant, yet meaningful.
    return candidateType.isExtensionFunctionType == targetType.isExtensionFunctionType
}