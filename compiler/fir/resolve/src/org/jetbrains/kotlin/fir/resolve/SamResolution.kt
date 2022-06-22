/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.NullableMap
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getOrPut
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirTypeParameterBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.diagnostics.ConeIntermediateDiagnostic
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.impl.hasTypeOf
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance

abstract class FirSamResolver {
    abstract fun getSamInfoForPossibleSamType(type: ConeKotlinType): SAMInfo<ConeKotlinType>?
    abstract fun shouldRunSamConversionForFunction(firFunction: FirFunction): Boolean
    abstract fun getSamConstructor(firRegularClass: FirRegularClass): FirSimpleFunction?

    fun getFunctionTypeForPossibleSamType(type: ConeKotlinType): ConeKotlinType? =
        getSamInfoForPossibleSamType(type)?.type
}

private val SAM_PARAMETER_NAME = Name.identifier("function")

data class SAMInfo<out C : ConeKotlinType>(internal val symbol: FirNamedFunctionSymbol, val type: C)

class FirSamResolverImpl(
    private val session: FirSession,
    private val scopeSession: ScopeSession,
    private val outerClassManager: FirOuterClassManager? = null,
) : FirSamResolver() {
    private val resolvedFunctionType: NullableMap<FirRegularClass, SAMInfo<ConeLookupTagBasedType>?> = NullableMap()
    private val samConstructorsCache = session.samConstructorStorage.samConstructors
    private val samConversionTransformers = session.extensionService.samConversionTransformers

    override fun getSamInfoForPossibleSamType(type: ConeKotlinType): SAMInfo<ConeKotlinType>? {
        return when (type) {
            is ConeClassLikeType -> getFunctionTypeForPossibleSamType(type.fullyExpandedType(session))
            is ConeFlexibleType -> {
                val (lowerSymbol, lowerType) = getSamInfoForPossibleSamType(type.lowerBound) ?: return null
                val (_, upperType) = getSamInfoForPossibleSamType(type.upperBound) ?: return null
                SAMInfo(
                    lowerSymbol,
                    ConeFlexibleType(lowerType.lowerBoundIfFlexible(), upperType.upperBoundIfFlexible())
                )
            }
            is ConeErrorType, is ConeStubType -> null
            // TODO: support those types as well
            is ConeTypeParameterType, is ConeTypeVariableType,
            is ConeCapturedType, is ConeDefinitelyNotNullType, is ConeIntersectionType,
            is ConeIntegerLiteralType,
            -> null
            // TODO: Thing of getting rid of this branch since ConeLookupTagBasedType should be a sealed class
            is ConeLookupTagBasedType -> null
        }
    }

    private fun getFunctionTypeForPossibleSamType(type: ConeClassLikeType): SAMInfo<ConeLookupTagBasedType>? {
        @OptIn(LookupTagInternals::class)
        val firRegularClass = type.lookupTag.toFirRegularClass(session) ?: return null

        val (functionSymbol, unsubstitutedFunctionType) = resolveFunctionTypeIfSamInterface(firRegularClass) ?: return null

        if (firRegularClass.typeParameters.isEmpty()) {
            return SAMInfo(
                functionSymbol,
                unsubstitutedFunctionType.withNullability(ConeNullability.create(type.isMarkedNullable), session.typeContext)
            )
        }

        val substitutor =
            substitutorByMap(
                firRegularClass.typeParameters
                    .map { it.symbol }
                    .zip(
                        type.typeArguments,
                    ).map { (parameterSymbol, projection) ->
                        val typeArgument =
                            (projection as? ConeKotlinTypeProjection)?.type
                            // TODO: Consider using `parameterSymbol.fir.bounds.first().coneType` once sure that it won't fail with exception
                                ?: parameterSymbol.fir.bounds.firstOrNull()?.coneTypeSafe()
                                ?: session.builtinTypes.nullableAnyType.type

                        Pair(parameterSymbol, typeArgument)
                    }
                    .toMap(),
                session
            )

        val result =
            substitutor
                .substituteOrSelf(unsubstitutedFunctionType)
                .withNullability(ConeNullability.create(type.isMarkedNullable), session.typeContext)

        require(result is ConeLookupTagBasedType) {
            "Function type should always be ConeLookupTagBasedType, but ${result::class} was found"
        }

        return SAMInfo(functionSymbol, result)
    }

    override fun getSamConstructor(firRegularClass: FirRegularClass): FirSimpleFunction? {
        return samConstructorsCache.getValue(firRegularClass.symbol, this)?.fir
    }

    fun buildSamConstructor(classSymbol: FirRegularClassSymbol): FirNamedFunctionSymbol? {
        val firRegularClass = classSymbol.fir
        val (functionSymbol, functionType) = resolveFunctionTypeIfSamInterface(firRegularClass) ?: return null

        val classId = firRegularClass.classId
        val symbol = FirSyntheticFunctionSymbol(
            CallableId(
                classId.packageFqName,
                classId.relativeClassName.parent().takeIf { !it.isRoot },
                classId.shortClassName,
            ),
        )

        val newTypeParameters = firRegularClass.typeParameters.map { typeParameter ->
            val declaredTypeParameter = typeParameter.symbol.fir // TODO: or really declared?
            FirTypeParameterBuilder().apply {
                source = declaredTypeParameter.source
                moduleData = session.moduleData
                origin = FirDeclarationOrigin.SamConstructor
                resolvePhase = FirResolvePhase.DECLARATIONS
                name = declaredTypeParameter.name
                this.symbol = FirTypeParameterSymbol()
                variance = Variance.INVARIANT
                isReified = false
                annotations += declaredTypeParameter.annotations
                containingDeclarationSymbol = symbol
            }
        }

        val newTypeParameterTypes =
            newTypeParameters
                .map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), isNullable = false) }

        val substitutor = substitutorByMap(
            firRegularClass.typeParameters
                .map { it.symbol }
                .zip(newTypeParameterTypes).toMap(),
            session
        )

        for ((newTypeParameter, oldTypeParameter) in newTypeParameters.zip(firRegularClass.typeParameters)) {
            val declared = oldTypeParameter.symbol.fir // TODO: or really declared?
            newTypeParameter.bounds += declared.symbol.resolvedBounds.map { typeRef ->
                buildResolvedTypeRef {
                    source = typeRef.source
                    type = substitutor.substituteOrSelf(typeRef.coneType)
                }
            }
        }

        return buildSimpleFunction {
            moduleData = session.moduleData
            source = firRegularClass.source
            name = classId.shortClassName
            origin = FirDeclarationOrigin.SamConstructor
            val visibility = firRegularClass.visibility
            status = FirResolvedDeclarationStatusImpl(
                visibility,
                Modality.FINAL,
                EffectiveVisibility.Local
            ).apply {
                isExpect = firRegularClass.isExpect
                isActual = firRegularClass.isActual
                isOverride = false
                isOperator = false
                isInfix = false
                isExternal = false
                isInline = false
                isSuspend = false
                isTailRec = false
            }
            this.symbol = symbol
            typeParameters += newTypeParameters.map { it.build() }

            val substitutedFunctionType = substitutor.substituteOrSelf(functionType)
            val substitutedReturnType =
                ConeClassLikeTypeImpl(
                    firRegularClass.symbol.toLookupTag(), newTypeParameterTypes.toTypedArray(), isNullable = false,
                )

            returnTypeRef = buildResolvedTypeRef {
                source = null
                type = substitutedReturnType
            }

            valueParameters += buildValueParameter {
                moduleData = session.moduleData
                origin = FirDeclarationOrigin.SamConstructor
                returnTypeRef = buildResolvedTypeRef {
                    source = firRegularClass.source
                    type = substitutedFunctionType
                }
                name = SAM_PARAMETER_NAME
                this.symbol = FirValueParameterSymbol(SAM_PARAMETER_NAME)
                isCrossinline = false
                isNoinline = false
                isVararg = false
                resolvePhase = FirResolvePhase.BODY_RESOLVE
            }

            annotations += functionSymbol.annotations

            resolvePhase = FirResolvePhase.BODY_RESOLVE
        }.apply {
            containingClassForStaticMemberAttr = outerClassManager?.outerClass(firRegularClass.symbol)?.toLookupTag()
        }.symbol
    }

    private fun resolveFunctionTypeIfSamInterface(firRegularClass: FirRegularClass): SAMInfo<ConeLookupTagBasedType>? {
        return resolvedFunctionType.getOrPut(firRegularClass) {
            if (!firRegularClass.status.isFun) return@getOrPut null
            val abstractMethod = firRegularClass.getSingleAbstractMethodOrNull(session, scopeSession) ?: return@getOrPut null
            // TODO: val shouldConvertFirstParameterToDescriptor = samWithReceiverResolvers.any { it.shouldConvertFirstSamParameterToReceiver(abstractMethod) }

            val typeFromExtension = samConversionTransformers.firstNotNullOfOrNull {
                it.getCustomFunctionalTypeForSamConversion(abstractMethod)
            }

            SAMInfo(abstractMethod.symbol, typeFromExtension ?: abstractMethod.getFunctionTypeForAbstractMethod())
        }
    }

    override fun shouldRunSamConversionForFunction(firFunction: FirFunction): Boolean {
        // TODO: properly support, see org.jetbrains.kotlin.load.java.sam.JvmSamConversionTransformer.shouldRunSamConversionForFunction
        return true
    }
}

private fun FirRegularClass.getSingleAbstractMethodOrNull(
    session: FirSession,
    scopeSession: ScopeSession,
): FirSimpleFunction? {
    // TODO: restrict to Java interfaces
    if (classKind != ClassKind.INTERFACE || hasMoreThenOneAbstractFunctionOrHasAbstractProperty()) return null

    val samCandidateNames = computeSamCandidateNames(session)
    return findSingleAbstractMethodByNames(session, scopeSession, samCandidateNames)
}

private fun FirRegularClass.computeSamCandidateNames(session: FirSession): Set<Name> {
    val classes =
        // Note: we search only for names in this function, so substitution is not needed      V
        lookupSuperTypes(this, lookupInterfaces = true, deep = true, useSiteSession = session, substituteTypes = false)
            .mapNotNullTo(mutableListOf(this)) {
                (session.symbolProvider.getSymbolByLookupTag(it.lookupTag) as? FirRegularClassSymbol)?.fir
            }

    val samCandidateNames = mutableSetOf<Name>()
    for (clazz in classes) {
        for (declaration in clazz.declarations) {
            when (declaration) {
                is FirProperty -> if (declaration.resolvedIsAbstract) {
                    samCandidateNames.add(declaration.name)
                }
                is FirSimpleFunction -> if (declaration.resolvedIsAbstract) {
                    samCandidateNames.add(declaration.name)
                }
                else -> {}
            }
        }
    }

    return samCandidateNames
}

private fun FirRegularClass.findSingleAbstractMethodByNames(
    session: FirSession,
    scopeSession: ScopeSession,
    samCandidateNames: Set<Name>,
): FirSimpleFunction? {
    var resultMethod: FirSimpleFunction? = null
    var metIncorrectMember = false

    val classUseSiteMemberScope = this.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false)

    for (candidateName in samCandidateNames) {
        if (metIncorrectMember) break

        classUseSiteMemberScope.processPropertiesByName(candidateName) {
            if (it is FirPropertySymbol && it.fir.resolvedIsAbstract) {
                metIncorrectMember = true
            }
        }

        if (metIncorrectMember) break

        classUseSiteMemberScope.processFunctionsByName(candidateName) { functionSymbol ->
            val firFunction = functionSymbol.fir
            if (!firFunction.resolvedIsAbstract ||
                firFunction.isPublicInObject(checkOnlyName = false)
            ) return@processFunctionsByName

            if (resultMethod != null) {
                metIncorrectMember = true
            } else {
                resultMethod = firFunction
            }
        }
    }

    if (metIncorrectMember || resultMethod == null || resultMethod!!.typeParameters.isNotEmpty()) return null

    return resultMethod
}

private fun FirRegularClass.hasMoreThenOneAbstractFunctionOrHasAbstractProperty(): Boolean {
    var wasAbstractFunction = false
    for (declaration in declarations) {
        if (declaration is FirProperty && declaration.resolvedIsAbstract) return true
        if (declaration is FirSimpleFunction && declaration.resolvedIsAbstract &&
            !declaration.isPublicInObject(checkOnlyName = true)
        ) {
            if (wasAbstractFunction) return true
            wasAbstractFunction = true
        }
    }

    return false
}

/**
 * Checks if declaration is indeed abstract, ensuring that its status has been completely resolved
 * beforehand.
 */
private val FirCallableDeclaration.resolvedIsAbstract: Boolean
    get() = symbol.isAbstract

// From the definition of function interfaces in the Java specification (pt. 9.8):
// "methods that are members of I that do not have the same signature as any public instance method of the class Object"
// It means that if an interface declares `int hashCode()` then the method won't be taken into account when
// checking if the interface is SAM.
fun FirSimpleFunction.isPublicInObject(checkOnlyName: Boolean): Boolean {
    if (name.asString() !in PUBLIC_METHOD_NAMES_IN_OBJECT) return false
    if (checkOnlyName) return true

    return when (name.asString()) {
        "hashCode", "getClass", "notify", "notifyAll", "toString" -> valueParameters.isEmpty()
        "equals" -> valueParameters.singleOrNull()?.hasTypeOf(StandardClassIds.Any, allowNullable = true) == true
        "wait" -> when (valueParameters.size) {
            0 -> true
            1 -> valueParameters[0].hasTypeOf(StandardClassIds.Long, allowNullable = false)
            2 -> valueParameters[0].hasTypeOf(StandardClassIds.Long, allowNullable = false) &&
                    valueParameters[1].hasTypeOf(StandardClassIds.Int, allowNullable = false)
            else -> false
        }
        else -> error("Unexpected method name: $name")
    }
}

private val PUBLIC_METHOD_NAMES_IN_OBJECT = setOf("equals", "hashCode", "getClass", "wait", "notify", "notifyAll", "toString")

private fun FirSimpleFunction.getFunctionTypeForAbstractMethod(): ConeLookupTagBasedType {
    val parameterTypes = valueParameters.map {
        it.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: ConeErrorType(ConeIntermediateDiagnostic("No type for parameter $it"))
    }

    return createFunctionalType(
        parameterTypes,
        receiverType = receiverTypeRef?.coneType,
        rawReturnType = returnTypeRef.coneType,
        isSuspend = this.isSuspend
    )
}

class FirSamConstructorStorage(session: FirSession) : FirSessionComponent {
    val samConstructors: FirCache<FirRegularClassSymbol, FirNamedFunctionSymbol?, FirSamResolverImpl> =
        session.firCachesFactory.createCache { classSymbol, samResolver -> samResolver.buildSamConstructor(classSymbol) }
}

private val FirSession.samConstructorStorage: FirSamConstructorStorage by FirSession.sessionComponentAccessor()
