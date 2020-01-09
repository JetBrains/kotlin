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
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirSimpleFunctionImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirTypeParameterImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.inferenceContext
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.scope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

interface FirSamResolver : FirSessionComponent {
    fun getFunctionTypeForPossibleSamType(type: ConeKotlinType): ConeKotlinType?
    fun shouldRunSamConversionForFunction(firNamedFunction: FirSimpleFunction): Boolean
    fun getSamConstructor(firRegularClass: FirRegularClass): FirSimpleFunction?
}

private val NULL_STUB = Any()
val SAM_PARAMETER_NAME = Name.identifier("block")

class FirSamResolverImpl(
    private val firSession: FirSession,
    private val scopeSession: ScopeSession
) : FirSamResolver {

    private val resolvedFunctionType: MutableMap<FirRegularClass, Any> = mutableMapOf()
    private val samConstructor: MutableMap<FirRegularClass, Any> = mutableMapOf()

    override fun getFunctionTypeForPossibleSamType(type: ConeKotlinType): ConeKotlinType? {
        return when (type) {
            is ConeClassLikeType -> getFunctionTypeForPossibleSamType(type)
            is ConeFlexibleType -> ConeFlexibleType(
                getFunctionTypeForPossibleSamType(type.lowerBound) ?: return null,
                getFunctionTypeForPossibleSamType(type.upperBound) ?: return null
            )
            is ConeClassErrorType, is ConeStubType -> null
            // TODO: support those types as well
            is ConeTypeParameterType, is ConeTypeVariableType,
            is ConeCapturedType, is ConeDefinitelyNotNullType, is ConeIntersectionType,
            is ConeIntegerLiteralType -> null
            // TODO: Thing of getting rid of this branch since ConeLookupTagBasedType should be a sealed class
            is ConeLookupTagBasedType -> null
        }
    }

    private fun getFunctionTypeForPossibleSamType(type: ConeClassLikeType): ConeLookupTagBasedType? {
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
                            ?: firSession.builtinTypes.nullableAnyType.type
                        //ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(StandardClassIds.Any), emptyArray(), isNullable = true)
                    })
                    .toMap()
            )

        val result =
            substitutor
                .substituteOrSelf(unsubstitutedFunctionType)
                .withNullability(ConeNullability.create(type.isMarkedNullable), firSession.inferenceContext)

        require(result is ConeLookupTagBasedType) {
            "Function type should always be ConeLookupTagBasedType, but ${result::class} was found"
        }

        return result
    }

    override fun getSamConstructor(firRegularClass: FirRegularClass): FirSimpleFunction? {
        return samConstructor.getOrPut(firRegularClass) {
            buildSamConstructor(firRegularClass) ?: return@getOrPut NULL_STUB
        } as? FirSimpleFunction
    }

    private fun buildSamConstructor(firRegularClass: FirRegularClass): FirSimpleFunction? {
        val functionType = resolveFunctionTypeIfSamInterface(firRegularClass) ?: return null

        val classId = firRegularClass.classId
        val symbol = FirSyntheticFunctionSymbol(
            CallableId(
                classId.packageFqName,
                classId.relativeClassName.parent().takeIf { !it.isRoot },
                classId.shortClassName
            )
        )

        val newTypeParameters = firRegularClass.typeParameters.map { typeParameter ->
            FirTypeParameterImpl(
                typeParameter.source,
                firSession,
                typeParameter.name,
                FirTypeParameterSymbol(),
                Variance.INVARIANT,
                isReified = false
            ).apply {
                annotations += typeParameter.annotations
            }
        }

        val newTypeParameterTypes =
            newTypeParameters
                .map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), isNullable = false) }

        val substitutor = substitutorByMap(
            firRegularClass.typeParameters
                .map { it.symbol }
                .zip(newTypeParameterTypes).toMap()
        )

        for ((newTypeParameter, oldTypeParameter) in newTypeParameters.zip(firRegularClass.typeParameters)) {
            newTypeParameter.bounds += oldTypeParameter.bounds.mapNotNull { typeRef ->
                FirResolvedTypeRefImpl(
                    typeRef.source,
                    substitutor.substituteOrSelf(typeRef.coneTypeSafe() ?: return@mapNotNull null)
                )
            }
        }

        val substitutedFunctionType = substitutor.substituteOrSelf(functionType)
        val substitutedReturnType =
            ConeClassLikeTypeImpl(
                firRegularClass.symbol.toLookupTag(), newTypeParameterTypes.toTypedArray(), isNullable = false
            )

        val status = FirDeclarationStatusImpl(firRegularClass.visibility, Modality.FINAL).apply {
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

        return FirSimpleFunctionImpl(
            null,
            firSession,
            FirResolvedTypeRefImpl(null, substitutedReturnType),
            null,
            classId.shortClassName,
            status,
            symbol
        ).apply {
            valueParameters += listOf(
                FirValueParameterImpl(
                    source,
                    session,
                    FirResolvedTypeRefImpl(firRegularClass.source, substitutedFunctionType),
                    SAM_PARAMETER_NAME,
                    FirVariableSymbol(SAM_PARAMETER_NAME),
                    defaultValue = null,
                    isCrossinline = false,
                    isNoinline = false,
                    isVararg = false
                )
            )
            typeParameters += newTypeParameters
            resolvePhase = FirResolvePhase.BODY_RESOLVE
        }
    }

    private fun resolveFunctionTypeIfSamInterface(firRegularClass: FirRegularClass): ConeKotlinType? {
        return resolvedFunctionType.getOrPut(firRegularClass) {
            val abstractMethod = firRegularClass.getSingleAbstractMethodOrNull(firSession, scopeSession) ?: return@getOrPut NULL_STUB
            // TODO: val shouldConvertFirstParameterToDescriptor = samWithReceiverResolvers.any { it.shouldConvertFirstSamParameterToReceiver(abstractMethod) }

            abstractMethod.getFunctionTypeForAbstractMethod()
        } as? ConeKotlinType
    }

    override fun shouldRunSamConversionForFunction(firNamedFunction: FirSimpleFunction): Boolean {
        // TODO: properly support, see org.jetbrains.kotlin.load.java.sam.JvmSamConversionTransformer.shouldRunSamConversionForFunction
        return true
    }
}

private fun FirRegularClass.getSingleAbstractMethodOrNull(
    session: FirSession,
    scopeSession: ScopeSession
): FirSimpleFunction? {
    // TODO: restrict to Java interfaces
    if (classKind != ClassKind.INTERFACE || hasMoreThenOneAbstractFunctionOrHasAbstractProperty()) return null

    val samCandidateNames = computeSamCandidateNames(session)

    return findSingleAbstractMethodByNames(session, scopeSession, samCandidateNames)
}

private fun FirRegularClass.computeSamCandidateNames(session: FirSession): Set<Name> {
    val classes =
        lookupSuperTypes(this, lookupInterfaces = true, deep = true, useSiteSession = session)
            .mapNotNullTo(mutableListOf(this)) {
                (session.firSymbolProvider.getSymbolByLookupTag(it.lookupTag) as? FirRegularClassSymbol)?.fir
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
): FirSimpleFunction? {
    var resultMethod: FirSimpleFunction? = null
    var metIncorrectMember = false

    val classUseSiteMemberScope = this.scope(ConeSubstitutor.Empty, session, scopeSession)

    for (candidateName in samCandidateNames) {
        if (metIncorrectMember) break

        classUseSiteMemberScope.processPropertiesByName(candidateName) {
            if ((it as? FirPropertySymbol)?.fir?.modality == Modality.ABSTRACT) {
                metIncorrectMember = true
            }
        }

        if (metIncorrectMember) break

        classUseSiteMemberScope.processFunctionsByName(candidateName) { functionSymbol ->
            val firFunction = functionSymbol.fir
            require(firFunction is FirSimpleFunction) {
                "${functionSymbol.callableId
                    .callableName} is expected to be _root_ide_package_.org.jetbrains.kotlin.fir.declarations.FirSimpleFunction, but ${functionSymbol::class} was found"
            }

            if (firFunction.modality != Modality.ABSTRACT || firFunction
                    .isPublicInObject(checkOnlyName = false)
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
        if (declaration is FirProperty && declaration.modality == Modality.ABSTRACT) return true
        if (declaration is FirSimpleFunction && declaration.modality == Modality.ABSTRACT &&
            !declaration.isPublicInObject(checkOnlyName = true)
        ) {
            if (wasAbstractFunction) return true
            wasAbstractFunction = true
        }
    }

    return false
}

// From the definition of function interfaces in the Java specification (pt. 9.8):
// "methods that are members of I that do not have the same signature as any public instance method of the class Object"
// It means that if an interface declares `int hashCode()` then the method won't be taken into account when
// checking if the interface is SAM.
private fun FirSimpleFunction.isPublicInObject(checkOnlyName: Boolean): Boolean {
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

private fun FirValueParameter.hasTypeOf(classId: ClassId, allowNullable: Boolean): Boolean {
    val type = returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: return false

    val classLike = when (type) {
        is ConeClassLikeType -> type
        is ConeFlexibleType -> type.upperBound as? ConeClassLikeType ?: return false
        else -> return false
    }

    if (classLike.isMarkedNullable && !allowNullable) return false
    return classLike.lookupTag.classId == classId
}

private val PUBLIC_METHOD_NAMES_IN_OBJECT = setOf("equals", "hashCode", "getClass", "wait", "notify", "notifyAll", "toString")

private fun FirSimpleFunction.getFunctionTypeForAbstractMethod(): ConeLookupTagBasedType {
    val parameterTypes = valueParameters.map {
        it.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: ConeKotlinErrorType("No type for parameter $it")
    }

    return createFunctionalType(
        parameterTypes, receiverType = null,
        rawReturnType = returnTypeRef.coneTypeSafe() ?: ConeKotlinErrorType("No type for return type of $this")
    )
}
