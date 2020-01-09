/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import com.intellij.openapi.progress.ProcessCanceledException
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirConstructorImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope
import org.jetbrains.kotlin.fir.scopes.impl.withReplacedConeType
import org.jetbrains.kotlin.fir.scopes.scope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal fun FirScope.processFunctionsAndConstructorsByName(
    name: Name,
    session: FirSession,
    bodyResolveComponents: BodyResolveComponents,
    noInnerConstructors: Boolean = false,
    processor: (FirCallableSymbol<*>) -> Unit
) {
    // TODO: Handle case with two or more accessible classifiers
    val matchedClassSymbol = getFirstClassifierOrNull(name) as? FirClassLikeSymbol<*>

    processConstructors(
        matchedClassSymbol,
        processor,
        session,
        bodyResolveComponents.scopeSession,
        name,
            noInnerConstructors
        )

    processSyntheticConstructors(
        matchedClassSymbol,
        processor,
        bodyResolveComponents
    )

    processFunctionsByName(name) {
        if (it !is FirConstructorSymbol) {
            processor(it)
        }
    }
}

private fun FirScope.getFirstClassifierOrNull(name: Name): FirClassifierSymbol<*>? {
    var result: FirClassifierSymbol<*>? = null
    processClassifiersByName(name) {
        if (result == null) {
            result = it
        }
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

    val substitutor = prepareSubstitutorForTypeAliasConstructors<FirSimpleFunction>(
        this,
        type,
        session
    ) { newReturnType, newParameterTypes, newTypeParameters ->
        FirClassSubstitutionScope.createFakeOverrideFunction(
            session, this, namedSymbol, null,
            newReturnType, newParameterTypes, newTypeParameters
        ).fir
    } ?: return null

    return substitutor.substitute(samConstructorForClass)
}

private fun processConstructors(
    matchedSymbol: FirClassLikeSymbol<*>?,
    processor: (FirFunctionSymbol<*>) -> Unit,
    session: FirSession,
    scopeSession: ScopeSession,
    name: Name,
    noInner: Boolean
) {
    try {
        if (matchedSymbol != null) {
            val scope = when (matchedSymbol) {
                is FirTypeAliasSymbol -> {
                    val type = matchedSymbol.fir.expandedTypeRef.coneTypeUnsafe<ConeClassLikeType>().fullyExpandedType(session)
                    val basicScope = type.scope(session, scopeSession)

                    if (basicScope != null && type.typeArguments.isNotEmpty()) {
                        prepareSubstitutingScopeForTypeAliasConstructors(
                            matchedSymbol, type, session, basicScope
                        ) ?: return
                    } else basicScope
                }
                is FirClassSymbol -> (matchedSymbol.fir as FirClass<*>).scope(ConeSubstitutor.Empty, session, scopeSession)
            }

            val constructorName = when (matchedSymbol) {
                is FirTypeAliasSymbol -> finalExpansionName(matchedSymbol, session) ?: return
                is FirClassSymbol -> name
            }

            //TODO: why don't we use declared member scope at this point?
            scope?.processFunctionsByName(constructorName) {
                if (!noInner || (it as? FirConstructorSymbol)?.fir?.isInner != true) {
                    processor(it)
                }
            }
        }
    } catch (e: ProcessCanceledException) {
        throw e
    } catch (e: Throwable) {
        throw RuntimeException("While processing constructors", e)
    }
}

private class TypeAliasConstructorsSubstitutingScope(
    private val typeAliasConstructorsSubstitutor: TypeAliasConstructorsSubstitutor<FirConstructor>,
    private val delegatingScope: FirScope
) : FirScope() {
    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        delegatingScope.processFunctionsByName(name) {
            val toProcess = if (it is FirConstructorSymbol) {
                typeAliasConstructorsSubstitutor.substitute(it.fir).symbol
            } else {
                it
            }

            processor(toProcess)
        }
    }
}

private typealias ConstructorCopyFactory<F> =
        F.(newReturnType: ConeKotlinType?, newValueParameterTypes: List<ConeKotlinType?>, newTypeParameters: List<FirTypeParameter>) -> F

private class TypeAliasConstructorsSubstitutor<F : FirMemberFunction<F>>(
    private val typeAliasSymbol: FirTypeAliasSymbol,
    private val substitutor: ConeSubstitutor,
    private val copyFactory: ConstructorCopyFactory<F>
) {
    fun substitute(baseFunction: F): F {
        val typeParameters = typeAliasSymbol.fir.typeParameters
        val newReturnType = baseFunction.returnTypeRef.coneTypeUnsafe<ConeKotlinType>().let(substitutor::substituteOrNull)

        val newParameterTypes = baseFunction.valueParameters.map { valueParameter ->
            valueParameter.returnTypeRef.coneTypeUnsafe<ConeKotlinType>().let(substitutor::substituteOrNull)
        }

        if (newReturnType == null && newParameterTypes.all { it == null }) return baseFunction

        return baseFunction.copyFactory(
            newReturnType,
            newParameterTypes,
            typeParameters
        )
    }
}

private fun prepareSubstitutingScopeForTypeAliasConstructors(
    typeAliasSymbol: FirTypeAliasSymbol,
    expandedType: ConeClassLikeType,
    session: FirSession,
    delegatingScope: FirScope
): FirScope? {
    val typeAliasConstructorsSubstitutor =
        prepareSubstitutorForTypeAliasConstructors<FirConstructor>(
            typeAliasSymbol,
            expandedType,
            session
        ) factory@{ newReturnType, newParameterTypes, newTypeParameters ->
            FirConstructorImpl(
                source, session,
                returnTypeRef.withReplacedConeType(newReturnType),
                receiverTypeRef, status,
                FirConstructorSymbol(symbol.callableId, overriddenSymbol = symbol)
            ).apply {
                resolvePhase = this@factory.resolvePhase
                valueParameters +=
                    this@factory.valueParameters.zip(
                        newParameterTypes
                    ) { valueParameter, newParameterType ->
                        with(valueParameter) {
                            FirValueParameterImpl(
                                source,
                                session,
                                returnTypeRef.withReplacedConeType(newParameterType),
                                name,
                                FirVariableSymbol(valueParameter.symbol.callableId),
                                defaultValue,
                                isCrossinline,
                                isNoinline,
                                isVararg
                            )
                        }
                    }
                this.typeParameters += newTypeParameters
            }
        } ?: return null

    return TypeAliasConstructorsSubstitutingScope(
        typeAliasConstructorsSubstitutor,
        delegatingScope
    )
}

private fun <F : FirMemberFunction<F>> prepareSubstitutorForTypeAliasConstructors(
    typeAliasSymbol: FirTypeAliasSymbol,
    expandedType: ConeClassLikeType,
    session: FirSession,
    copyFactory: ConstructorCopyFactory<F>
): TypeAliasConstructorsSubstitutor<F>? {
    val expandedClass = expandedType.lookupTag.toSymbol(session)?.fir as? FirRegularClass ?: return null

    val resultingTypeArguments = expandedType.typeArguments.map {
        // We don't know how to handle cases like yet
        // typealias A = ArrayList<*>()
        it as? ConeKotlinType ?: return null
    }

    val substitutor = substitutorByMap(
        expandedClass.typeParameters.map { it.symbol }.zip(resultingTypeArguments).toMap()
    )

    return TypeAliasConstructorsSubstitutor(typeAliasSymbol, substitutor, copyFactory)
}
