/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructedClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructorCopy
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.ensureResolved
import org.jetbrains.kotlin.fir.scopes.FakeOverrideTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirFakeOverrideGenerator
import org.jetbrains.kotlin.fir.scopes.scopeForClass
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name

private operator fun <T> Pair<T, *>?.component1() = this?.first
private operator fun <T> Pair<*, T>?.component2() = this?.second

internal fun FirScope.processConstructorsByName(
    name: Name,
    session: FirSession,
    bodyResolveComponents: BodyResolveComponents,
    includeInnerConstructors: Boolean,
    processor: (FirCallableSymbol<*>) -> Unit
) {
    // TODO: Handle case with two or more accessible classifiers
    val classifierInfo = getFirstClassifierOrNull(name)
    if (classifierInfo != null) {
        val (matchedClassifierSymbol, substitutor) = classifierInfo
        val matchedClassSymbol = matchedClassifierSymbol as? FirClassLikeSymbol<*>

        processConstructors(
            matchedClassSymbol,
            substitutor,
            processor,
            session,
            bodyResolveComponents,
            includeInnerConstructors
        )

        processSyntheticConstructors(
            matchedClassSymbol,
            processor,
            bodyResolveComponents
        )
    }
}

internal fun FirScope.processFunctionsAndConstructorsByName(
    name: Name,
    session: FirSession,
    bodyResolveComponents: BodyResolveComponents,
    includeInnerConstructors: Boolean,
    processor: (FirCallableSymbol<*>) -> Unit
) {
    processConstructorsByName(
        name, session, bodyResolveComponents,
        includeInnerConstructors = includeInnerConstructors,
        processor
    )

    processFunctionsByName(name, processor)
}

private fun FirScope.getFirstClassifierOrNull(name: Name): Pair<FirClassifierSymbol<*>, ConeSubstitutor>? {
    var result: Pair<FirClassifierSymbol<*>, ConeSubstitutor>? = null
    processClassifiersByNameWithSubstitution(name) { symbol, substitution ->
        if (result == null) {
            result = symbol to substitution
        }
    }

    return result
}

private fun processSyntheticConstructors(
    matchedSymbol: FirClassLikeSymbol<*>?,
    processor: (FirFunctionSymbol<*>) -> Unit,
    bodyResolveComponents: BodyResolveComponents
) {
    val samConstructor = matchedSymbol.findSAMConstructor(bodyResolveComponents)
    if (samConstructor != null) {
        processor(samConstructor.symbol)
    }
}

private fun FirClassLikeSymbol<*>?.findSAMConstructor(
    bodyResolveComponents: BodyResolveComponents
): FirSimpleFunction? {
    return when (this) {
        is FirRegularClassSymbol -> bodyResolveComponents.samResolver.getSamConstructor(fir)
        is FirTypeAliasSymbol -> findSAMConstructorForTypeAlias(bodyResolveComponents)
        is FirAnonymousObjectSymbol, null -> null
    }
}

private fun FirTypeAliasSymbol.findSAMConstructorForTypeAlias(
    bodyResolveComponents: BodyResolveComponents
): FirSimpleFunction? {
    val session = bodyResolveComponents.session
    val type =
        fir.expandedTypeRef.coneTypeUnsafe<ConeClassLikeType>().fullyExpandedType(session)

    val expansionRegularClass = type.lookupTag.toSymbol(session)?.fir as? FirRegularClass ?: return null
    val samConstructorForClass = bodyResolveComponents.samResolver.getSamConstructor(expansionRegularClass) ?: return null

    if (type.typeArguments.isEmpty()) return samConstructorForClass

    val namedSymbol = samConstructorForClass.symbol as? FirNamedFunctionSymbol ?: return null

    val substitutor = prepareSubstitutorForTypeAliasConstructors(
        type,
        session
    ) ?: return null

    val typeParameters = this@findSAMConstructorForTypeAlias.fir.typeParameters
    val newReturnType = samConstructorForClass.returnTypeRef.coneType.let(substitutor::substituteOrNull)

    val newParameterTypes = samConstructorForClass.valueParameters.map { valueParameter ->
        valueParameter.returnTypeRef.coneType.let(substitutor::substituteOrNull)
    }

    if (newReturnType == null && newParameterTypes.all { it == null }) return samConstructorForClass

    return FirFakeOverrideGenerator.createFakeOverrideFunction(
        session, samConstructorForClass, namedSymbol,
        newDispatchReceiverType = null,
        newReceiverType = null,
        newReturnType, newParameterTypes, typeParameters,
        expansionRegularClass.classId,
    ).fir
}

private fun processConstructors(
    matchedSymbol: FirClassLikeSymbol<*>?,
    substitutor: ConeSubstitutor,
    processor: (FirFunctionSymbol<*>) -> Unit,
    session: FirSession,
    bodyResolveComponents: BodyResolveComponents,
    includeInnerConstructors: Boolean
) {
    try {
        if (matchedSymbol != null) {
            val scope = when (matchedSymbol) {
                is FirTypeAliasSymbol -> {
                    matchedSymbol.ensureResolved(FirResolvePhase.TYPES, session)
                    val type = matchedSymbol.fir.expandedTypeRef.coneTypeUnsafe<ConeClassLikeType>().fullyExpandedType(session)
                    val basicScope = type.scope(session, bodyResolveComponents.scopeSession, FakeOverrideTypeCalculator.DoNothing)

                    if (basicScope != null && type.typeArguments.isNotEmpty()) {
                        TypeAliasConstructorsSubstitutingScope(
                            matchedSymbol,
                            basicScope
                        )
                    } else basicScope
                }
                is FirClassSymbol ->
                    (matchedSymbol.fir as FirClass<*>).scopeForClass(
                        substitutor, session, bodyResolveComponents.scopeSession
                    )
            }

            //TODO: why don't we use declared member scope at this point?
            scope?.processDeclaredConstructors {
                if (includeInnerConstructors || !it.fir.isInner) {
                    val constructorSymbolToProcess =
                        prepareCopyConstructorForTypealiasNestedClass(matchedSymbol, it, session, bodyResolveComponents) ?: it
                    processor(constructorSymbolToProcess)
                }
            }
        }
    } catch (e: Throwable) {
        throw RuntimeException("While processing constructors", e)
    }
}

private class TypeAliasConstructorsSubstitutingScope(
    private val typeAliasSymbol: FirTypeAliasSymbol,
    private val delegatingScope: FirScope
) : FirScope() {

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        delegatingScope.processDeclaredConstructors { originalConstructorSymbol ->

            val typeParameters = typeAliasSymbol.fir.typeParameters
            if (typeParameters.isEmpty()) processor(originalConstructorSymbol)
            else {
                processor(
                    buildConstructorCopy(originalConstructorSymbol.fir) {
                        symbol = FirConstructorSymbol(originalConstructorSymbol.callableId, overriddenSymbol = originalConstructorSymbol)
                        origin = FirDeclarationOrigin.SubstitutionOverride
                        this.typeParameters += typeParameters.map { buildConstructedClassTypeParameterRef { symbol = it.symbol } }
                    }.symbol
                )
            }
        }
    }
}

private fun prepareSubstitutorForTypeAliasConstructors(
    expandedType: ConeClassLikeType,
    session: FirSession
): ConeSubstitutor? {
    val expandedClass = expandedType.lookupTag.toSymbol(session)?.fir as? FirRegularClass ?: return null

    val resultingTypeArguments = expandedType.typeArguments.map {
        // We don't know how to handle cases like yet
        // typealias A = ArrayList<*>()
        it as? ConeKotlinType ?: return null
    }
    return substitutorByMap(
        expandedClass.typeParameters.map { it.symbol }.zip(resultingTypeArguments).toMap()
    )
}

private fun prepareCopyConstructorForTypealiasNestedClass(
    matchedSymbol: FirClassLikeSymbol<*>,
    originalSymbol: FirConstructorSymbol,
    session: FirSession,
    bodyResolveComponents: BodyResolveComponents,
): FirConstructorSymbol? {
    // If the matched symbol is a type alias, and the expanded type is a nested class, e.g.,
    //
    //   class Outer {
    //     inner class Inner
    //   }
    //   typealias OI = Outer.Inner
    //   fun foo() { Outer().OI() }
    //
    // the chances are that `processor` belongs to [ScopeTowerLevel] (to resolve type aliases at top-level), which treats
    // the explicit receiver (`Outer()`) as an extension receiver, whereas the constructor of the nested class may regard
    // the same explicit receiver as a dispatch receiver (hence inconsistent receiver).
    // Here, we add a copy of the nested class constructor, along with the outer type as an extension receiver, so that it
    // can be seen as if resolving:
    //
    //   fun Outer.OI(): OI = ...
    //
    if (originalSymbol.callableId.classId?.isNestedClass == true && matchedSymbol is FirTypeAliasSymbol) {
        val innerTypeRef = originalSymbol.fir.returnTypeRef
        val innerType = innerTypeRef.coneType.fullyExpandedType(session) as? ConeClassLikeType
        if (innerType != null) {
            val outerType = bodyResolveComponents.outerClassManager.outerType(innerType)
            if (outerType != null) {
                val extCopy = buildConstructorCopy(originalSymbol.fir) {
                    origin = FirDeclarationOrigin.Synthetic
                    receiverTypeRef = innerTypeRef.withReplacedConeType(outerType)
                    symbol = FirConstructorSymbol(originalSymbol.callableId)
                }.apply {
                    originalConstructorIfTypeAlias = originalSymbol.fir
                }
                return extCopy.symbol
            }
        }
    }
    return null
}

private object TypeAliasConstructorKey : FirDeclarationDataKey()

var FirConstructor.originalConstructorIfTypeAlias: FirConstructor? by FirDeclarationDataRegistry.data(TypeAliasConstructorKey)
