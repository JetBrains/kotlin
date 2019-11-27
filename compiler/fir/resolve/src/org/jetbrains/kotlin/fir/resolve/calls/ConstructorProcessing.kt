/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirConstructorImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal fun FirScope.processFunctionsAndConstructorsByName(
    name: Name,
    session: FirSession,
    bodyResolveComponents: BodyResolveComponents,
    processor: (FirCallableSymbol<*>) -> ProcessorAction
): ProcessorAction {
    val matchedClassSymbol = getFirstClassifierOrNull(name) as? FirClassLikeSymbol<*>

    if (processConstructors(
            matchedClassSymbol,
            processor,
            session,
            bodyResolveComponents.scopeSession,
            name
        ).stop()
    ) {
        return ProcessorAction.STOP
    }

    if (processSyntheticConstructors(
            matchedClassSymbol,
            processor,
            bodyResolveComponents
        ).stop()
    ) {
        return ProcessorAction.STOP
    }

    return processFunctionsByName(name) {
        if (it is FirConstructorSymbol) ProcessorAction.NEXT
        else processor(it)
    }
}

private fun FirScope.getFirstClassifierOrNull(name: Name): FirClassifierSymbol<*>? {
    var result: FirClassifierSymbol<*>? = null
    processClassifiersByName(name) {
        result = it
        ProcessorAction.STOP
    }

    return result
}

private fun finalExpansionName(symbol: FirTypeAliasSymbol, session: FirSession): Name? {
    val expandedType = symbol.fir.expandedTypeRef.coneTypeUnsafe<ConeClassLikeType>()
    val typeAliasSymbol = expandedType.lookupTag.toSymbol(session)?.safeAs<FirTypeAliasSymbol>()

    return if (typeAliasSymbol != null)
        finalExpansionName(typeAliasSymbol, session)
    else
        expandedType.lookupTag.classId.shortClassName
}

private fun processSyntheticConstructors(
    matchedSymbol: FirClassLikeSymbol<*>?,
    processor: (FirFunctionSymbol<*>) -> ProcessorAction,
    bodyResolveComponents: BodyResolveComponents
): ProcessorAction {
    if (matchedSymbol == null) return ProcessorAction.NEXT
    if (matchedSymbol !is FirRegularClassSymbol) return ProcessorAction.NEXT

    val function = bodyResolveComponents.samResolver.getSamConstructor(matchedSymbol.fir) ?: return ProcessorAction.NEXT

    return processor(function.symbol)
}

private fun processConstructors(
    matchedSymbol: FirClassLikeSymbol<*>?,
    processor: (FirFunctionSymbol<*>) -> ProcessorAction,
    session: FirSession,
    scopeSession: ScopeSession,
    name: Name
): ProcessorAction {
    try {
        if (matchedSymbol != null) {
            val scope = when (matchedSymbol) {
                is FirTypeAliasSymbol -> {
                    val type = matchedSymbol.fir.expandedTypeRef.coneTypeUnsafe<ConeClassLikeType>().fullyExpandedType(session)
                    val basicScope = type.scope(session, scopeSession)

                    if (basicScope != null && type.typeArguments.isNotEmpty()) {
                        prepareSubstitutingScopeForTypeAliasConstructors(
                            matchedSymbol, type, session, basicScope
                        ) ?: return ProcessorAction.STOP
                    } else basicScope
                }
                is FirClassSymbol -> matchedSymbol.buildUseSiteMemberScope(session, scopeSession)
            }

            val constructorName = when (matchedSymbol) {
                is FirTypeAliasSymbol -> finalExpansionName(matchedSymbol, session) ?: return ProcessorAction.NEXT
                is FirClassSymbol -> name
            }

            //TODO: why don't we use declared member scope at this point?
            if (scope != null && scope.processFunctionsByName(
                    constructorName,
                    processor
                ) == ProcessorAction.STOP
            ) {
                return ProcessorAction.STOP
            }
        }
        return ProcessorAction.NEXT
    } catch (e: Throwable) {
        throw RuntimeException("While processing constructors", e)
    }
}

private class TypeAliasConstructorsSubstitutingScope(
    private val typeAliasSymbol: FirTypeAliasSymbol,
    private val substitutor: ConeSubstitutor,
    private val delegatingScope: FirScope
) : FirScope() {
    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> ProcessorAction): ProcessorAction {
        return delegatingScope.processFunctionsByName(name) {
            val toProcess = if (it is FirConstructorSymbol) {
                it.fir.createTypeAliasConstructor(substitutor, typeAliasSymbol.fir.typeParameters).symbol
            } else {
                it
            }

            processor(toProcess)
        }
    }
}

private fun prepareSubstitutingScopeForTypeAliasConstructors(
    typeAliasSymbol: FirTypeAliasSymbol,
    expandedType: ConeClassLikeType,
    session: FirSession,
    delegatingScope: FirScope
): FirScope? {
    val expandedClass = expandedType.lookupTag.toSymbol(session)?.fir as? FirRegularClass ?: return null

    val resultingTypeArguments = expandedType.typeArguments.map {
        // We don't know how to handle cases like yet
        // typealias A = ArrayList<*>()
        it as? ConeKotlinType ?: return null
    }

    val substitutor = substitutorByMap(
        expandedClass.typeParameters.map { it.symbol }.zip(resultingTypeArguments).toMap()
    );

    return TypeAliasConstructorsSubstitutingScope(typeAliasSymbol, substitutor, delegatingScope)
}

private fun FirConstructor.createTypeAliasConstructor(
    substitutor: ConeSubstitutor,
    typeParameters: List<FirTypeParameter>
): FirConstructor {
    val newReturnTypeRef = returnTypeRef.substitute(substitutor)

    val newParameterTypeRefs = valueParameters.map { valueParameter ->
        valueParameter.returnTypeRef.substitute(substitutor)
    }

    if (newReturnTypeRef == null && newParameterTypeRefs.all { it == null }) return this

    return FirConstructorImpl(
        source, session, newReturnTypeRef ?: returnTypeRef, receiverTypeRef, status,
        FirConstructorSymbol(symbol.callableId, overriddenSymbol = symbol)
    ).apply {
        resolvePhase = this@createTypeAliasConstructor.resolvePhase
        valueParameters += this@createTypeAliasConstructor.valueParameters.zip(
            newParameterTypeRefs
        ) { valueParameter, newTypeRef ->
            with(valueParameter) {
                FirValueParameterImpl(
                    source,
                    session,
                    newTypeRef ?: returnTypeRef,
                    name,
                    FirVariableSymbol(valueParameter.symbol.callableId),
                    defaultValue,
                    isCrossinline,
                    isNoinline,
                    isVararg
                )
            }
        }
        this.typeParameters += typeParameters
    }
}

private fun FirTypeRef.substitute(substitutor: ConeSubstitutor): FirResolvedTypeRef? =
    coneTypeUnsafe<ConeKotlinType>()
        .let(substitutor::substituteOrNull)
        ?.let { FirResolvedTypeRefImpl(source, it) }
