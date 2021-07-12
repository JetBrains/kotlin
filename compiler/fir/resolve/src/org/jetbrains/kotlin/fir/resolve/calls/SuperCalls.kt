/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.dispatchReceiverClassOrNull
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.FakeOverrideTypeCalculator
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addIfNotNull

fun BodyResolveComponents.findTypesForSuperCandidates(
    superTypeRefs: List<FirTypeRef>,
    containingCall: FirQualifiedAccess,
): Collection<ConeKotlinType> {
    val supertypes = superTypeRefs.map { (it as FirResolvedTypeRef).type }
    val isMethodOfAny = containingCall is FirFunctionCall && isCallingMethodOfAny(containingCall)
    if (supertypes.size <= 1 && !isMethodOfAny) return supertypes

    return when (containingCall) {
        is FirFunctionCall -> {
            val calleeName = containingCall.calleeReference.name
            if (isMethodOfAny) {
                resolveSupertypesForMethodOfAny(supertypes, calleeName)
            } else {
                resolveSupertypesByCalleeName(supertypes, calleeName)
            }
        }
        else -> {
            resolveSupertypesByPropertyName(
                supertypes,
                (containingCall.calleeReference as? FirNamedReference)?.name ?: return emptyList()
            )
        }
    }
}

private val ARITY_OF_METHODS_OF_ANY = hashMapOf("hashCode" to 0, "equals" to 1, "toString" to 0)

private fun isCallingMethodOfAny(callExpression: FirFunctionCall): Boolean =
    ARITY_OF_METHODS_OF_ANY.getOrElse(callExpression.calleeReference.name.asString(), { -1 }) == callExpression.argumentList.arguments.size

private fun BodyResolveComponents.resolveSupertypesForMethodOfAny(
    supertypes: Collection<ConeKotlinType>,
    calleeName: Name
): Collection<ConeKotlinType> {
    val typesWithConcreteOverride = resolveSupertypesByMembers(supertypes, false) {
        getFunctionMembers(it, calleeName)
    }
    return if (typesWithConcreteOverride.isNotEmpty())
        typesWithConcreteOverride
    else
        listOf(session.builtinTypes.anyType.type)
}

private fun BodyResolveComponents.resolveSupertypesByCalleeName(supertypes: Collection<ConeKotlinType>, calleeName: Name): Collection<ConeKotlinType> =
    resolveSupertypesByMembers(supertypes, true) {
        getFunctionMembers(it, calleeName) +
                getPropertyMembers(it, calleeName)
    }

private fun BodyResolveComponents.resolveSupertypesByPropertyName(supertypes: Collection<ConeKotlinType>, propertyName: Name): Collection<ConeKotlinType> =
    resolveSupertypesByMembers(supertypes, true) {
        getPropertyMembers(it, propertyName)
    }

private inline fun BodyResolveComponents.resolveSupertypesByMembers(
    supertypes: Collection<ConeKotlinType>,
    allowNonConcreteMembers: Boolean,
    getMembers: (ConeKotlinType) -> Collection<FirCallableDeclaration>
): Collection<ConeKotlinType> {
    val typesWithConcreteMembers = SmartList<ConeKotlinType>()
    val typesWithNonConcreteMembers = SmartList<ConeKotlinType>()

    for (supertype in supertypes) {
        val members = getMembers(supertype)
        if (members.isNotEmpty()) {
            if (members.any { isConcreteMember(supertype, it) })
                typesWithConcreteMembers.add(supertype)
            else
                typesWithNonConcreteMembers.add(supertype)
        }
    }

    typesWithConcreteMembers.removeAll { typeWithConcreteMember ->
        typesWithNonConcreteMembers.any { typeWithNonConcreteMember ->
            AbstractTypeChecker.isSubtypeOf(session.typeContext, typeWithNonConcreteMember, typeWithConcreteMember)
        }
    }

    return when {
        typesWithConcreteMembers.isNotEmpty() ->
            typesWithConcreteMembers
        allowNonConcreteMembers ->
            typesWithNonConcreteMembers
        else ->
            emptyList()
    }
}

@OptIn(ExperimentalStdlibApi::class)
private fun BodyResolveComponents.getFunctionMembers(type: ConeKotlinType, name: Name): Collection<FirCallableDeclaration> =
    buildList {
        type.scope(session, scopeSession, FakeOverrideTypeCalculator.DoNothing)?.processFunctionsByName(name) {
            add(it.fir)
        }
    }

@OptIn(ExperimentalStdlibApi::class)
private fun BodyResolveComponents.getPropertyMembers(type: ConeKotlinType, name: Name): Collection<FirCallableDeclaration> =
    buildList {
        type.scope(session, scopeSession, FakeOverrideTypeCalculator.DoNothing)?.processPropertiesByName(name) {
            addIfNotNull(it.fir as? FirProperty)
        }
    }


private fun BodyResolveComponents.isConcreteMember(supertype: ConeKotlinType, member: FirCallableDeclaration): Boolean {
    // "Concrete member" is a function or a property that is not abstract,
    // and is not an implicit fake override for a method of Any on an interface.

    if (member.modality == Modality.ABSTRACT)
        return false

    val classSymbol =
        (supertype as? ConeClassLikeType)?.lookupTag?.toSymbol(session) as? FirRegularClassSymbol ?: return true
    if (classSymbol.fir.classKind != ClassKind.INTERFACE) return true
    return member.symbol.unwrapFakeOverrides().dispatchReceiverClassOrNull()?.classId != StandardClassIds.Any
}
