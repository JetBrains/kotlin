/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.NullableMap
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getOrPut
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirTypeParameterBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.diagnostics.ConeCannotInferTypeParameterType
import org.jetbrains.kotlin.fir.diagnostics.ConeIntermediateDiagnostic
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.impl.FirFakeOverrideGenerator
import org.jetbrains.kotlin.fir.scopes.impl.TypeAliasConstructorInfo
import org.jetbrains.kotlin.fir.scopes.impl.hasTypeOf
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
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
import org.jetbrains.kotlin.utils.addToStdlib.unreachableBranch
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

private val SAM_PARAMETER_NAME = Name.identifier("function")

data class SAMInfo<out C : ConeKotlinType>(internal val symbol: FirNamedFunctionSymbol, val type: C)

class FirSamResolver(
    private val session: FirSession,
    private val scopeSession: ScopeSession,
    private val outerClassManager: FirOuterClassManager? = null,
) {
    private val resolvedFunctionType: NullableMap<FirRegularClass, SAMInfo<ConeLookupTagBasedType>?> = NullableMap()
    private val samConstructorsCache = session.samConstructorStorage.samConstructors
    private val samConversionTransformers = session.extensionService.samConversionTransformers

    fun isSamType(type: ConeKotlinType): Boolean = when (type) {
        is ConeClassLikeType -> {
            val symbol = type.fullyExpandedType(session).lookupTag.toSymbol(session)
            symbol is FirRegularClassSymbol && resolveFunctionTypeIfSamInterface(symbol.fir) != null
        }

        is ConeFlexibleType -> isSamType(type.lowerBound) && isSamType(type.upperBound)
        else -> false
    }

    /**
     * fun interface Foo {
     *     fun bar(x: Int): String
     * }
     *
     * [functionalType] is `(Int) -> String`
     * [samType] is `Foo`
     */
    data class SamConversionInfo(val functionalType: ConeKotlinType, val samType: ConeKotlinType)

    fun getSamInfoForPossibleSamType(type: ConeKotlinType): SamConversionInfo? {
        return when (type) {
            is ConeClassLikeType -> SamConversionInfo(
                functionalType = getFunctionTypeForPossibleSamType(type.fullyExpandedType(session)) ?: return null,
                samType = type
            )
            is ConeFlexibleType -> {
                val lowerType = getSamInfoForPossibleSamType(type.lowerBound)?.functionalType ?: return null

                if (type.isTrivial) {
                    SamConversionInfo(
                        functionalType = lowerType.lowerBoundIfFlexible().toTrivialFlexibleType(session.typeContext),
                        samType = type
                    )
                } else {
                    val upperType = getSamInfoForPossibleSamType(type.upperBound)?.functionalType ?: return null
                    SamConversionInfo(
                        functionalType = ConeFlexibleType(
                            lowerType.lowerBoundIfFlexible(),
                            upperType.upperBoundIfFlexible(),
                            isTrivial = false,
                        ),
                        samType = type
                    )
                }
            }

            is ConeStubType, is ConeTypeParameterType, is ConeTypeVariableType,
            is ConeDefinitelyNotNullType, is ConeIntersectionType, is ConeIntegerLiteralType,
            -> null

            is ConeCapturedType -> type.lowerType?.let { getSamInfoForPossibleSamType(it) }

            is ConeLookupTagBasedType -> unreachableBranch(type)
        }
    }

    private fun getFunctionTypeForPossibleSamType(type: ConeClassLikeType): ConeLookupTagBasedType? {
        val firRegularClass = type.lookupTag.toRegularClassSymbol(session)?.fir ?: return null

        val (_, unsubstitutedFunctionType) = resolveFunctionTypeIfSamInterface(firRegularClass) ?: return null

        val functionType = firRegularClass.buildSubstitutorWithUpperBounds(session, type)?.substituteOrNull(unsubstitutedFunctionType)
            ?: unsubstitutedFunctionType

        require(functionType is ConeLookupTagBasedType) {
            "Function type should always be ConeLookupTagBasedType, but ${functionType::class} was found"
        }
        return functionType.withNullabilityOf(type, session.typeContext)
    }

    fun getSamConstructor(firClassOrTypeAlias: FirClassLikeDeclaration): FirSimpleFunction? {
        if (firClassOrTypeAlias is FirTypeAlias) {
            // Precompute the constructor for the base type to avoid deadlocks in the IDE.
            firClassOrTypeAlias.symbol.resolvedExpandedTypeRef.coneTypeSafe<ConeClassLikeType>()
                ?.fullyExpandedType(session)?.lookupTag?.toSymbol(session)
                ?.let { samConstructorsCache.getValue(it, this) }
        }

        return samConstructorsCache.getValue(firClassOrTypeAlias.symbol, this)?.fir
    }

    fun buildSamConstructorForRegularClass(classSymbol: FirRegularClassSymbol): FirNamedFunctionSymbol? {
        val firRegularClass = classSymbol.fir
        val (functionSymbol, functionType) = resolveFunctionTypeIfSamInterface(firRegularClass) ?: return null

        val syntheticFunctionSymbol = classSymbol.createSyntheticConstructorSymbol()

        val newTypeParameters = firRegularClass.typeParameters.map { typeParameter ->
            val declaredTypeParameter = typeParameter.symbol.fir
            FirTypeParameterBuilder().apply {
                source = declaredTypeParameter.source?.fakeElement(KtFakeSourceElementKind.SamConstructor)
                moduleData = session.moduleData
                origin = FirDeclarationOrigin.SamConstructor
                resolvePhase = FirResolvePhase.DECLARATIONS
                name = declaredTypeParameter.name
                this.symbol = FirTypeParameterSymbol()
                variance = Variance.INVARIANT
                isReified = false
                annotations += declaredTypeParameter.annotations
                containingDeclarationSymbol = syntheticFunctionSymbol
            }
        }

        val newTypeParameterTypes =
            newTypeParameters
                .map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), isMarkedNullable = false) }

        val substitutor = substitutorByMap(
            firRegularClass.typeParameters
                .map { it.symbol }
                .zip(newTypeParameterTypes).toMap(),
            session
        )

        for ((newTypeParameter, oldTypeParameter) in newTypeParameters.zip(firRegularClass.typeParameters)) {
            val declared = oldTypeParameter.symbol.fir
            newTypeParameter.bounds += declared.symbol.resolvedBounds.map { typeRef ->
                buildResolvedTypeRef {
                    source = typeRef.source?.fakeElement(KtFakeSourceElementKind.SamConstructor)
                    coneType = substitutor.substituteOrSelf(typeRef.coneType)
                }
            }
        }

        val fakeSource = firRegularClass.source?.fakeElement(KtFakeSourceElementKind.SamConstructor)

        return buildSimpleFunction {
            moduleData = session.moduleData
            source = fakeSource
            name = syntheticFunctionSymbol.name
            origin = FirDeclarationOrigin.SamConstructor
            status = firRegularClass.status.copy(modality = Modality.FINAL)
            this.symbol = syntheticFunctionSymbol
            typeParameters += newTypeParameters.map { it.build() }

            val substitutedFunctionType = substitutor.substituteOrSelf(functionType)
            val substitutedReturnType =
                ConeClassLikeTypeImpl(
                    firRegularClass.symbol.toLookupTag(), newTypeParameterTypes.toTypedArray(), isMarkedNullable = false,
                )

            returnTypeRef = buildResolvedTypeRef {
                source = fakeSource
                coneType = substitutedReturnType
            }

            valueParameters += buildValueParameter {
                moduleData = session.moduleData
                containingDeclarationSymbol = syntheticFunctionSymbol
                origin = FirDeclarationOrigin.SamConstructor
                returnTypeRef = buildResolvedTypeRef {
                    source = fakeSource
                    coneType = substitutedFunctionType
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

    fun buildSamConstructorForTypeAlias(typeAliasSymbol: FirTypeAliasSymbol): FirNamedFunctionSymbol? {
        val type =
            typeAliasSymbol.fir.expandedTypeRef.coneTypeUnsafe<ConeClassLikeType>().fullyExpandedType(session)

        val expansionRegularClass = type.lookupTag.toRegularClassSymbol(session)?.fir ?: return null
        val samConstructorForClass = getSamConstructor(expansionRegularClass) ?: return null

        // The constructor is something like `fun <T, ...> C(...): C<T, ...>`, meaning the type parameters
        // we need to replace are owned by it, not by the class (see the substitutor in `buildSamConstructor`
        // for `FirRegularClass` above).
        val substitutor = samConstructorForClass.buildSubstitutorWithUpperBounds(session, type)
        val newParameterTypes = samConstructorForClass.valueParameters.map {
            substitutor.substituteOrSelf(it.returnTypeRef.coneType)
        }
        val newContextParameterTypes = samConstructorForClass.contextParameters.map {
            substitutor.substituteOrSelf(it.returnTypeRef.coneType)
        }

        return FirFakeOverrideGenerator.createCopyForFirFunction(
            typeAliasSymbol.createSyntheticConstructorSymbol(), samConstructorForClass,
            derivedClassLookupTag = null,
            session, FirDeclarationOrigin.SamConstructor,
            newDispatchReceiverType = null,
            newReceiverType = null,
            newContextParameterTypes = newContextParameterTypes,
            newReturnType = type.withAbbreviation(AbbreviatedTypeAttribute(typeAliasSymbol.defaultType())),
            newParameterTypes = newParameterTypes,
            newTypeParameters = typeAliasSymbol.fir.typeParameters,
            markAsOverride = true
        ).also {
            it.typeAliasConstructorInfo = TypeAliasConstructorInfo(
                samConstructorForClass,
                typeAliasSymbol,
                substitutor = null,
            )
        }.symbol
    }

    private fun FirClassLikeSymbol<*>.createSyntheticConstructorSymbol() =
        FirSyntheticFunctionSymbol(
            CallableId(
                classId.packageFqName,
                classId.relativeClassName.parent().takeIf { !it.isRoot },
                classId.shortClassName,
            ),
        )

    private fun resolveFunctionTypeIfSamInterface(firRegularClass: FirRegularClass): SAMInfo<ConeLookupTagBasedType>? {
        return resolvedFunctionType.getOrPut(firRegularClass) {
            if (!firRegularClass.status.isFun) return@getOrPut null
            val abstractMethod = firRegularClass.getSingleAbstractMethodOrNull(session, scopeSession) ?: return@getOrPut null

            val typeFromExtension = samConversionTransformers.firstNotNullOfOrNull {
                it.getCustomFunctionTypeForSamConversion(abstractMethod)
            }

            SAMInfo(abstractMethod.symbol, typeFromExtension ?: abstractMethod.getFunctionTypeForAbstractMethod(session))
        }
    }

}

/**
 * This function creates a substitutor for SAM class/SAM constructor based on the expected SAM type.
 * If there is a typeless projection in some argument of the expected type then the upper bound of the corresponding type parameters is used
 *
 * If type 'samType' contains no projection, then it's non-projection parametrization is 'samType' itself
 * Else each projection type argument 'out/in A_i' (but star projections) is replaced with it's bound 'A_i'
 * Star projections are treated specially:
 * - If first upper bound of corresponding type parameter does not contain any type parameter of 'samType' class,
 *   then use this upper bound instead of star projection
 * - Otherwise no non-projection parametrization exists for such 'samType'
 *
 * See Non-wildcard parametrization in JLS 8 p.9.9 for clarification
 */
private fun FirTypeParameterRefsOwner.buildSubstitutorWithUpperBounds(session: FirSession, type: ConeClassLikeType): ConeSubstitutor {
    if (typeParameters.isEmpty()) return ConeSubstitutor.Empty

    var containsNonSubstitutedArguments = false

    fun createMapping(substitutor: ConeSubstitutor): Map<FirTypeParameterSymbol, ConeKotlinType> {
        return typeParameters.zip(type.typeArguments).associate { (parameter, projection) ->
            val typeArgument =
                projection.type?.let(substitutor::substituteOrSelf)
                // TODO: Consider using `parameterSymbol.fir.bounds.first().coneType` once sure that it won't fail with exception
                    ?: parameter.symbol.fir.bounds.firstOrNull()?.coneTypeOrNull
                        ?.let(substitutor::substituteOrSelf)
                        ?.also { bound ->
                            // We only check for type parameters in upper bounds
                            // because `projection` can contain a type parameter type as well in a situation like
                            // fun interface Sam<T> {
                            //      fun invoke()
                            //      fun foo(s: Sam<T>) {} <--- here T is substituted with T but it's not a recursion
                            // }
                            if (bound.containsReferenceToOtherTypeParameter(this)) {
                                containsNonSubstitutedArguments = true
                            }
                        }
                    ?: session.builtinTypes.nullableAnyType.coneType
            Pair(parameter.symbol, typeArgument)
        }
    }

    /*
     *
     * There might be a case when there is a recursion in upper bounds of SAM type parameters:
     *
     * ```
     * public interface Function<E extends CharSequence, F extends Map<String, E>> {
     *     E handle(F f);
     * }
     * ```
     *
     * In this case, it's not enough to just take the upper bound of the parameter, as it may contain the reference to another parameter.
     * To handle it correctly, we need to substitute upper bounds with existing substitutor too.
     * This recursive substitution process may last at most as the number of presented type parameters
     */
    var substitutor: ConeSubstitutor = ConeSubstitutor.Empty

    for (i in typeParameters.indices) {
        containsNonSubstitutedArguments = false
        val mapping = createMapping(substitutor)
        substitutor = substitutorByMap(mapping, session)
        if (!containsNonSubstitutedArguments) {
            break
        }
    }

    /*
    * If there are still unsubstituted
     *   parameters, then it means that there is a cycle in parameters themselves and it's impossible to infer proper substitution. For that
     *   case we just create error types for such parameters
     *
     * ```
     * public interface Function1<A extends B, B extends A> {
     *     B handle(A a);
     * }
     * ```
     */
    if (containsNonSubstitutedArguments) {
        val errorSubstitution = typeParameters.associate {
            val diagnostic = ConeCannotInferTypeParameterType(
                it.symbol,
                reason = "Parameter ${it.symbol.name} has a cycle in its upper bounds",
            )
            it.symbol to ConeErrorType(diagnostic)
        }
        val errorSubstitutor = substitutorByMap(errorSubstitution, session)
        substitutor = substitutorByMap(createMapping(errorSubstitutor), session)
    }

    return substitutor
}

private fun ConeKotlinType.containsReferenceToOtherTypeParameter(owner: FirTypeParameterRefsOwner): Boolean {
    return contains { type ->
        type is ConeTypeParameterType && owner.typeParameters.any { it.symbol == type.lookupTag.typeParameterSymbol }
    }
}

private fun FirRegularClass.getSingleAbstractMethodOrNull(
    session: FirSession,
    scopeSession: ScopeSession,
): FirSimpleFunction? {
    if (classKind != ClassKind.INTERFACE || hasMoreThenOneAbstractFunctionOrHasAbstractProperty()) return null

    val samCandidateNames = computeSamCandidateNames(session)
    return findSingleAbstractMethodByNames(session, scopeSession, samCandidateNames)
}

private fun FirRegularClass.computeSamCandidateNames(session: FirSession): Set<Name> {
    val classes =
        // Note: we search only for names in this function, so substitution is not needed      V
        lookupSuperTypes(this, lookupInterfaces = true, deep = true, useSiteSession = session, substituteTypes = false)
            .mapNotNullTo(mutableListOf(this)) {
                (it.lookupTag.toRegularClassSymbol(session))?.fir
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

    val classUseSiteMemberScope = this.unsubstitutedScope(
        session,
        scopeSession,
        withForcedTypeCalculator = false,
        memberRequiredPhase = null,
    )

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

/**
 * From the definition of function interfaces in the Java specification (pt. 9.8):
 * "methods that are members of I that do not have the same signature as any public instance method of the class Object"
 * It means that if an interface declares `int hashCode()` then the method won't be taken into account when
 * checking if the interface is SAM.
 *
 * For K1 compatibility, this only applies to members declared in Java, see KT-67283.
 */
private fun FirSimpleFunction.isPublicInObject(checkOnlyName: Boolean): Boolean {
    if (!isJavaOrEnhancement) return false
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
        else -> errorWithAttachment("Unexpected method name") {
            withEntry("methodName", name.asString())
        }
    }
}

private val PUBLIC_METHOD_NAMES_IN_OBJECT = setOf("equals", "hashCode", "getClass", "wait", "notify", "notifyAll", "toString")

private fun FirSimpleFunction.getFunctionTypeForAbstractMethod(session: FirSession): ConeLookupTagBasedType {
    val parameterTypes = valueParameters.map {
        it.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: ConeErrorType(ConeIntermediateDiagnostic("No type for parameter $it"))
    }
    val contextParameterTypes = contextParameters.map {
        it.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: ConeErrorType(ConeIntermediateDiagnostic("No type for context receiver $it"))
    }
    val kind = session.functionTypeService.extractSingleSpecialKindForFunction(symbol) ?: FunctionTypeKind.Function
    return createFunctionType(
        kind,
        parameterTypes,
        receiverType = receiverParameter?.typeRef?.coneType,
        rawReturnType = returnTypeRef.coneType,
        contextParameters = contextParameterTypes
    )
}

class FirSamConstructorStorage(session: FirSession) : FirSessionComponent {
    val samConstructors: FirCache<FirClassLikeSymbol<*>, FirNamedFunctionSymbol?, FirSamResolver> =
        session.firCachesFactory.createCache { classSymbol, samResolver ->
            when (classSymbol) {
                is FirRegularClassSymbol -> samResolver.buildSamConstructorForRegularClass(classSymbol)
                is FirTypeAliasSymbol -> samResolver.buildSamConstructorForTypeAlias(classSymbol)
                is FirAnonymousObjectSymbol -> null
            }
        }
}

private val FirSession.samConstructorStorage: FirSamConstructorStorage by FirSession.sessionComponentAccessor()
