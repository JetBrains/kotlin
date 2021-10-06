/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeUnexpectedTypeArgumentsError
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeOuterClassArgumentsRequired
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedQualifierError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnsupportedDynamicType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeWrongNumberOfTypeArgumentsError
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.ScopeClassDeclaration
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds

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

    private fun resolveToSymbol(
        typeRef: FirTypeRef,
        scope: FirScope,
    ): Pair<FirBasedSymbol<*>?, ConeSubstitutor?> {
        return when (typeRef) {
            is FirResolvedTypeRef -> {
                val resultSymbol = typeRef.coneTypeSafe<ConeLookupTagBasedType>()?.lookupTag?.let(symbolProvider::getSymbolByLookupTag)
                resultSymbol to null
            }

            is FirUserTypeRef -> {
                val qualifierResolver = session.qualifierResolver
                var resolvedSymbol: FirBasedSymbol<*>? = null
                var substitutor: ConeSubstitutor? = null
                val qualifier = typeRef.qualifier
                scope.processClassifiersByNameWithSubstitution(qualifier.first().name) { symbol, substitutorFromScope ->
                    if (resolvedSymbol != null) return@processClassifiersByNameWithSubstitution
                    resolvedSymbol = when (symbol) {
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
                    substitutor = substitutorFromScope
                }

                // TODO: Imports
                val resultSymbol: FirBasedSymbol<*>? = resolvedSymbol ?: qualifierResolver.resolveSymbol(qualifier)
                resultSymbol to substitutor
            }

            is FirImplicitBuiltinTypeRef -> {
                resolveBuiltInQualified(typeRef.id, session) to null
            }

            else -> null to null
        }
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
        symbol: FirBasedSymbol<*>?,
        substitutor: ConeSubstitutor?,
        areBareTypesAllowed: Boolean,
        topDeclaration: FirRegularClass?,
        isOperandOfIsOperator: Boolean
    ): ConeKotlinType {
        if (symbol == null || symbol !is FirClassifierSymbol<*>) {
            val diagnostic = if (symbol?.fir is FirEnumEntry) {
                if (isOperandOfIsOperator) {
                    ConeSimpleDiagnostic("'is' operator can not be applied to an enum entry.", DiagnosticKind.IsEnumEntry)
                } else {
                    ConeSimpleDiagnostic("An enum entry should not be used as a type.", DiagnosticKind.EnumEntryAsType)
                }
            } else {
                ConeUnresolvedQualifierError(typeRef.render())
            }
            return ConeKotlinErrorType(diagnostic)
        }
        if (symbol is FirTypeParameterSymbol) {
            for (part in typeRef.qualifier) {
                if (part.typeArgumentList.typeArguments.isNotEmpty()) {
                    return ConeClassErrorType(
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
                val typeParameters = symbol.fir.typeParameters

                val (typeParametersAlignedToQualifierParts, outerClasses) = getClassesAlignedToQualifierParts(symbol, qualifier, session)

                for ((index, typeParameter) in typeParameters.withIndex()) {
                    val (parameterClass, qualifierPartIndex) = typeParametersAlignedToQualifierParts[typeParameter.symbol] ?: continue

                    if (index < typeArgumentsCount) {
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
                        isValidTypeParameterFromOuterClass(typeParameter.symbol, topDeclaration, session)
                    ) {
                        val type = ConeTypeParameterTypeImpl(ConeTypeParameterLookupTag(typeParameter.symbol), isNullable = false)
                        val substituted = actualSubstitutor.substituteOrNull(type)
                        if (substituted == null) {
                            createDiagnosticsIfExists(parameterClass, qualifierPartIndex, symbol, typeRef)?.let { return it }
                        } else {
                            allTypeArguments.add(substituted)
                        }
                    } else {
                        return ConeClassErrorType(ConeOuterClassArgumentsRequired(parameterClass.symbol))
                    }
                }

                // Check rest type arguments
                if (typeArgumentsCount > typeParameters.size) {
                    for (index in qualifier.indices) {
                        if (qualifier[index].typeArgumentList.typeArguments.isNotEmpty()) {
                            val parameterClass = outerClasses.elementAtOrNull(index)
                            createDiagnosticsIfExists(
                                parameterClass,
                                index,
                                symbol,
                                typeRef
                            )?.let { return it }
                        }
                    }
                }
            }
        }

        return symbol.constructType(allTypeArguments.toTypedArray(), typeRef.isMarkedNullable, typeRef.annotations.computeTypeAttributes())
            .also {
                val lookupTag = it.lookupTag
                if (lookupTag is ConeClassLikeLookupTagImpl && symbol is FirClassLikeSymbol<*>) {
                    lookupTag.bindSymbolToLookupTag(session, symbol)
                }
            }
    }

    @OptIn(SymbolInternals::class)
    private fun getClassesAlignedToQualifierParts(
        symbol: FirRegularClassSymbol,
        qualifier: List<FirQualifierPart>,
        session: FirSession
    ): ParametersMapAndOuterClasses {
        var currentClass: FirRegularClass? = null
        val outerClasses = mutableListOf<FirRegularClass?>()

        // Try to get at least qualifier.size classes that match qualifier parts
        var qualifierPartIndex = 0
        while (qualifierPartIndex < qualifier.size || currentClass != null) {
            if (qualifierPartIndex == 0) {
                currentClass = symbol.fir
            } else {
                if (currentClass != null) {
                    currentClass = currentClass.getContainingDeclaration(session) as? FirRegularClass
                }
            }

            outerClasses.add(currentClass)
            qualifierPartIndex++
        }

        val outerArgumentsCount = outerClasses.size - qualifier.size
        val reversedOuterClasses = outerClasses.asReversed()
        val result = mutableMapOf<FirTypeParameterSymbol, ClassWithQualifierPartIndex>()

        for (index in reversedOuterClasses.indices) {
            currentClass = reversedOuterClasses[index]
            if (currentClass != null) {
                for (typeParameter in currentClass.typeParameters) {
                    val typeParameterSymbol = typeParameter.symbol
                    if (!result.containsKey(typeParameterSymbol)) {
                        result[typeParameterSymbol] = ClassWithQualifierPartIndex(currentClass, index - outerArgumentsCount)
                    }
                }
            }
        }

        return ParametersMapAndOuterClasses(result, reversedOuterClasses.drop(outerArgumentsCount))
    }

    private data class ParametersMapAndOuterClasses(
        val parameters: Map<FirTypeParameterSymbol, ClassWithQualifierPartIndex>,
        val outerClasses: List<FirRegularClass?>
    )

    private data class ClassWithQualifierPartIndex(
        val klass: FirRegularClass,
        val index: Int
    )

    @OptIn(SymbolInternals::class)
    private fun createDiagnosticsIfExists(
        parameterClass: FirRegularClass?,
        qualifierPartIndex: Int,
        symbol: FirRegularClassSymbol,
        userTypeRef: FirUserTypeRef,
        qualifierPartArgumentsCount: Int? = null
    ): ConeClassErrorType? {
        // TODO: It should be TYPE_ARGUMENTS_NOT_ALLOWED diagnostics when parameterClass is null
        val actualTypeParametersCount = (parameterClass ?: symbol.fir).getActualTypeParametersCount(session)

        if (qualifierPartArgumentsCount == null || actualTypeParametersCount != qualifierPartArgumentsCount) {
            val source = getTypeArgumentsOrNameSource(userTypeRef, qualifierPartIndex)
            if (source != null) {
                return ConeClassErrorType(
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

    private fun getTypeArgumentsOrNameSource(typeRef: FirUserTypeRef, qualifierIndex: Int?): FirSourceElement? {
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
                    typeRef.valueParameters.map { it.returnTypeRef.coneType } +
                    listOf(typeRef.returnTypeRef.coneType)
        val classId = if (typeRef.isSuspend) {
            StandardClassIds.SuspendFunctionN(typeRef.parametersCount)
        } else {
            StandardClassIds.FunctionN(typeRef.parametersCount)
        }
        val attributes = typeRef.annotations.computeTypeAttributes()
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
        isOperandOfIsOperator: Boolean
    ): ConeKotlinType {
        return when (typeRef) {
            is FirResolvedTypeRef -> typeRef.type
            is FirUserTypeRef -> {
                val (symbol, substitutor) = resolveToSymbol(typeRef, scopeClassDeclaration.scope)
                resolveUserType(
                    typeRef,
                    symbol,
                    substitutor,
                    areBareTypesAllowed,
                    scopeClassDeclaration.topDeclaration,
                    isOperandOfIsOperator
                )
            }
            is FirFunctionTypeRef -> createFunctionalType(typeRef)
            is FirDynamicTypeRef -> ConeKotlinErrorType(ConeUnsupportedDynamicType())
            else -> error(typeRef.render())
        }
    }
}
