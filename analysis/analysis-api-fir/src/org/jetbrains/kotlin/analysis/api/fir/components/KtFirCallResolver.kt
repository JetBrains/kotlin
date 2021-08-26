/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.calls.*
import org.jetbrains.kotlin.analysis.api.components.KtCallResolver
import org.jetbrains.kotlin.analysis.api.diagnostics.KtNonBoundToPsiErrorDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.analysis.api.fir.getCandidateSymbols
import org.jetbrains.kotlin.analysis.api.fir.isImplicitFunctionCall
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirArrayOfSymbolProvider.arrayOf
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirArrayOfSymbolProvider.arrayOfSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirArrayOfSymbolProvider.arrayTypeToArrayOfCall
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.FirErrorReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.idea.references.FirReferenceResolveHelper
import org.jetbrains.kotlin.idea.references.readWriteAccess
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findAssignment
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class KtFirCallResolver(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtCallResolver(), KtFirAnalysisSessionComponent {
    private val diagnosticCache = mutableListOf<FirDiagnostic>()

    override fun resolveAccessorCall(call: KtSimpleNameExpression): KtCall? = withValidityAssertion {
        when (val fir = call.getOrBuildFir(firResolveState)) {
            is FirResolvedNamedReference -> {
                val propertySymbol = fir.resolvedSymbol as? FirPropertySymbol ?: return null
                val access = call.readWriteAccess(useResolveForReadWrite = false)
                val setterValue = findAssignment(call)?.right
                val accessor = when {
                    access.isWrite -> propertySymbol.setterSymbol?.fir
                    access.isRead -> propertySymbol.getterSymbol?.fir
                    else -> null
                } ?: return null
                val accessorSymbol = analysisSession.firSymbolBuilder.functionLikeBuilder.buildFunctionLikeSymbol(accessor)
                val target =
                    if (!access.isWrite || setterValue != null)
                        KtSuccessCallTarget(accessorSymbol, token)
                    else // access.isWrite && setterValue == null
                        KtErrorCallTarget(
                            listOf(accessorSymbol),
                            KtNonBoundToPsiErrorDiagnostic(factoryName = null, "Setter value is missing", token),
                            token
                        )
                val ktArgumentMapping = LinkedHashMap<KtExpression, KtValueParameterSymbol>()
                if (access.isWrite && setterValue != null) {
                    val setterParameterSymbol = accessor.valueParameters.single().buildSymbol(firSymbolBuilder) as KtValueParameterSymbol
                    ktArgumentMapping[setterValue] = setterParameterSymbol
                }
                return KtFunctionCall(ktArgumentMapping, target, KtSubstitutor.Empty(token), token)
            }
            else -> return null
        }
    }

    override fun resolveCall(call: KtBinaryExpression): KtCall? = withValidityAssertion {
        when (val fir = call.getOrBuildFir(firResolveState)) {
            is FirFunctionCall -> resolveCall(fir)
            is FirComparisonExpression -> resolveCall(fir.compareToCall)
            is FirEqualityOperatorCall -> null // TODO
            else -> null
        }
    }

    override fun resolveCall(call: KtUnaryExpression): KtCall? = withValidityAssertion {
        when (val fir = call.getOrBuildFir(firResolveState)) {
            is FirFunctionCall -> resolveCall(fir)
            is FirBlock -> {
                // Desugared increment or decrement block. See [BaseFirBuilder#generateIncrementOrDecrementBlock]
                // There would be corresponding inc()/dec() call that is assigned back to a temp variable.
                val prefix = fir.statements.filterIsInstance<FirVariableAssignment>().find { it.rValue is FirFunctionCall }
                (prefix?.rValue as? FirFunctionCall)?.let { resolveCall(it) }
            }
            is FirCheckNotNullCall -> null // TODO
            else -> null
        }
    }

    override fun resolveCall(call: KtCallElement): KtCall? = withValidityAssertion {
        return when (val fir = call.getOrBuildFir(firResolveState)) {
            is FirArrayOfCall -> resolveArrayOfCall(fir)
            is FirFunctionCall -> resolveCall(fir)
            is FirAnnotationCall -> fir.asAnnotationCall()
            is FirDelegatedConstructorCall -> fir.asDelegatedConstructorCall()
            is FirConstructor -> fir.asDelegatedConstructorCall()
            is FirSafeCallExpression -> fir.regularQualifiedAccess.safeAs<FirFunctionCall>()?.let { resolveCall(it) }
            else -> null
        }
    }

    override fun resolveCall(call: KtArrayAccessExpression): KtCall? = withValidityAssertion {
        return when (val fir = call.getOrBuildFir(firResolveState)) {
            is FirFunctionCall -> resolveCall(fir)
            else -> null
        }
    }

    private fun resolveArrayOfCall(arrayOfCall: FirArrayOfCall): KtCall? {
        val arrayOfSymbol = with(analysisSession) {
            val type = arrayOfCall.typeRef.coneTypeSafe<ConeClassLikeType>()
                ?: return run {
                    val defaultArrayOfSymbol = arrayOfSymbol(arrayOf) ?: return null
                    KtFunctionCall(
                        arrayOfCall.createArgumentMapping(defaultArrayOfSymbol),
                        KtErrorCallTarget(
                            listOf(defaultArrayOfSymbol),
                            KtNonBoundToPsiErrorDiagnostic(factoryName = null, "type of arrayOf call is not resolved", token),
                            token
                        ),
                        arrayOfCall.createSubstitutorFromTypeArguments(defaultArrayOfSymbol),
                        token
                    )
                }
            val call = arrayTypeToArrayOfCall[type.lookupTag.classId] ?: arrayOf
            arrayOfSymbol(call)
        } ?: return null
        return KtFunctionCall(
            arrayOfCall.createArgumentMapping(arrayOfSymbol),
            KtSuccessCallTarget(arrayOfSymbol, token),
            arrayOfCall.createSubstitutorFromTypeArguments(arrayOfSymbol),
            token
        )
    }

    private fun FirArrayOfCall.createSubstitutorFromTypeArguments(arrayOfSymbol: KtFirFunctionSymbol): KtSubstitutor {
        return arrayOfSymbol.firRef.withFir {
            // No type parameter means this is an arrayOf call of primitives, in which case there is no type arguments
            val typeParameter = it.typeParameters.singleOrNull() ?: return@withFir null
            val elementType = typeRef.coneTypeSafe<ConeClassLikeType>()?.arrayElementType() ?: return@withFir null
            val coneSubstitutor = substitutorByMap(mapOf(typeParameter.symbol to elementType), rootModuleSession)
            firSymbolBuilder.typeBuilder.buildSubstitutor(coneSubstitutor)
        } ?: KtSubstitutor.Empty(token)
    }

    private fun resolveCall(firCall: FirFunctionCall): KtCall? {
        val session = firResolveState.rootModuleSession
        return when {
            firCall.isImplicitFunctionCall() -> {
                val target = with(FirReferenceResolveHelper) {
                    val calleeReference = (firCall.dispatchReceiver as FirQualifiedAccessExpression).calleeReference
                    calleeReference.toTargetSymbol(session, firSymbolBuilder).singleOrNull()
                }
                when (target) {
                    is KtVariableLikeSymbol -> firCall.createCallByVariableLikeSymbolCall(target)
                    null -> null
                    else -> firCall.asSimpleFunctionCall()
                }
            }
            else -> firCall.asSimpleFunctionCall()
        }
    }

    private fun FirFunctionCall.createCallByVariableLikeSymbolCall(variableLikeSymbol: KtVariableLikeSymbol): KtCall? {
        val (functionSymbol, target) = when (val callReference = calleeReference) {
            is FirResolvedNamedReference -> {
                val functionSymbol = callReference.resolvedSymbol as? FirNamedFunctionSymbol
                (functionSymbol?.fir?.buildSymbol(firSymbolBuilder) as? KtFunctionSymbol)?.let {
                    functionSymbol to KtSuccessCallTarget(it, token)
                } ?: return null
            }
            is FirErrorNamedReference -> {
                val functionSymbol = callReference.candidateSymbol as? FirNamedFunctionSymbol
                functionSymbol to callReference.createErrorCallTarget(source)
            }
            else -> error("Unexpected call reference ${callReference::class.simpleName}")
        }
        val callableId = functionSymbol?.callableId ?: return null
        return if (callableId in kotlinFunctionInvokeCallableIds) {
            // A fake override is always created for a function type with all types substituted properly inside the dispatch receiver. Hence
            // there is no need for additional substitutor.
            KtFunctionalTypeVariableCall(variableLikeSymbol, createArgumentMapping(), target, KtSubstitutor.Empty(token), token)
        } else {
            val substitutor = createSubstitutorFromTypeArguments(functionSymbol)
            KtVariableWithInvokeFunctionCall(
                variableLikeSymbol,
                createArgumentMapping(),
                target,
                substitutor, token
            )
        }
    }

    private fun FirFunctionCall.asSimpleFunctionCall(): KtFunctionCall? {
        val calleeReference = this.calleeReference
        val target = calleeReference.createCallTarget() ?: return null
        val symbol = when (calleeReference) {
            is FirResolvedNamedReference -> calleeReference.resolvedSymbol as? FirCallableSymbol<*>
            is FirErrorNamedReference -> calleeReference.candidateSymbol as? FirCallableSymbol<*>
            else -> null
        } ?: return null
        return KtFunctionCall(createArgumentMapping(), target, createSubstitutorFromTypeArguments(symbol), token)
    }

    private fun FirFunctionCall.createSubstitutorFromTypeArguments(functionSymbol: FirCallableSymbol<*>): KtSubstitutor {
        val typeArgumentMap = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()
        for (i in typeArguments.indices) {
            val type = typeArguments[i].safeAs<FirTypeProjectionWithVariance>()?.typeRef?.coneType
            if (type != null) {
                typeArgumentMap[functionSymbol.typeParameterSymbols[i]] = type
            }
        }
        val coneSubstitutor = substitutorByMap(typeArgumentMap, rootModuleSession)
        return firSymbolBuilder.typeBuilder.buildSubstitutor(coneSubstitutor)
    }

    private fun FirAnnotationCall.asAnnotationCall(): KtAnnotationCall? {
        val target = calleeReference.createCallTarget() ?: return null
        return KtAnnotationCall(createArgumentMapping(), target, token)
    }

    private fun FirDelegatedConstructorCall.asDelegatedConstructorCall(): KtDelegatedConstructorCall? {
        val target = calleeReference.createCallTarget() ?: return null
        val kind = if (isSuper) KtDelegatedConstructorCallKind.SUPER_CALL else KtDelegatedConstructorCallKind.THIS_CALL
        return KtDelegatedConstructorCall(createArgumentMapping(), target, kind, token)
    }

    private fun FirConstructor.asDelegatedConstructorCall(): KtDelegatedConstructorCall? {
        // A delegation call may not be present in the source code:
        //
        //   class A {
        //     constructor(i: Int)   // <--- implicit constructor delegation call (empty element after RPAR)
        //   }
        //
        // and FIR built/found from that implicit `KtConstructorDelegationCall` is `FirConstructor`,
        // which may have a pointer to the delegated constructor.
        return delegatedConstructor?.asDelegatedConstructorCall()
    }

    private fun FirReference.createCallTarget(): KtCallTarget? {
        return when (this) {
            is FirSuperReference -> createCallTarget(source)
            is FirResolvedNamedReference -> getKtFunctionOrConstructorSymbol()?.let { KtSuccessCallTarget(it, token) }
            is FirErrorNamedReference -> createErrorCallTarget(source)
            is FirErrorReferenceWithCandidate -> createErrorCallTarget(source)
            is FirSimpleNamedReference ->
                null
            /*  error(
                  """
                    Looks like ${this::class.simpleName} && it calle reference ${calleeReference::class.simpleName} were not resolved to BODY_RESOLVE phase,
                    consider resolving it containing declaration before starting resolve calls
                    ${this.render()}
                    ${(this.psi as? KtElement)?.getElementTextInContext()}
                    """.trimIndent()
              )*/
            else -> error("Unexpected call reference ${this::class.simpleName}")
        }
    }

    private fun FirExpression.findSourceKtExpressionForCallArgument(): KtExpression? {
        // For spread, named, and lambda arguments, the source is the KtValueArgument.
        // For other arguments (including array indices), the source is the KtExpression.
        return when (this) {
            is FirNamedArgumentExpression, is FirSpreadArgumentExpression, is FirLambdaArgumentExpression ->
                realPsi.safeAs<KtValueArgument>()?.getArgumentExpression()
            else -> realPsi as? KtExpression
        }
    }

    private fun mapArgumentExpressionToParameter(
        argumentExpression: FirExpression,
        parameterSymbol: KtValueParameterSymbol,
        argumentMapping: LinkedHashMap<KtExpression, KtValueParameterSymbol>
    ) {
        if (argumentExpression is FirVarargArgumentsExpression) {
            for (varargArgument in argumentExpression.arguments) {
                val valueArgument = varargArgument.findSourceKtExpressionForCallArgument() ?: return
                argumentMapping[valueArgument] = parameterSymbol
            }
        } else {
            val valueArgument = argumentExpression.findSourceKtExpressionForCallArgument() ?: return
            argumentMapping[valueArgument] = parameterSymbol
        }
    }

    private fun FirCall.createArgumentMapping(): LinkedHashMap<KtExpression, KtValueParameterSymbol> {
        val ktArgumentMapping = LinkedHashMap<KtExpression, KtValueParameterSymbol>()
        argumentMapping?.let {
            for ((firExpression, firValueParameter) in it.entries) {
                val parameterSymbol = firValueParameter.buildSymbol(firSymbolBuilder) as KtValueParameterSymbol
                mapArgumentExpressionToParameter(firExpression, parameterSymbol, ktArgumentMapping)
            }
        }
        return ktArgumentMapping
    }

    private fun FirArrayOfCall.createArgumentMapping(
        arrayOfCallSymbol: KtFirFunctionSymbol
    ): LinkedHashMap<KtExpression, KtValueParameterSymbol> {
        val ktArgumentMapping = LinkedHashMap<KtExpression, KtValueParameterSymbol>()
        val parameterSymbol = arrayOfCallSymbol.firRef.withFir {
            it.valueParameters.single().buildSymbol(firSymbolBuilder) as KtValueParameterSymbol
        }
        for (firExpression in argumentList.arguments) {
            mapArgumentExpressionToParameter(firExpression, parameterSymbol, ktArgumentMapping)
        }
        return ktArgumentMapping
    }

    private fun FirErrorNamedReference.createErrorCallTarget(qualifiedAccessSource: FirSourceElement?): KtErrorCallTarget =
        KtErrorCallTarget(
            getCandidateSymbols().mapNotNull { it.fir.buildSymbol(firSymbolBuilder) as? KtFunctionLikeSymbol },
            source?.let { diagnostic.asKtDiagnostic(it, qualifiedAccessSource, diagnosticCache) }
                ?: KtNonBoundToPsiErrorDiagnostic(factoryName = null, diagnostic.reason, token), token)

    private fun FirErrorReferenceWithCandidate.createErrorCallTarget(qualifiedAccessSource: FirSourceElement?): KtErrorCallTarget =
        KtErrorCallTarget(
            getCandidateSymbols().mapNotNull { it.fir.buildSymbol(firSymbolBuilder) as? KtFunctionLikeSymbol },
            source?.let { diagnostic.asKtDiagnostic(it, qualifiedAccessSource, diagnosticCache) }
                ?: KtNonBoundToPsiErrorDiagnostic(factoryName = null, diagnostic.reason, token), token)

    private fun FirResolvedNamedReference.getKtFunctionOrConstructorSymbol(): KtFunctionLikeSymbol? =
        resolvedSymbol.fir.buildSymbol(firSymbolBuilder) as? KtFunctionLikeSymbol

    private fun FirSuperReference.createCallTarget(qualifiedAccessSource: FirSourceElement?): KtCallTarget? =
        when (val type = superTypeRef.coneType) {
            is ConeKotlinErrorType ->
                KtErrorCallTarget(
                    (firSymbolBuilder.classifierBuilder.buildClassLikeSymbolByLookupTag(type.lookupTag) as? KtSymbolWithMembers)?.let {
                        analysisSession.getPrimaryConstructor(it)?.let { ctor -> listOf(ctor) }
                    } ?: emptyList(),
                    source?.let { type.diagnostic.asKtDiagnostic(it, qualifiedAccessSource, diagnosticCache) }
                        ?: KtNonBoundToPsiErrorDiagnostic(factoryName = null, type.diagnostic.reason, token), token)
            is ConeClassLikeType ->
                type.classId?.let { classId ->
                    (firSymbolBuilder.classifierBuilder.buildClassLikeSymbolByClassId(classId) as? KtSymbolWithMembers)?.let {
                        analysisSession.getPrimaryConstructor(it)?.let { ctor -> KtSuccessCallTarget(ctor, token) }
                    }
                }
            else ->
                error("Unexpected type in super reference: ${type::class}")
        }

    private fun KtAnalysisSession.getPrimaryConstructor(symbolWithMembers: KtSymbolWithMembers): KtConstructorSymbol? =
        symbolWithMembers.getDeclaredMemberScope().getConstructors().firstOrNull { it.isPrimary }

    companion object {
        private val kotlinFunctionInvokeCallableIds = (0..23).flatMapTo(hashSetOf()) { arity ->
            listOf(
                CallableId(StandardNames.getFunctionClassId(arity), OperatorNameConventions.INVOKE),
                CallableId(StandardNames.getSuspendFunctionClassId(arity), OperatorNameConventions.INVOKE)
            )
        }
    }
}
