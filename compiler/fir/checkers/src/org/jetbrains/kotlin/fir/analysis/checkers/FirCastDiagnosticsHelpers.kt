/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.getClassAndItsOuterClassesWhenLocal
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
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
        val nullableToDefinitelyNotNull = !subtype.canBeNull(context.session) && supertype.withNullability(nullable = false, typeContext) == subtype
        if (!nullableToDefinitelyNotNull) {
            return true
        }
    }

    // cast between T and T? is always OK
    if ((supertype !is ConeErrorType && supertype.isMarkedNullable) || (subtype !is ConeErrorType && subtype.isMarkedNullable)) {
        return isCastErased(
            supertype.withNullability(nullable = false, typeContext),
            subtype.withNullability(nullable = false, typeContext),
            context
        )
    }

    // if it is a upcast, it's never erased
    if (isUpcast) return false

    // downcasting to a non-reified type parameter is always erased
    if (isNonReifiedTypeParameter) return true
    // downcasting to a reified type parameter is never erased
    else if (subtype is ConeTypeParameterType) return false

    val regularClassSymbol = subtype.toRegularClassSymbol(context.session) ?: return true

    val outerClasses = regularClassSymbol.getClassAndItsOuterClassesWhenLocal(context.session)

    if (regularClassSymbol.isLocal && regularClassSymbol.typeParameterSymbols.any { it.containingDeclarationSymbol !in outerClasses }) {
        return true
    }

    val staticallyKnownSubtype = findStaticallyKnownSubtype(supertype, regularClassSymbol, context)

    // If the substitution failed, it means that the result is an impossible type, e.g. something like Out<in Foo>
    // In this case, we can't guarantee anything, so the cast is considered to be erased

    // If the type we calculated is a subtype of the cast target, it's OK to use the cast target instead.
    // If not, it's wrong to use it
    return !AbstractTypeChecker.isSubtypeOf(
        context.session.typeContext.newTypeCheckerState(
            errorTypesEqualToAnything = true,
            stubTypesEqualToAnything = false,
            dnnTypesEqualToFlexible = false
        ),
        staticallyKnownSubtype,
        subtype
    )
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
    subTypeClassSymbol: FirRegularClassSymbol,
    context: CheckerContext
): ConeKotlinType {
    assert(!supertype.isMarkedNullable) { "This method only makes sense for non-nullable types" }

    val session = context.session
    val typeContext = session.typeContext

    if (supertype is ConeClassLikeType && supertype.toSymbol(session) == subTypeClassSymbol) {
        return supertype
    }

    // Assume we are casting an expression of type Collection<Foo> to List<Bar>
    // First, let's make List<T>, where T is a type variable
    val subtypeWithVariablesType = subTypeClassSymbol.defaultType()

    // Now, let's find a supertype of List<T> that is a Collection of something,
    // in this case it will be Collection<T>
    val typeCheckerState = context.session.typeContext.newTypeCheckerState(
        errorTypesEqualToAnything = false,
        stubTypesEqualToAnything = false
    )

    val normalizedTypes = if (supertype is ConeIntersectionType) {
        supertype.intersectedTypes
    } else {
        ArrayList<ConeKotlinType>(1).also { it.add(supertype) }
    }

    val resultSubstitution = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()

    for (normalizedType in normalizedTypes) {
        val supertypeWithVariables =
            findCorrespondingSupertypes(
                typeCheckerState,
                subtypeWithVariablesType,
                normalizedType.typeConstructor(typeContext)
            ).firstOrNull()

        val variables: List<FirTypeParameterSymbol> = subTypeClassSymbol.typeParameterSymbols

        val substitution = if (supertypeWithVariables != null) {
            // Now, let's try to unify Collection<T> and Collection<Foo> solution is a map from T to Foo
            val result = mutableMapOf<FirTypeParameterSymbol, ConeTypeProjection>()
            if (context.session.doUnify(
                    supertype,
                    supertypeWithVariables as ConeKotlinTypeProjection,
                    variables.toSet(),
                    result
                )
            ) {
                result
            } else {
                mutableMapOf()
            }
        } else {
            mutableMapOf()
        }

        // If some parameters are not determined by unification, it means that these parameters are lost,
        // let's put ConeStubType instead, so that we can only cast to something like List<*>, e.g. (a: Any) as List<*>
        for (variable in variables) {
            val resultValue = when (val value = substitution[variable]) {
                null -> null
                is ConeStarProjection -> {
                    ConeStubTypeForTypeVariableInSubtyping(ConeTypeVariable("", null), isMarkedNullable = true)
                }
                else -> value.type
            }
            if (resultValue != null) {
                resultSubstitution[variable] = resultValue
            }
        }
    }

    // At this point we have values for all type parameters of List
    // Let's make a type by substituting them: List<T> -> List<Foo>
    val substitutor = substitutorByMap(resultSubstitution, session)
    return substitutor.substituteOrSelf(subtypeWithVariablesType)
}

fun ConeKotlinType.isNonReifiedTypeParameter(): Boolean {
    return this is ConeTypeParameterType && !this.lookupTag.typeParameterSymbol.isReified
}

fun isUpcast(context: CheckerContext, candidateType: ConeKotlinType, targetType: ConeKotlinType): Boolean =
    AbstractTypeChecker.isSubtypeOf(context.session.typeContext, candidateType, targetType, stubTypesEqualToAnything = false)

internal fun isRefinementUseless(
    context: CheckerContext,
    lhsType: ConeKotlinType,
    targetType: ConeKotlinType,
    expression: FirTypeOperatorCall,
): Boolean {
    if (lhsType is ConeErrorType || targetType is ConeErrorType) {
        return false
    }

    val arg = expression.argument

    return when (expression.operation) {
        FirOperation.AS, FirOperation.SAFE_AS -> {
            if (arg is FirFunctionCall) {
                val functionSymbol = arg.toResolvedCallableSymbol(context.session) as? FirFunctionSymbol<*>
                if (functionSymbol != null && functionSymbol.isFunctionForExpectTypeFromCastFeature()) return false
            }

            // Normalize `targetType` for cases like the following:
            // fun f(x: Int?) { x as? Int } // USELESS_CAST is reasonable here
            val refinedTargetType =
                if (expression.operation == FirOperation.SAFE_AS && lhsType.isMarkedOrFlexiblyNullable) {
                    targetType.withNullability(nullable = true, context.session.typeContext)
                } else {
                    targetType
                }
            isExactTypeCast(context, lhsType, refinedTargetType)
        }
        FirOperation.IS, FirOperation.NOT_IS -> {
            isUpcast(context, lhsType, targetType)
        }
        else -> throw AssertionError("Should not be here: ${expression.operation}")
    }
}

private fun isExactTypeCast(context: CheckerContext, lhsType: ConeKotlinType, targetType: ConeKotlinType): Boolean =
    AbstractTypeChecker.equalTypes(context.session.typeContext, lhsType, targetType, stubTypesEqualToAnything = false)
