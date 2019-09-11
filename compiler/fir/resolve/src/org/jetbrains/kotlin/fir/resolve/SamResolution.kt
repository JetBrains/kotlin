/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.name.Name

interface FirSamResolver : FirSessionComponent {
    fun getFunctionTypeForPossibleSamType(type: ConeKotlinType): ConeKotlinType?
    fun shouldRunSamConversionForFunction(firNamedFunction: FirNamedFunction): Boolean
}

private val NULL_STUB = Any()

class FirSamResolverImpl(
    private val firSession: FirSession,
    private val scopeSession: ScopeSession
) : FirSamResolver {

    private val cache: MutableMap<FirRegularClass, Any> = mutableMapOf()

    override fun getFunctionTypeForPossibleSamType(type: ConeKotlinType): ConeKotlinType? {
        return when (type) {
            is ConeClassType -> getFunctionTypeForPossibleSamType(type)
            is ConeFlexibleType -> ConeFlexibleType(
                getFunctionTypeForPossibleSamType(type.lowerBound) ?: return null,
                getFunctionTypeForPossibleSamType(type.upperBound) ?: return null
            )
            is ConeClassErrorType -> null
            // TODO: support those types as well
            is ConeAbbreviatedType, is ConeTypeParameterType, is ConeTypeVariableType,
            is ConeCapturedType, is ConeDefinitelyNotNullType, is ConeIntersectionType -> null
            // TODO: Thing of getting rid of this branch since ConeLookupTagBasedType should be a sealed class
            is ConeLookupTagBasedType -> null
        }
    }

    private fun getFunctionTypeForPossibleSamType(type: ConeClassType): ConeLookupTagBasedType? {
        val firRegularClass =
            firSession.firSymbolProvider
                .getSymbolByLookupTag(type.lookupTag)
                ?.fir as? FirRegularClass
                ?: return null

        val unsubstitutedFunctionType = resolveFunctionTypeIfSamInterface(firRegularClass) ?: return null
        val substitutor =
            substitutorByMap(
                firRegularClass.typeParameters
                    .map { it.symbol }
                    .zip(type.typeArguments.map {
                        (it as? ConeTypedProjection)?.type
                            ?: ConeClassTypeImpl(ConeClassLikeLookupTagImpl(StandardClassIds.Any), emptyArray(), isNullable = true)
                    })
                    .toMap()
            )

        val result =
            substitutor
                .substituteOrSelf(unsubstitutedFunctionType)
                .withNullability(ConeNullability.create(type.isMarkedNullable))

        require(result is ConeLookupTagBasedType) {
            "Function type should always be ConeLookupTagBasedType, but ${result::class} was found"
        }

        return result
    }

    private fun resolveFunctionTypeIfSamInterface(firRegularClass: FirRegularClass): ConeKotlinType? {
        return cache.getOrPut(firRegularClass) {
            val abstractMethod = firRegularClass.getSingleAbstractMethodOrNull(firSession, scopeSession) ?: return@getOrPut NULL_STUB
            // TODO: val shouldConvertFirstParameterToDescriptor = samWithReceiverResolvers.any { it.shouldConvertFirstSamParameterToReceiver(abstractMethod) }

            abstractMethod.getFunctionTypeForAbstractMethod(firSession)
        } as? ConeKotlinType
    }

    override fun shouldRunSamConversionForFunction(firNamedFunction: FirNamedFunction): Boolean {
        // TODO: properly support, see org.jetbrains.kotlin.load.java.sam.JvmSamConversionTransformer.shouldRunSamConversionForFunction
        return true
    }
}

private fun FirRegularClass.getSingleAbstractMethodOrNull(
    session: FirSession,
    scopeSession: ScopeSession
): FirNamedFunction? {
    // TODO: restrict to Java interfaces
    if (classKind != ClassKind.INTERFACE || hasMoreThenOneAbstractFunctionOrHasAbstractProperty()) return null

    val samCandidateNames = computeSamCandidateNames(session)

    return findSingleAbstractMethodByNames(session, scopeSession, samCandidateNames)
}

private fun FirRegularClass.computeSamCandidateNames(session: FirSession): Set<Name> {
    val classes =
        lookupSuperTypes(this, lookupInterfaces = true, deep = true, useSiteSession = session)
            .mapNotNullTo(mutableListOf(this)) {
                (session.firSymbolProvider.getSymbolByLookupTag(it.lookupTag) as? FirClassSymbol)?.fir
            }

    val samCandidateNames = mutableSetOf<Name>()
    for (clazz in classes) {
        for (declaration in clazz.declarations) {
            if (declaration !is FirMemberDeclaration || declaration.modality != Modality.ABSTRACT) continue
            samCandidateNames.add(declaration.name)
        }
    }

    return samCandidateNames
}

private fun FirRegularClass.findSingleAbstractMethodByNames(
    session: FirSession,
    scopeSession: ScopeSession,
    samCandidateNames: Set<Name>
): FirNamedFunction? {
    var resultMethod: FirNamedFunction? = null
    var metIncorrectMember = false

    val classUseSiteMemberScope = session.firSymbolProvider.getClassUseSiteMemberScope(classId, session, scopeSession)

    for (candidateName in samCandidateNames) {
        if (classUseSiteMemberScope == null) break
        if (metIncorrectMember) break

        classUseSiteMemberScope.processPropertiesByName(candidateName) {
            if ((it as? FirProperty)?.modality == Modality.ABSTRACT) {
                metIncorrectMember = true
                ProcessorAction.STOP
            } else {
                ProcessorAction.NEXT
            }
        }

        if (metIncorrectMember) break

        classUseSiteMemberScope.processFunctionsByName(candidateName) { functionSymbol ->
            val firFunction = functionSymbol.fir
            require(firFunction is FirNamedFunction) {
                "${functionSymbol.callableId.callableName} is expected to be FirNamedFunction, but ${functionSymbol::class} was found"
            }

            if (firFunction.modality != Modality.ABSTRACT) return@processFunctionsByName ProcessorAction.NEXT

            if (resultMethod != null) {
                metIncorrectMember = true
                ProcessorAction.STOP
            } else {
                resultMethod = firFunction
                ProcessorAction.NEXT
            }
        }
    }

    if (metIncorrectMember || resultMethod == null || resultMethod!!.typeParameters.isNotEmpty()) return null

    return resultMethod
}

private fun FirRegularClass.hasMoreThenOneAbstractFunctionOrHasAbstractProperty(): Boolean {
    var wasAbstractFunction = false
    for (declaration in declarations) {
        if (declaration is FirProperty && declaration.modality == Modality.ABSTRACT) return true
        if (declaration is FirNamedFunction && declaration.modality == Modality.ABSTRACT) {
            if (wasAbstractFunction) return true
            wasAbstractFunction = true
        }
    }

    return false
}

private fun FirNamedFunction.getFunctionTypeForAbstractMethod(session: FirSession): ConeLookupTagBasedType {
    val parameterTypes = valueParameters.map {
        it.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: ConeKotlinErrorType("No type for parameter $it")
    }

    return createFunctionalType(
        session, parameterTypes,
        receiverType = null,
        rawReturnType = returnTypeRef.coneTypeSafe() ?: ConeKotlinErrorType("No type for return type of $this")
    )
}
