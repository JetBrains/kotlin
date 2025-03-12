/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirTypeCandidateCollector.TypeResolutionResult
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.impl.FirDefaultStarImportingScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability

@ThreadSafeMutableState
class FirTypeResolverImpl(private val session: FirSession) : FirTypeResolver() {
    private val aliasedTypeExpansionGloballyDisabled: Boolean =
        !session.languageVersionSettings.getFlag(AnalysisFlags.expandTypeAliasesInTypeResolution)

    private fun resolveSymbol(
        symbol: FirBasedSymbol<*>,
        remainingQualifier: List<FirQualifierPart>,
        qualifierResolver: FirQualifierResolver,
    ): FirBasedSymbol<*>? {
        return when (symbol) {
            is FirClassLikeSymbol<*> -> {
                if (remainingQualifier.isEmpty()) {
                    symbol
                } else {
                    resolveLocalClassChain(symbol, remainingQualifier)
                        ?: qualifierResolver.resolveSymbolWithPrefix(symbol.classId, remainingQualifier)
                        ?: qualifierResolver.resolveEnumEntrySymbol(symbol.classId, remainingQualifier)
                }
            }
            is FirTypeParameterSymbol -> symbol.takeIf { remainingQualifier.isEmpty() }
            else -> error("!")
        }
    }

    private fun resolveUserTypeToSymbol(
        typeRef: FirUserTypeRef,
        configuration: TypeResolutionConfiguration,
        supertypeSupplier: SupertypeSupplier,
        resolveDeprecations: Boolean
    ): TypeResolutionResult {
        session.lookupTracker?.recordUserTypeRefLookup(
            typeRef, configuration.scopes.flatMap { it.scopeOwnerLookupNames }, configuration.useSiteFile?.source
        )

        val qualifier = typeRef.qualifier
        val qualifierResolver = session.qualifierResolver
        val collector = FirTypeCandidateCollector(
            session,
            configuration.useSiteFile,
            configuration.containingClassDeclarations,
            supertypeSupplier,
            resolveDeprecations
        )

        if (configuration.sealedClassForContextSensitiveResolution != null) {
            val resolvedSymbol = resolveSymbol(configuration.sealedClassForContextSensitiveResolution, qualifier, qualifierResolver)

            if (resolvedSymbol is FirRegularClassSymbol
                && resolvedSymbol.fir.typeParameters.firstOrNull() !is FirOuterClassTypeParameterRef
                // Only sealed subclasses are allowed
                && resolvedSymbol.fir.isSubclassOf(
                    configuration.sealedClassForContextSensitiveResolution.toLookupTag(), session, isStrict = true
                )
            ) {
                collector.processCandidate(
                    resolvedSymbol,
                    // We don't allow inner classes capturing outer type parameters
                    ConeSubstitutor.Empty,
                )
            }

            // We need/expect no scopes for context-sensitive resolution
            // See TypeResolutionConfiguration.Companion.createForContextSensitiveResolution
            check(!configuration.scopes.iterator().hasNext())

            return collector.getResult()
        }

        for (scope in configuration.scopes) {
            if (collector.applicability == CandidateApplicability.RESOLVED) break
            val name = qualifier.first().name
            val processor = { symbol: FirClassifierSymbol<*>, substitutorFromScope: ConeSubstitutor ->
                val resolvedSymbol = resolveSymbol(symbol, qualifier.subList(1, qualifier.size), qualifierResolver)

                if (resolvedSymbol != null) {
                    collector.processCandidate(resolvedSymbol, substitutorFromScope)
                }
            }

            if (scope is FirDefaultStarImportingScope) {
                scope.processClassifiersByNameWithSubstitutionFromBothLevelsConditionally(name) { symbol, substitutor ->
                    processor(symbol, substitutor)
                    collector.applicability == CandidateApplicability.RESOLVED
                }
            } else {
                scope.processClassifiersByNameWithSubstitution(name, processor)
            }
        }

        if (collector.applicability != CandidateApplicability.RESOLVED) {
            val symbol = qualifierResolver.resolveFullyQualifiedSymbol(qualifier)
            if (symbol != null) {
                collector.processCandidate(symbol, null)
            }
        }

        return collector.getResult()
    }

    private fun resolveLocalClassChain(symbol: FirClassLikeSymbol<*>, remainingQualifier: List<FirQualifierPart>): FirRegularClassSymbol? {
        if (symbol !is FirRegularClassSymbol || !symbol.isLocal) {
            return null
        }

        fun resolveLocalClassChain(classSymbol: FirRegularClassSymbol, qualifierIndex: Int): FirRegularClassSymbol? {
            if (qualifierIndex == remainingQualifier.size) {
                return classSymbol
            }

            val qualifierName = remainingQualifier[qualifierIndex].name
            for (declarationSymbol in classSymbol.declarationSymbols) {
                if (declarationSymbol is FirRegularClassSymbol) {
                    if (declarationSymbol.toLookupTag().name == qualifierName) {
                        return resolveLocalClassChain(declarationSymbol, qualifierIndex + 1)
                    }
                }
            }

            return null
        }

        return resolveLocalClassChain(symbol, 0)
    }

    @OptIn(SymbolInternals::class)
    private fun FirQualifierResolver.resolveEnumEntrySymbol(
        classId: ClassId,
        remainingQualifier: List<FirQualifierPart>,
    ): FirVariableSymbol<FirEnumEntry>? {
        // Assuming the current qualifier refers to an enum entry, we drop the last part so we get a reference to the enum class.
        val enumClassSymbol = resolveSymbolWithPrefix(classId, remainingQualifier.dropLast(1)) ?: return null
        val enumClassFir = enumClassSymbol.fir as? FirRegularClass ?: return null
        if (!enumClassFir.isEnumClass) return null
        val enumEntryMatchingLastQualifier = enumClassFir.declarations
            .firstOrNull { it is FirEnumEntry && it.name == remainingQualifier.last().name } as? FirEnumEntry
        return enumEntryMatchingLastQualifier?.symbol
    }

    /**
     * @return ConeErrorType only for completely unresolved symbols or ambiguity or type argument mapping problems
     * @return regular ConeLookupTagBasedType if resolution is successful or a single erroneous candidate was found
     *
     * Thus, the visibility error should be handled further by just looking into TypeResolutionResult again
     */
    @OptIn(SymbolInternals::class)
    private fun resolveUserType(
        typeRef: FirUserTypeRef,
        result: TypeResolutionResult,
        areBareTypesAllowed: Boolean,
        topContainer: FirDeclaration?,
        isOperandOfIsOperator: Boolean
    ): ConeKotlinType {
        val (symbol, substitutor) = when (result) {
            is TypeResolutionResult.Resolved -> {
                result.typeCandidate.symbol to result.typeCandidate.substitutor
            }
            is TypeResolutionResult.Ambiguity -> null to null
            TypeResolutionResult.Unresolved -> null to null
        }

        val qualifier = typeRef.qualifier
        val allTypeArguments =
            qualifier.reversed().flatMap { it.typeArgumentList.typeArguments }.mapTo(mutableListOf()) { it.toConeTypeProjection() }

        if (symbol is FirClassLikeSymbol<*> && !isPossibleBareType(areBareTypesAllowed, allTypeArguments)) {
            matchQualifierPartsAndClasses(symbol, qualifier)?.let { return ConeErrorType(it) }
            allTypeArguments.addImplicitTypeArgumentsOrReturnError(symbol, topContainer, substitutor)
                ?.let { return ConeErrorType(it) }
        }

        val resultingArguments = allTypeArguments.toTypedArray()

        if (symbol == null || symbol !is FirClassifierSymbol<*>) {
            val diagnostic = when {
                symbol?.fir is FirEnumEntry -> {
                    if (isOperandOfIsOperator) {
                        ConeSimpleDiagnostic("'is' operator can not be applied to an enum entry.", DiagnosticKind.IsEnumEntry)
                    } else {
                        ConeSimpleDiagnostic("An enum entry should not be used as a type.", DiagnosticKind.EnumEntryAsType)
                    }
                }
                result is TypeResolutionResult.Ambiguity -> {
                    ConeAmbiguityError(typeRef.qualifier.last().name, result.typeCandidates.first().applicability, result.typeCandidates)
                }
                else -> {
                    ConeUnresolvedTypeQualifierError(typeRef.qualifier, isNullable = typeRef.isMarkedNullable)
                }
            }
            return ConeErrorType(
                diagnostic,
                typeArguments = resultingArguments,
                attributes = typeRef.annotations.computeTypeAttributes(session, shouldExpandTypeAliases = true)
            )
        }

        if (symbol is FirTypeParameterSymbol) {
            for (part in typeRef.qualifier) {
                if (part.typeArgumentList.typeArguments.isNotEmpty()) {
                    return ConeErrorType(
                        ConeUnexpectedTypeArgumentsError("Type arguments not allowed for type parameters", part.typeArgumentList.source),
                        typeArguments = resultingArguments
                    )
                }
            }
        }

        return symbol.constructType(
            resultingArguments,
            typeRef.isMarkedNullable,
            typeRef.annotations.computeTypeAttributes(
                session,
                shouldExpandTypeAliases = true,
                allowExtensionFunctionType = (symbol.toLookupTag() as? ConeClassLikeLookupTag)?.isSomeFunctionType(session) == true,
            )
        ).also {
            val lookupTag = it.lookupTag
            if (lookupTag is ConeClassLikeLookupTagImpl && symbol is FirClassLikeSymbol<*>) {
                @OptIn(LookupTagInternals::class)
                lookupTag.bindSymbolToLookupTag(session, symbol)
            }
        }
    }

    private fun isPossibleBareType(areBareTypesAllowed: Boolean, allTypeArguments: List<ConeTypeProjection>): Boolean =
        areBareTypesAllowed && allTypeArguments.isEmpty()

    private fun matchQualifierPartsAndClasses(symbol: FirClassLikeSymbol<*>, qualifier: List<FirQualifierPart>): ConeDiagnostic? {
        var currentDeclaration: FirClassLikeDeclaration? = symbol.fir
        var areTypeArgumentsAllowed = true

        for (qualifierPart in qualifier.asReversed()) {
            val typeArgumentList = qualifierPart.typeArgumentList
            val qualifierPartArgsCount = typeArgumentList.typeArguments.size

            if (currentDeclaration == null) {
                // It's a package name
                if (qualifierPartArgsCount > 0) {
                    return ConeTypeArgumentsNotAllowedOnPackageError(typeArgumentList.source!!)
                }
                break
            }

            val desiredTypeParametersCount = currentDeclaration.typeParameters.count { it !is FirOuterClassTypeParameterRef }
            if (areTypeArgumentsAllowed) {
                if (desiredTypeParametersCount != qualifierPartArgsCount) {
                    val source = if (qualifierPartArgsCount == 0) qualifierPart.source else typeArgumentList.source
                    return ConeWrongNumberOfTypeArgumentsError(desiredTypeParametersCount, currentDeclaration.symbol, source!!)
                }
            } else if (qualifierPartArgsCount > 0) {
                return ConeTypeArgumentsForOuterClassWhenNestedReferencedError(typeArgumentList.source!!)
            }

            // Inner class can't contain non-inner class
            // No more arguments are allowed after first static/non-inner class
            areTypeArgumentsAllowed = currentDeclaration.isInner
            currentDeclaration = currentDeclaration.getContainingDeclaration(session)
        }

        return null
    }

    private fun MutableList<ConeTypeProjection>.addImplicitTypeArgumentsOrReturnError(
        symbol: FirClassLikeSymbol<*>,
        topContainer: FirDeclaration?,
        substitutor: ConeSubstitutor?,
    ): ConeDiagnostic? {
        // substitutor is used for checking if all implicit type arguments are defined in outer classes. Consider the following example:
        //
        // class A<T> {
        //    inner class B
        //    val x: B? = null // substitutor returns not null for implicit T, hence there is no error, FQN is not required
        //    class Nested {
        //        val y: B? = null // substitutor returns null for implicit T, hence OUTER_CLASS_ARGUMENTS_REQUIRED is reported here.
        //                         // To fix the problem, the FQN should be used, for instance: val y: A<String>.B? = null
        //    }
        //}
        val explicitTypeArgumentsNumber = size
        for ((typeParameterIndex, typeParameter) in symbol.fir.typeParameters.withIndex()) {
            if (typeParameterIndex < explicitTypeArgumentsNumber) {
                // Ignore explicit type parameters since only outer type parameters are relevant
                continue
            }

            if (typeParameter !is FirOuterClassTypeParameterRef
                || isValidTypeParameterFromOuterDeclaration(typeParameter.symbol, topContainer, session)
            ) {
                val substituted = substitutor?.substituteOrNull(typeParameter.symbol.defaultType)
                if (substituted == null) {
                    return ConeOuterClassArgumentsRequired(typeParameter.symbol.containingDeclarationSymbol as FirClassLikeSymbol<*>)
                } else {
                    add(substituted)
                }
            } else {
                return ConeOuterClassArgumentsRequired(typeParameter.symbol.containingDeclarationSymbol as FirClassLikeSymbol<*>)
            }
        }

        return null
    }

    private fun createFunctionType(typeRef: FirFunctionTypeRef): FirTypeResolutionResult {
        val parameters =
            typeRef.contextParameterTypeRefs.map { it.coneType } +
                    listOfNotNull(typeRef.receiverTypeRef?.coneType) +
                    typeRef.parameters.map { it.returnTypeRef.coneType.withParameterNameAnnotation(it) } +
                    listOf(typeRef.returnTypeRef.coneType)
        val functionKinds = session.functionTypeService.extractAllSpecialKindsForFunctionTypeRef(typeRef)
        var diagnostic: ConeDiagnostic? = null
        val kind = when (functionKinds.size) {
            0 -> FunctionTypeKind.Function
            1 -> functionKinds.single()
            else -> {
                diagnostic = ConeAmbiguousFunctionTypeKinds(functionKinds)
                FunctionTypeKind.Function
            }
        }

        val classId = kind.numberedClassId(typeRef.parametersCount)

        val attributes = typeRef.annotations.computeTypeAttributes(
            session,
            predefined = buildList {
                if (typeRef.receiverTypeRef != null) {
                    add(CompilerConeAttributes.ExtensionFunctionType)
                }

                if (typeRef.contextParameterTypeRefs.isNotEmpty()) {
                    add(CompilerConeAttributes.ContextFunctionTypeParams(typeRef.contextParameterTypeRefs.size))
                }
            },
            shouldExpandTypeAliases = true
        )
        return FirTypeResolutionResult(
            ConeClassLikeTypeImpl(
                classId.toLookupTag(),
                parameters.toTypedArray(),
                typeRef.isMarkedNullable,
                attributes
            ),
            diagnostic
        )
    }

    override fun resolveType(
        typeRef: FirTypeRef,
        configuration: TypeResolutionConfiguration,
        areBareTypesAllowed: Boolean,
        isOperandOfIsOperator: Boolean,
        resolveDeprecations: Boolean,
        supertypeSupplier: SupertypeSupplier,
        expandTypeAliases: Boolean,
    ): FirTypeResolutionResult {
        return when (typeRef) {
            is FirResolvedTypeRef -> error("Do not resolve, resolved type-refs")
            is FirUserTypeRef -> {
                val result = resolveUserTypeToSymbol(typeRef, configuration, supertypeSupplier, resolveDeprecations)
                val resolvedType = resolveUserType(
                    typeRef,
                    result,
                    areBareTypesAllowed,
                    configuration.topContainer ?: configuration.containingClassDeclarations.lastOrNull(),
                    isOperandOfIsOperator,
                )
                val resolvedTypeSymbol = result.resolvedCandidateOrNull()?.symbol
                // We can expand typealiases from dependencies right away, as it won't depend on us back,
                // so there will be no problems with recursion.
                // In the ideal world, this should also work with some source dependencies as the only case
                // where it does not is when we are a platform module, and we look at the common module
                // from our dependencies.
                // Those are guaranteed to have source sessions, though.
                val isFromLibraryDependency = resolvedTypeSymbol?.moduleData?.session?.kind == FirSession.Kind.Library
                val resolvedExpandedType = when {
                    aliasedTypeExpansionGloballyDisabled -> resolvedType
                    isFromLibraryDependency && resolvedTypeSymbol is FirTypeAliasSymbol -> {
                        resolvedType.fullyExpandedType(resolvedTypeSymbol.moduleData.session)
                    }
                    expandTypeAliases && resolvedTypeSymbol is FirTypeAliasSymbol -> {
                        resolvedType.fullyExpandedType(session)
                    }
                    else -> resolvedType
                }
                FirTypeResolutionResult(resolvedExpandedType, result.resolvedCandidateOrNull()?.diagnostic)
            }
            is FirFunctionTypeRef -> createFunctionType(typeRef)
            is FirDynamicTypeRef -> {
                val attributes = typeRef.annotations.computeTypeAttributes(
                    session,
                    shouldExpandTypeAliases = true
                )
                FirTypeResolutionResult(ConeDynamicType.create(session, attributes), diagnostic = null)
            }
            is FirIntersectionTypeRef -> {
                val leftType = typeRef.leftType.coneType
                if (leftType is ConeTypeParameterType) {
                    FirTypeResolutionResult(ConeDefinitelyNotNullType(leftType), diagnostic = null)
                } else {
                    FirTypeResolutionResult(ConeErrorType(ConeForbiddenIntersection), diagnostic = null)
                }
            }
            else -> error(typeRef.render())
        }.also {
            session.lookupTracker?.recordTypeResolveAsLookup(it.type, typeRef.source, configuration.useSiteFile?.source)
        }
    }
}
