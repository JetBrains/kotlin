/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeUnexpectedTypeArgumentsError
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.AbstractCallInfo
import org.jetbrains.kotlin.fir.resolve.calls.AbstractCandidate
import org.jetbrains.kotlin.fir.resolve.calls.ReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionDiagnostic
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.ScopeClassDeclaration
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintSystemError
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue

@ThreadSafeMutableState
class FirTypeResolverImpl(private val session: FirSession) : FirTypeResolver() {

    private val symbolProvider by lazy {
        session.symbolProvider
    }

    private data class ClassIdInSession(val session: FirSession, val id: ClassId)

    private val implicitBuiltinTypeSymbols = mutableMapOf<ClassIdInSession, FirClassLikeSymbol<*>>()

    // TODO: get rid of session used here, and may be also of the cache above (see KT-30275)
    private fun resolveBuiltInQualified(id: ClassId, session: FirSession): FirClassLikeSymbol<*> {
        val nameInSession = ClassIdInSession(session, id)
        return implicitBuiltinTypeSymbols.getOrPut(nameInSession) {
            symbolProvider.getClassLikeSymbolByClassId(id)!!
        }
    }

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
            is FirTypeParameterSymbol -> {
                assert(qualifier.size == 1)
                symbol
            }
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
        supertypeSupplier: SupertypeSupplier
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
                symbolApplicability = minOf(CandidateApplicability.VISIBILITY_ERROR, symbolApplicability)
                diagnostic = ConeVisibilityError(symbol)
            }

            val deprecation = symbol.getDeprecation(useSiteFile)
            if (deprecation != null && deprecation.deprecationLevel == DeprecationLevelValue.HIDDEN) {
                symbolApplicability = minOf(CandidateApplicability.HIDDEN, symbolApplicability)
                diagnostic = null
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
                    ConeUnresolvedQualifierError(typeRef.render())
                }
            }
            return ConeErrorType(diagnostic)
        }
        if (symbol is FirTypeParameterSymbol) {
            for (part in typeRef.qualifier) {
                if (part.typeArgumentList.typeArguments.isNotEmpty()) {
                    return ConeErrorType(
                        ConeUnexpectedTypeArgumentsError("Type arguments not allowed", part.typeArgumentList.source)
                    )
                }
            }
        }

        val allTypeArguments = mutableListOf<ConeTypeProjection>()
        var typeArgumentsCount = 0

        val qualifier = typeRef.qualifier
        for (qualifierIndex in qualifier.size - 1 downTo 0) {
            val qualifierTypeArguments = qualifier[qualifierIndex].typeArgumentList.typeArguments

            for (qualifierTypeArgument in qualifierTypeArguments) {
                allTypeArguments.add(qualifierTypeArgument.toConeTypeProjection())
                typeArgumentsCount++
            }
        }

        if (symbol is FirRegularClassSymbol) {
            val isPossibleBareType = areBareTypesAllowed && allTypeArguments.isEmpty()
            if (!isPossibleBareType) {
                val actualSubstitutor = substitutor ?: ConeSubstitutor.Empty

                val originalTypeParameters = symbol.fir.typeParameters

                val (typeParametersAlignedToQualifierParts, outerDeclarations) = getClassesAlignedToQualifierParts(
                    symbol,
                    qualifier,
                    session
                )

                val actualTypeParametersCount =
                    when (symbol) {
                        is FirTypeAliasSymbol ->
                            outerDeclarations.sumOf { it?.let { d -> getActualTypeParametersCount(d) } ?: 0 }
                        else -> symbol.typeParameterSymbols.size
                    }

                for ((typeParameterIndex, typeParameter) in originalTypeParameters.withIndex()) {
                    val (parameterClass, qualifierPartIndex) = typeParametersAlignedToQualifierParts[typeParameter.symbol] ?: continue

                    if (typeParameterIndex < typeArgumentsCount) {
                        // Check if type argument matches type parameter in respective qualifier part
                        val qualifierPartArgumentsCount = qualifier[qualifierPartIndex].typeArgumentList.typeArguments.size
                        createDiagnosticsIfExists(
                            parameterClass,
                            qualifierPartIndex,
                            symbol,
                            typeRef,
                            qualifierPartArgumentsCount
                        )?.let { return it }
                        continue
                    }

                    if (typeParameter !is FirOuterClassTypeParameterRef ||
                        isValidTypeParameterFromOuterDeclaration(typeParameter.symbol, topContainer, session)
                    ) {
                        val type = ConeTypeParameterTypeImpl(ConeTypeParameterLookupTag(typeParameter.symbol), isNullable = false)
                        val substituted = actualSubstitutor.substituteOrNull(type)
                        if (substituted == null) {
                            createDiagnosticsIfExists(
                                parameterClass,
                                qualifierPartIndex,
                                symbol,
                                typeRef,
                                qualifierPartArgumentsCount = null
                            )?.let { return it }
                        } else {
                            allTypeArguments.add(substituted)
                        }
                    } else {
                        return ConeErrorType(ConeOuterClassArgumentsRequired(parameterClass.symbol))
                    }
                }

                // Check rest type arguments
                if (typeArgumentsCount > actualTypeParametersCount) {
                    for (index in qualifier.indices) {
                        if (qualifier[index].typeArgumentList.typeArguments.isNotEmpty()) {
                            val parameterClass = outerDeclarations.elementAtOrNull(index)
                            createDiagnosticsIfExists(
                                parameterClass,
                                index,
                                symbol,
                                typeRef,
                                qualifierPartArgumentsCount = null
                            )?.let { return it }
                        }
                    }
                }
            }
        }

        return symbol.constructType(
            allTypeArguments.toTypedArray(),
            typeRef.isMarkedNullable,
            typeRef.annotations.computeTypeAttributes(session)
        ).also {
            val lookupTag = it.lookupTag
            if (lookupTag is ConeClassLikeLookupTagImpl && symbol is FirClassLikeSymbol<*>) {
                lookupTag.bindSymbolToLookupTag(session, symbol)
            }
        }
    }

    @OptIn(SymbolInternals::class)
    private fun getClassesAlignedToQualifierParts(
        symbol: FirClassLikeSymbol<*>,
        qualifier: List<FirQualifierPart>,
        session: FirSession
    ): ParametersMapAndOuterClasses {
        var currentClassLikeDeclaration: FirClassLikeDeclaration? = null
        val outerDeclarations = mutableListOf<FirClassLikeDeclaration?>()

        // Try to get at least qualifier.size classes that match qualifier parts
        var qualifierPartIndex = 0
        while (qualifierPartIndex < qualifier.size || currentClassLikeDeclaration != null) {
            if (qualifierPartIndex == 0) {
                currentClassLikeDeclaration = symbol.fir
            } else {
                if (currentClassLikeDeclaration != null) {
                    currentClassLikeDeclaration = currentClassLikeDeclaration.getContainingDeclaration(session)
                }
            }

            outerDeclarations.add(currentClassLikeDeclaration)
            qualifierPartIndex++
        }

        val outerArgumentsCount = outerDeclarations.size - qualifier.size
        val reversedOuterClasses = outerDeclarations.asReversed()
        val result = mutableMapOf<FirTypeParameterSymbol, ClassWithQualifierPartIndex>()

        for (index in reversedOuterClasses.indices) {
            currentClassLikeDeclaration = reversedOuterClasses[index]
            val typeParameters = when (currentClassLikeDeclaration) {
                is FirTypeAlias -> currentClassLikeDeclaration.typeParameters
                is FirClass -> currentClassLikeDeclaration.typeParameters
                else -> null
            }
            if (currentClassLikeDeclaration != null && typeParameters != null) {
                for (typeParameter in typeParameters) {
                    val typeParameterSymbol = typeParameter.symbol
                    if (!result.containsKey(typeParameterSymbol)) {
                        result[typeParameterSymbol] = ClassWithQualifierPartIndex(currentClassLikeDeclaration, index - outerArgumentsCount)
                    }
                }
            }
        }

        return ParametersMapAndOuterClasses(result, reversedOuterClasses.drop(outerArgumentsCount))
    }

    private data class ParametersMapAndOuterClasses(
        val parameters: Map<FirTypeParameterSymbol, ClassWithQualifierPartIndex>,
        val outerClasses: List<FirClassLikeDeclaration?>
    )

    private data class ClassWithQualifierPartIndex(
        val klass: FirClassLikeDeclaration,
        val index: Int
    )

    @OptIn(SymbolInternals::class)
    private fun createDiagnosticsIfExists(
        parameterClass: FirClassLikeDeclaration?,
        qualifierPartIndex: Int,
        symbol: FirClassLikeSymbol<*>,
        userTypeRef: FirUserTypeRef,
        qualifierPartArgumentsCount: Int?
    ): ConeErrorType? {
        // TODO: It should be TYPE_ARGUMENTS_NOT_ALLOWED diagnostics when parameterClass is null
        val actualTypeParametersCount = getActualTypeParametersCount(parameterClass ?: symbol.fir)

        if (qualifierPartArgumentsCount == null || actualTypeParametersCount != qualifierPartArgumentsCount) {
            val source = getTypeArgumentsOrNameSource(userTypeRef, qualifierPartIndex)
            if (source != null) {
                return ConeErrorType(
                    ConeWrongNumberOfTypeArgumentsError(
                        actualTypeParametersCount,
                        parameterClass?.symbol ?: symbol,
                        source
                    )
                )
            }
        }

        return null
    }

    private fun getActualTypeParametersCount(element: FirClassLikeDeclaration): Int {
        return (element as FirTypeParameterRefsOwner).typeParameters
            .count { it !is FirOuterClassTypeParameterRef }
    }

    private fun getTypeArgumentsOrNameSource(typeRef: FirUserTypeRef, qualifierIndex: Int?): KtSourceElement? {
        val qualifierPart = if (qualifierIndex != null) typeRef.qualifier.elementAtOrNull(qualifierIndex) else null
        val typeArgumentsList = qualifierPart?.typeArgumentList
        return if (typeArgumentsList == null || typeArgumentsList.typeArguments.isEmpty()) {
            qualifierPart?.source ?: typeRef.source
        } else {
            typeArgumentsList.source
        }
    }

    private fun createFunctionalType(typeRef: FirFunctionTypeRef): ConeClassLikeType {
        val parameters =
            listOfNotNull(typeRef.receiverTypeRef?.coneType) +
                    typeRef.valueParameters.map { it.returnTypeRef.coneType.withParameterNameAnnotation(it) } +
                    listOf(typeRef.returnTypeRef.coneType)
        val classId = if (typeRef.isSuspend) {
            StandardClassIds.SuspendFunctionN(typeRef.parametersCount)
        } else {
            StandardClassIds.FunctionN(typeRef.parametersCount)
        }
        val attributes = typeRef.annotations.computeTypeAttributes(session)
        val symbol = resolveBuiltInQualified(classId, session)
        return ConeClassLikeTypeImpl(
            symbol.toLookupTag().also {
                if (it is ConeClassLikeLookupTagImpl) {
                    it.bindSymbolToLookupTag(session, symbol)
                }
            },
            parameters.toTypedArray(),
            typeRef.isMarkedNullable,
            attributes
        )
    }

    override fun resolveType(
        typeRef: FirTypeRef,
        scopeClassDeclaration: ScopeClassDeclaration,
        areBareTypesAllowed: Boolean,
        isOperandOfIsOperator: Boolean,
        useSiteFile: FirFile?,
        supertypeSupplier: SupertypeSupplier
    ): Pair<ConeKotlinType, ConeDiagnostic?> {
        return when (typeRef) {
            is FirResolvedTypeRef -> error("Do not resolve, resolved type-refs")
            is FirUserTypeRef -> {
                val result = resolveUserTypeToSymbol(typeRef, scopeClassDeclaration, useSiteFile, supertypeSupplier)
                resolveUserType(
                    typeRef,
                    result,
                    areBareTypesAllowed,
                    scopeClassDeclaration.topContainer ?: scopeClassDeclaration.containingDeclarations.lastOrNull(),
                    isOperandOfIsOperator
                ) to (result as? TypeResolutionResult.Resolved)?.typeCandidate?.diagnostic
            }
            is FirFunctionTypeRef -> createFunctionalType(typeRef) to null
            is FirDynamicTypeRef -> ConeDynamicType.create(session) to null
            is FirIntersectionTypeRef -> {
                val leftType = typeRef.leftType.coneType
                val rightType = typeRef.rightType.coneType

                if (rightType.isAny && leftType is ConeTypeParameterType) {
                    ConeDefinitelyNotNullType(leftType) to null
                } else {
                    ConeErrorType(ConeUnsupported("Intersection types are not supported yet", typeRef.source)) to null
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

        override val dispatchReceiverValue: ReceiverValue?
            get() = null

        override val extensionReceiverValue: ReceiverValue?
            get() = null

        override val explicitReceiverKind: ExplicitReceiverKind
            get() = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER

        override val diagnostics: List<ResolutionDiagnostic>
            get() = emptyList()

        override val errors: List<ConstraintSystemError>
            get() = emptyList()

        override val callInfo: AbstractCallInfo
            get() = throw UnsupportedOperationException("Should not be called")

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
