/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.AbstractCallInfo
import org.jetbrains.kotlin.fir.resolve.calls.AbstractCandidate
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionDiagnostic
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.ScopeClassDeclaration
import org.jetbrains.kotlin.fir.scopes.platformClassMapper
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintSystemError
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

@ThreadSafeMutableState
class FirTypeResolverImpl(private val session: FirSession) : FirTypeResolver() {
    private fun resolveSymbol(
        symbol: FirBasedSymbol<*>,
        qualifier: List<FirQualifierPart>,
        qualifierResolver: FirQualifierResolver,
    ): FirBasedSymbol<*>? {
        return when (symbol) {
            is FirClassLikeSymbol<*> -> {
                if (qualifier.size == 1) {
                    symbol
                } else {
                    resolveLocalClassChain(symbol, qualifier)
                        ?: qualifierResolver.resolveSymbolWithPrefix(qualifier, symbol.classId)
                        ?: qualifierResolver.resolveEnumEntrySymbol(qualifier, symbol.classId)
                }
            }
            is FirTypeParameterSymbol -> symbol.takeIf { qualifier.size == 1 }
            else -> error("!")
        }
    }

    private fun FirBasedSymbol<*>?.isVisible(
        useSiteFile: FirFile?,
        containingDeclarations: List<FirDeclaration>,
        supertypeSupplier: SupertypeSupplier
    ): Boolean {
        val declaration = this?.fir
        return if (useSiteFile != null && declaration is FirMemberDeclaration) {
            session.visibilityChecker.isVisible(
                declaration,
                session,
                useSiteFile,
                containingDeclarations,
                dispatchReceiver = null,
                isCallToPropertySetter = false,
                supertypeSupplier = supertypeSupplier
            )
        } else {
            true
        }
    }

    fun resolveUserTypeToSymbol(
        typeRef: FirUserTypeRef,
        scopeClassDeclaration: ScopeClassDeclaration,
        useSiteFile: FirFile?,
        supertypeSupplier: SupertypeSupplier,
        resolveDeprecations: Boolean
    ): TypeResolutionResult {
        val qualifierResolver = session.qualifierResolver
        var applicability: CandidateApplicability? = null

        val candidates = mutableSetOf<TypeCandidate>()
        val qualifier = typeRef.qualifier
        val scopes = scopeClassDeclaration.scopes
        val containingDeclarations = scopeClassDeclaration.containingDeclarations

        fun processCandidate(symbol: FirBasedSymbol<*>, substitutor: ConeSubstitutor?) {
            var symbolApplicability = CandidateApplicability.RESOLVED
            var diagnostic: ConeDiagnostic? = null

            if (!symbol.isVisible(useSiteFile, containingDeclarations, supertypeSupplier)) {
                symbolApplicability = minOf(CandidateApplicability.K2_VISIBILITY_ERROR, symbolApplicability)
                diagnostic = ConeVisibilityError(symbol)
            }

            if (resolveDeprecations) {
                val deprecation = symbol.getOwnDeprecation(session, useSiteFile)
                if (deprecation != null && deprecation.deprecationLevel == DeprecationLevelValue.HIDDEN) {
                    symbolApplicability = minOf(CandidateApplicability.HIDDEN, symbolApplicability)
                    diagnostic = null
                }
            }

            if (applicability == null || symbolApplicability > applicability!!) {
                applicability = symbolApplicability
                candidates.clear()
            }
            if (symbolApplicability == applicability) {
                candidates.add(TypeCandidate(symbol, substitutor, diagnostic, symbolApplicability))
            }
        }

        for (scope in scopes) {
            if (applicability == CandidateApplicability.RESOLVED) break
            scope.processClassifiersByNameWithSubstitution(qualifier.first().name) { symbol, substitutorFromScope ->
                val resolvedSymbol = resolveSymbol(symbol, qualifier, qualifierResolver)
                    ?: return@processClassifiersByNameWithSubstitution

                processCandidate(resolvedSymbol, substitutorFromScope)
            }
        }

        if (applicability != CandidateApplicability.RESOLVED) {
            val symbol = qualifierResolver.resolveSymbol(qualifier)
            if (symbol != null) {
                processCandidate(symbol, null)
            }
        }

        filterOutAmbiguousTypealiases(candidates)

        val candidateCount = candidates.size
        return when {
            candidateCount == 1 -> {
                val candidate = candidates.single()
                TypeResolutionResult.Resolved(candidate)
            }
            candidateCount > 1 -> {
                TypeResolutionResult.Ambiguity(candidates.toList())
            }
            candidateCount == 0 -> {
                TypeResolutionResult.Unresolved
            }
            else -> error("Unexpected")
        }
    }

    private fun filterOutAmbiguousTypealiases(candidates: MutableSet<TypeCandidate>) {
        if (candidates.size <= 1) return

        val aliasesToRemove = mutableSetOf<ClassId>()
        val classTypealiasesThatDontCauseAmbiguity = session.platformClassMapper.classTypealiasesThatDontCauseAmbiguity
        for (candidate in candidates) {
            val symbol = candidate.symbol
            if (symbol is FirClassLikeSymbol<*>) {
                classTypealiasesThatDontCauseAmbiguity[symbol.classId]?.let { aliasesToRemove.add(it) }
            }
        }
        if (aliasesToRemove.isNotEmpty()) {
            candidates.removeAll {
                (it.symbol as? FirClassLikeSymbol)?.classId?.let { classId -> aliasesToRemove.contains(classId) } == true
            }
        }
    }

    sealed class TypeResolutionResult {
        class Ambiguity(val typeCandidates: List<TypeCandidate>) : TypeResolutionResult()
        object Unresolved : TypeResolutionResult()
        class Resolved(val typeCandidate: TypeCandidate) : TypeResolutionResult()
    }

    private fun resolveLocalClassChain(symbol: FirClassLikeSymbol<*>, qualifier: List<FirQualifierPart>): FirRegularClassSymbol? {
        if (symbol !is FirRegularClassSymbol || !symbol.isLocal) {
            return null
        }

        fun resolveLocalClassChain(classSymbol: FirRegularClassSymbol, qualifierIndex: Int): FirRegularClassSymbol? {
            if (qualifierIndex == qualifier.size) {
                return classSymbol
            }

            val qualifierName = qualifier[qualifierIndex].name
            for (declarationSymbol in classSymbol.declarationSymbols) {
                if (declarationSymbol is FirRegularClassSymbol) {
                    if (declarationSymbol.toLookupTag().name == qualifierName) {
                        return resolveLocalClassChain(declarationSymbol, qualifierIndex + 1)
                    }
                }
            }

            return null
        }

        return resolveLocalClassChain(symbol, 1)
    }

    @OptIn(SymbolInternals::class)
    private fun FirQualifierResolver.resolveEnumEntrySymbol(
        qualifier: List<FirQualifierPart>,
        classId: ClassId
    ): FirVariableSymbol<FirEnumEntry>? {
        // Assuming the current qualifier refers to an enum entry, we drop the last part so we get a reference to the enum class.
        val enumClassSymbol = resolveSymbolWithPrefix(qualifier.dropLast(1), classId) ?: return null
        val enumClassFir = enumClassSymbol.fir as? FirRegularClass ?: return null
        if (!enumClassFir.isEnumClass) return null
        val enumEntryMatchingLastQualifier = enumClassFir.declarations
            .firstOrNull { it is FirEnumEntry && it.name == qualifier.last().name } as? FirEnumEntry
        return enumEntryMatchingLastQualifier?.symbol
    }

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
            typeRef.contextReceiverTypeRefs.map { it.coneType } +
                    listOfNotNull(typeRef.receiverTypeRef?.coneType) +
                    typeRef.parameters.map { it.returnTypeRef.coneType.withParameterNameAnnotation(it, session) } +
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

                if (typeRef.contextReceiverTypeRefs.isNotEmpty()) {
                    add(CompilerConeAttributes.ContextFunctionTypeParams(typeRef.contextReceiverTypeRefs.size))
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
        scopeClassDeclaration: ScopeClassDeclaration,
        areBareTypesAllowed: Boolean,
        isOperandOfIsOperator: Boolean,
        resolveDeprecations: Boolean,
        useSiteFile: FirFile?,
        supertypeSupplier: SupertypeSupplier
    ): FirTypeResolutionResult {
        return when (typeRef) {
            is FirResolvedTypeRef -> error("Do not resolve, resolved type-refs")
            is FirUserTypeRef -> {
                val result = resolveUserTypeToSymbol(typeRef, scopeClassDeclaration, useSiteFile, supertypeSupplier, resolveDeprecations)
                val resolvedType = resolveUserType(
                    typeRef,
                    result,
                    areBareTypesAllowed,
                    scopeClassDeclaration.topContainer ?: scopeClassDeclaration.containingDeclarations.lastOrNull(),
                    isOperandOfIsOperator,
                )
                FirTypeResolutionResult(resolvedType, (result as? TypeResolutionResult.Resolved)?.typeCandidate?.diagnostic)
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
        }
    }


    class TypeCandidate(
        override val symbol: FirBasedSymbol<*>,
        val substitutor: ConeSubstitutor?,
        val diagnostic: ConeDiagnostic?,
        override val applicability: CandidateApplicability
    ) : AbstractCandidate() {

        override val dispatchReceiver: FirExpression?
            get() = null

        override val chosenExtensionReceiver: FirExpression?
            get() = null

        override val explicitReceiverKind: ExplicitReceiverKind
            get() = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER

        override val diagnostics: List<ResolutionDiagnostic>
            get() = emptyList()

        override val errors: List<ConstraintSystemError>
            get() = emptyList()

        override val callInfo: AbstractCallInfo
            get() = shouldNotBeCalled()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TypeCandidate) return false

            if (symbol != other.symbol) return false

            return true
        }

        override fun hashCode(): Int {
            return symbol.hashCode()
        }
    }
}
