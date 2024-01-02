/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.getClassAndItsOuterClassesWhenLocal
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.scopes.platformClassMapper
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeChecker.findCorrespondingSupertypes
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.model.typeConstructor

enum class CastingType {
    Possible,
    Impossible,
    Always
}

fun checkCasting(
    lhsType: ConeKotlinType,
    rhsType: ConeKotlinType,
    isSafeCase: Boolean,
    context: CheckerContext
): CastingType {
    val lhsLowerType = lhsType.lowerBoundIfFlexible().originalIfDefinitelyNotNullable()
    val rhsLowerType = rhsType.lowerBoundIfFlexible().originalIfDefinitelyNotNullable()

    if (lhsLowerType is ConeErrorType || rhsLowerType is ConeErrorType) return CastingType.Possible

    val session = context.session

    if (lhsLowerType is ConeIntersectionType) {
        var result = false
        for (intersectedType in lhsLowerType.intersectedTypes) {
            val isIntersectedCastPossible = checkCasting(intersectedType, rhsLowerType, isSafeCase, context)
            val intersectedTypeSymbol = intersectedType.toRegularClassSymbol(session)
            if (intersectedTypeSymbol?.isInterface == false && isIntersectedCastPossible == CastingType.Impossible) {
                return CastingType.Impossible // Any class type in intersection type should be subtype of RHS
            }
            result = result or (isIntersectedCastPossible != CastingType.Impossible)
        }

        return if (result) CastingType.Possible else CastingType.Impossible
    }

    val lhsNullable = lhsLowerType.canBeNull(session)
    val rhsNullable = rhsLowerType.canBeNull(session)
    if (lhsLowerType.isNothing) return CastingType.Possible
    if (lhsLowerType.isNullableNothing && !rhsNullable) {
        return if (isSafeCase) CastingType.Always else CastingType.Impossible
    }
    if (rhsLowerType.isNothing) return CastingType.Impossible
    if (rhsLowerType.isNullableNothing) {
        return if (lhsNullable) CastingType.Possible else CastingType.Impossible
    }
    if (lhsNullable && rhsNullable) return CastingType.Possible

    // This is an oversimplification (which does not render the method incomplete):
    // we consider any type parameter capable of taking any value, which may be made more precise if we considered bounds
    if (lhsLowerType is ConeTypeParameterType || rhsLowerType is ConeTypeParameterType) return CastingType.Possible

    val lhsClassSymbol = lhsLowerType.toRegularClassSymbol(session)
    val rhsClassSymbol = rhsLowerType.toRegularClassSymbol(session)
    val lhsNormalizedType = getCorrespondingKotlinClass(lhsClassSymbol?.defaultType() ?: lhsLowerType, session)
    val rhsNormalizedType = getCorrespondingKotlinClass(rhsClassSymbol?.defaultType() ?: rhsLowerType, session)

    val state = session.typeContext.newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = false)

    // It's an optimization, the code below with `isRoughSubtypeOf` also checks subtyping, but it's slower
    if (AbstractTypeChecker.isSubtypeOf(state, lhsNormalizedType, rhsNormalizedType) ||
        AbstractTypeChecker.isSubtypeOf(state, rhsNormalizedType, lhsNormalizedType)
    ) {
        return CastingType.Possible
    }

    if (isRoughSubtypeOf(lhsNormalizedType, rhsNormalizedType, state, session) ||
        isRoughSubtypeOf(rhsNormalizedType, lhsNormalizedType, state, session)
    ) {
        return CastingType.Possible
    }

    if (isFinal(lhsNormalizedType, session) || isFinal(rhsNormalizedType, session)) return CastingType.Impossible

    val lhsNormalizedTypeSymbol = lhsNormalizedType.toSymbol(session) as? FirClassSymbol<*>
    val rhsNormalizedTypeSymbol = rhsNormalizedType.toSymbol(session) as? FirClassSymbol<*>
    if (lhsNormalizedTypeSymbol?.isInterface == true || rhsNormalizedTypeSymbol?.isInterface == true) return CastingType.Possible

    return CastingType.Impossible
}

/**
 * One type is roughly subtype of another superType when one of type's supertype constructor equals another superType constructor.
 *
 * Note that some types have platform-specific counterparts, i.e. kotlin.String is mapped to java.lang.String,
 * such types (and all their sub- and supertypes) are related too.
 *
 * Due to limitations in PlatformToKotlinClassMap, we only consider mapping of platform classes to Kotlin classed
 * (i.e. java.lang.String -> kotlin.String) and ignore mappings that go the other way.
 */
private fun isRoughSubtypeOf(
    type: ConeSimpleKotlinType,
    superType: ConeSimpleKotlinType,
    state: TypeCheckerState,
    session: FirSession
): Boolean {
    var result = false
    val superTypeConstructor = superType.typeConstructor(state.typeSystemContext)
    state.anySupertype(type, { typeMarker ->
        val correspondingKotlinClass = getCorrespondingKotlinClass(typeMarker as ConeSimpleKotlinType, session)
        if (correspondingKotlinClass.typeConstructor(state.typeSystemContext) == superTypeConstructor) {
            result = true
            true
        } else {
            false
        }
    }, { TypeCheckerState.SupertypesPolicy.LowerIfFlexible })

    return result
}

private fun getCorrespondingKotlinClass(type: ConeSimpleKotlinType, session: FirSession): ConeSimpleKotlinType {
    return session.platformClassMapper.getCorrespondingKotlinClass(type.classId)?.defaultType(emptyList()) ?: type
}

private fun isFinal(type: ConeSimpleKotlinType, session: FirSession): Boolean {
    return !type.canHaveSubtypes(session)
}

fun isCastErased(supertype: ConeKotlinType, subtype: ConeKotlinType, context: CheckerContext): Boolean {
    val typeContext = context.session.typeContext

    val isNonReifiedTypeParameter = subtype.isNonReifiedTypeParameter()
    val isUpcast = isUpcast(context, supertype, subtype)

    // here we want to restrict cases such as `x is T` for x = T?, when T might have nullable upper bound
    if (isNonReifiedTypeParameter && !isUpcast) {
        // hack to save previous behavior in case when `x is T`, where T is not nullable, see IsErasedNullableTasT.kt
        val nullableToDefinitelyNotNull = !subtype.canBeNull(context.session) && supertype.withNullability(ConeNullability.NOT_NULL, typeContext) == subtype
        if (!nullableToDefinitelyNotNull) {
            return true
        }
    }

    // cast between T and T? is always OK
    if ((supertype !is ConeErrorType && supertype.isMarkedNullable) || (subtype !is ConeErrorType && subtype.isMarkedNullable)) {
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
    return !AbstractTypeChecker.isSubtypeOf(context.session.typeContext, staticallyKnownSubtype, subtype, stubTypesEqualToAnything = false)
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
                    ConeStubTypeForTypeVariableInSubtyping(ConeTypeVariable("", null), ConeNullability.NULLABLE)
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
    val substitutor = ConeSubstitutorByMap(resultSubstitution, session)
    return substitutor.substituteOrSelf(subtypeWithVariablesType)
}

fun ConeKotlinType.isNonReifiedTypeParameter(): Boolean {
    return this is ConeTypeParameterType && !this.lookupTag.typeParameterSymbol.isReified
}

fun isUpcast(context: CheckerContext, candidateType: ConeKotlinType, targetType: ConeKotlinType): Boolean {
    if (!AbstractTypeChecker.isSubtypeOf(context.session.typeContext, candidateType, targetType, stubTypesEqualToAnything = false))
        return false

    // E.g., foo(p1: (X) -> Y), where p1 has a function type whose receiver type is X and return type is Y.
    // For bar(p2: X.() -> Y), p2 has the same function type (with same receiver and return types).
    // The only difference is the existence of type annotation, @ExtensionFunctionType,
    //   which indicates that the annotated type represents an extension function.
    // If one casts p1 to p2 (or vice versa), it is _not_ up cast, i.e., not redundant, yet meaningful.
    return candidateType.isExtensionFunctionType == targetType.isExtensionFunctionType
}
