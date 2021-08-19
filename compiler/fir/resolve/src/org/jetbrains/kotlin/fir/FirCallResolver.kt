/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isReferredViaField
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeStubDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedReifiedParameterReference
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.references.builder.buildBackingFieldReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.calls.tower.FirTowerResolver
import org.jetbrains.kotlin.fir.resolve.calls.tower.TowerResolveManager
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.inference.ResolvedCallableReferenceAtom
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.StoreNameReference
import org.jetbrains.kotlin.fir.resolve.transformers.StoreReceiver
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildStarProjection
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.Variance
import java.lang.annotation.ElementType

class FirCallResolver(
    private val components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val qualifiedResolver: FirQualifiedNameResolver,
) {
    private val session = components.session
    private val overloadByLambdaReturnTypeResolver = FirOverloadByLambdaReturnTypeResolver(components)

    private lateinit var transformer: FirExpressionsResolveTransformer

    fun initTransformer(transformer: FirExpressionsResolveTransformer) {
        this.transformer = transformer
    }

    private val towerResolver = FirTowerResolver(
        components, components.resolutionStageRunner,
    )

    val conflictResolver: ConeCallConflictResolver =
        session.callConflictResolverFactory.create(TypeSpecificityComparator.NONE, session.inferenceComponents)

    @PrivateForInline
    var needTransformArguments: Boolean = true

    @OptIn(PrivateForInline::class)
    fun resolveCallAndSelectCandidate(functionCall: FirFunctionCall): FirFunctionCall {
        qualifiedResolver.reset()
        @Suppress("NAME_SHADOWING")
        val functionCall = if (needTransformArguments) {
            components.context.withIncrementedQualifierPartIndex { functionCall.transformExplicitReceiver() }
                .also {
                    components.dataFlowAnalyzer.enterQualifiedAccessExpression()
                    functionCall.argumentList.transformArguments(transformer, ResolutionMode.ContextDependent)
                }
        } else {
            functionCall
        }

        return components.context.withIncrementedQualifierPartIndex {
            val name = functionCall.calleeReference.name
            val result = collectCandidates(functionCall, name)

            var forceCandidates: Collection<Candidate>? = null
            if (result.candidates.isEmpty()) {
                val newResult = collectCandidates(functionCall, name, CallKind.VariableAccess)
                if (newResult.candidates.isNotEmpty()) {
                    forceCandidates = newResult.candidates
                }
            }

            val nameReference = createResolvedNamedReference(
                functionCall.calleeReference,
                name,
                result.info,
                result.candidates,
                result.applicability,
                functionCall.explicitReceiver,
                expectedCallKind = if (forceCandidates != null) CallKind.VariableAccess else null,
                expectedCandidates = forceCandidates
            )

            val resultExpression = functionCall.transformCalleeReference(StoreNameReference, nameReference)
            val candidate = (nameReference as? FirNamedReferenceWithCandidate)?.candidate
            val resolvedReceiver = functionCall.explicitReceiver
            if (candidate != null && resolvedReceiver is FirResolvedQualifier) {
                resolvedReceiver.replaceResolvedToCompanionObject(candidate.isFromCompanionObjectTypeScope)
            }

            // We need desugaring
            val resultFunctionCall = if (candidate != null && candidate.callInfo != result.info) {
                functionCall.copy(
                    explicitReceiver = candidate.callInfo.explicitReceiver,
                    dispatchReceiver = candidate.dispatchReceiverExpression(),
                    extensionReceiver = candidate.extensionReceiverExpression(),
                    argumentList = candidate.callInfo.argumentList,
                )
            } else {
                resultExpression
            }
            val typeRef = components.typeFromCallee(resultFunctionCall)
            if (typeRef.type is ConeKotlinErrorType) {
                resultFunctionCall.resultType = typeRef
            }
            return@withIncrementedQualifierPartIndex resultFunctionCall
        }
    }

    private inline fun <reified Q : FirQualifiedAccess> Q.transformExplicitReceiver(): Q {
        val explicitReceiver =
            explicitReceiver as? FirQualifiedAccessExpression
                ?: return transformExplicitReceiver(transformer, ResolutionMode.ContextIndependent) as Q

        val callee =
            explicitReceiver.calleeReference as? FirSuperReference
                ?: return transformExplicitReceiver(transformer, ResolutionMode.ContextIndependent) as Q

        transformer.transformSuperReceiver(callee, explicitReceiver, this)

        return this
    }

    private data class ResolutionResult(
        val info: CallInfo, val applicability: CandidateApplicability, val candidates: Collection<Candidate>,
    )

    private fun <T : FirQualifiedAccess> collectCandidates(
        qualifiedAccess: T,
        name: Name,
        forceCallKind: CallKind? = null
    ): ResolutionResult {
        val explicitReceiver = qualifiedAccess.explicitReceiver
        val argumentList = (qualifiedAccess as? FirFunctionCall)?.argumentList ?: FirEmptyArgumentList
        val typeArguments = (qualifiedAccess as? FirFunctionCall)?.typeArguments.orEmpty()

        val info = CallInfo(
            qualifiedAccess,
            forceCallKind ?: if (qualifiedAccess is FirFunctionCall) CallKind.Function else CallKind.VariableAccess,
            name,
            explicitReceiver,
            argumentList,
            isPotentialQualifierPart = qualifiedAccess !is FirFunctionCall &&
                    qualifiedAccess.explicitReceiver is FirResolvedQualifier &&
                    qualifiedResolver.isPotentialQualifierPartPosition(),
            isImplicitInvoke = qualifiedAccess is FirImplicitInvokeCall,
            typeArguments,
            session,
            components.file,
            transformer.components.containingDeclarations,
        )
        towerResolver.reset()
        val result = towerResolver.runResolver(info, transformer.resolutionContext)
        val bestCandidates = result.bestCandidates()

        fun chooseMostSpecific(): Set<Candidate> {
            val onSuperReference = (explicitReceiver as? FirQualifiedAccessExpression)?.calleeReference is FirSuperReference
            return conflictResolver.chooseMaximallySpecificCandidates(
                bestCandidates, discriminateGenerics = true, discriminateAbstracts = onSuperReference
            )
        }

        var reducedCandidates = if (!result.currentApplicability.isSuccess) {
            val distinctApplicabilities = bestCandidates.mapTo(mutableSetOf()) { it.currentApplicability }
            //if all candidates have the same kind on inApplicability - try to choose the most specific one
            if (distinctApplicabilities.size == 1 && distinctApplicabilities.single() > CandidateApplicability.INAPPLICABLE) {
                chooseMostSpecific()
            } else {
                bestCandidates.toSet()
            }
        } else {
            chooseMostSpecific()
        }

        reducedCandidates = overloadByLambdaReturnTypeResolver.reduceCandidates(qualifiedAccess, bestCandidates, reducedCandidates)

        return ResolutionResult(info, result.currentApplicability, reducedCandidates)
    }

    fun <T : FirQualifiedAccess> resolveVariableAccessAndSelectCandidate(qualifiedAccess: T): FirStatement {
        return resolveVariableAccessAndSelectCandidateImpl(qualifiedAccess) { true }
    }

    fun resolveOnlyEnumOrQualifierAccessAndSelectCandidate(qualifiedAccess: FirQualifiedAccessExpression): FirStatement {
        return resolveVariableAccessAndSelectCandidateImpl(qualifiedAccess) accept@{ candidates ->
            val symbol = candidates.singleOrNull()?.symbol ?: return@accept false
            symbol is FirEnumEntrySymbol || symbol is FirRegularClassSymbol
        }
    }

    private fun <T : FirQualifiedAccess> resolveVariableAccessAndSelectCandidateImpl(
        qualifiedAccess: T,
        acceptCandidates: (Collection<Candidate>) -> Boolean
    ): FirStatement {
        val callee = qualifiedAccess.calleeReference as? FirSimpleNamedReference ?: return qualifiedAccess

        return components.context.withIncrementedQualifierPartIndex {
            qualifiedResolver.initProcessingQualifiedAccess(callee, qualifiedAccess.typeArguments)

            @Suppress("NAME_SHADOWING")
            val qualifiedAccess = qualifiedAccess.transformExplicitReceiver<FirQualifiedAccess>()
            qualifiedResolver.replacedQualifier(qualifiedAccess)?.let { resolvedQualifierPart ->
                return@withIncrementedQualifierPartIndex resolvedQualifierPart
            }

            var result = collectCandidates(qualifiedAccess, callee.name)

            if (qualifiedAccess.explicitReceiver == null) {
                if (!result.applicability.isSuccess) {
                    // We should run QualifierResolver if no successful candidates are available
                    // Otherwise expression (even ambiguous) beat qualifier
                    qualifiedResolver.tryResolveAsQualifier(qualifiedAccess.source)?.let { resolvedQualifier ->
                        return@withIncrementedQualifierPartIndex resolvedQualifier
                    }
                }
                qualifiedResolver.reset()
            }

            var functionCallExpected = false
            if (result.candidates.isEmpty() && qualifiedAccess !is FirFunctionCall) {
                val newResult = collectCandidates(qualifiedAccess, callee.name, CallKind.Function)
                if (newResult.candidates.isNotEmpty()) {
                    result = newResult
                    functionCallExpected = true
                }
            }

            val reducedCandidates = result.candidates
            if (!acceptCandidates(reducedCandidates)) return@withIncrementedQualifierPartIndex qualifiedAccess

            val nameReference = createResolvedNamedReference(
                callee,
                callee.name,
                result.info,
                reducedCandidates,
                result.applicability,
                qualifiedAccess.explicitReceiver,
                expectedCallKind = if (functionCallExpected) CallKind.Function else null
            )

            val referencedSymbol = when (nameReference) {
                is FirResolvedNamedReference -> nameReference.resolvedSymbol
                is FirNamedReferenceWithCandidate -> nameReference.candidateSymbol
                else -> null
            }

            val diagnostic = when (nameReference) {
                is FirErrorReferenceWithCandidate -> nameReference.diagnostic
                is FirErrorNamedReference -> nameReference.diagnostic
                else -> null
            }

            (qualifiedAccess.explicitReceiver as? FirResolvedQualifier)?.replaceResolvedToCompanionObject(
                reducedCandidates.isNotEmpty() && reducedCandidates.all { it.isFromCompanionObjectTypeScope }
            )

            when {
                referencedSymbol is FirClassLikeSymbol<*> -> {
                    return@withIncrementedQualifierPartIndex components.buildResolvedQualifierForClass(
                        referencedSymbol,
                        nameReference.source,
                        qualifiedAccess.typeArguments,
                        diagnostic,
                        nonFatalDiagnostics = extractNonFatalDiagnostics(
                            nameReference.source,
                            qualifiedAccess.explicitReceiver,
                            referencedSymbol,
                            (qualifiedAccess as? FirPropertyAccessExpression)?.nonFatalDiagnostics
                        )
                    )
                }
                referencedSymbol is FirTypeParameterSymbol && referencedSymbol.fir.isReified -> {
                    return@withIncrementedQualifierPartIndex buildResolvedReifiedParameterReference {
                        source = nameReference.source
                        symbol = referencedSymbol
                        typeRef = typeForReifiedParameterReference(this)
                    }
                }
            }

            var resultExpression = qualifiedAccess.transformCalleeReference(StoreNameReference, nameReference)
            if (reducedCandidates.size == 1) {
                val candidate = reducedCandidates.single()
                resultExpression = resultExpression.transformDispatchReceiver(StoreReceiver, candidate.dispatchReceiverExpression())
                resultExpression = resultExpression.transformExtensionReceiver(StoreReceiver, candidate.extensionReceiverExpression())
            }
            if (resultExpression is FirExpression) transformer.storeTypeFromCallee(resultExpression)
            return@withIncrementedQualifierPartIndex resultExpression
        }
    }

    private fun extractNonFatalDiagnostics(
        source: FirSourceElement?,
        explicitReceiver: FirExpression?,
        symbol: FirClassLikeSymbol<*>,
        extraNotFatalDiagnostics: List<ConeDiagnostic>?
    ): List<ConeDiagnostic> {
        val prevDiagnostics = (explicitReceiver as? FirResolvedQualifier)?.nonFatalDiagnostics ?: emptyList()
        var result: MutableList<ConeDiagnostic>? = null

        val deprecation = symbol.fir.deprecation?.forUseSite()
        if (deprecation != null) {
            result = mutableListOf()
            result.addAll(prevDiagnostics)
            result.add(ConeDeprecated(source, symbol, deprecation))
        }
        if (extraNotFatalDiagnostics != null && extraNotFatalDiagnostics.isNotEmpty()) {
            if (result == null) {
                result = mutableListOf()
                result.addAll(prevDiagnostics)
            }
            result.addAll(extraNotFatalDiagnostics)
        }

        return result?.toList() ?: prevDiagnostics
    }

    fun resolveCallableReference(
        constraintSystemBuilder: ConstraintSystemBuilder,
        resolvedCallableReferenceAtom: ResolvedCallableReferenceAtom,
    ): Pair<CandidateApplicability, Boolean> {
        val callableReferenceAccess = resolvedCallableReferenceAtom.reference
        val lhs = resolvedCallableReferenceAtom.lhs
        val coneSubstitutor = constraintSystemBuilder.buildCurrentSubstitutor() as ConeSubstitutor
        val expectedType = resolvedCallableReferenceAtom.expectedType?.let(coneSubstitutor::substituteOrSelf)

        val info = createCallableReferencesInfoForLHS(
            callableReferenceAccess, lhs,
            expectedType, constraintSystemBuilder,
        )
        // No reset here!
        val localCollector = CandidateCollector(components, components.resolutionStageRunner)

        val result = transformer.context.withCallableReferenceTowerDataContext(callableReferenceAccess) {
            towerResolver.runResolver(
                info,
                transformer.resolutionContext,
                collector = localCollector,
                manager = TowerResolveManager(localCollector),
            )
        }
        val bestCandidates = result.bestCandidates()
        val applicability = result.currentApplicability
        val noSuccessfulCandidates = !applicability.isSuccess
        val reducedCandidates = if (noSuccessfulCandidates) {
            bestCandidates.toSet()
        } else {
            conflictResolver.chooseMaximallySpecificCandidates(bestCandidates, discriminateGenerics = true)
        }

        (callableReferenceAccess.explicitReceiver as? FirResolvedQualifier)?.replaceResolvedToCompanionObject(
            bestCandidates.isNotEmpty() && bestCandidates.all { it.isFromCompanionObjectTypeScope }
        )

        resolvedCallableReferenceAtom.hasBeenResolvedOnce = true

        when {
            noSuccessfulCandidates -> {
                val errorReference = buildErrorReference(
                    info,
                    if (applicability == CandidateApplicability.UNSUPPORTED) {
                        val unsupportedResolutionDiagnostic = reducedCandidates.firstOrNull()?.diagnostics?.firstOrNull() as? Unsupported
                        ConeUnsupported(unsupportedResolutionDiagnostic?.message ?: "", unsupportedResolutionDiagnostic?.source)
                    } else {
                        ConeUnresolvedReferenceError(info.name)
                    },
                    callableReferenceAccess.source
                )
                resolvedCallableReferenceAtom.resultingReference = errorReference
                return applicability to false
            }
            reducedCandidates.size > 1 -> {
                if (resolvedCallableReferenceAtom.hasBeenPostponed) {
                    val errorReference = buildErrorReference(
                        info,
                        ConeAmbiguityError(info.name, applicability, reducedCandidates),
                        callableReferenceAccess.source
                    )
                    resolvedCallableReferenceAtom.resultingReference = errorReference
                    return applicability to false
                }
                resolvedCallableReferenceAtom.hasBeenPostponed = true
                return applicability to true
            }
        }

        val chosenCandidate = reducedCandidates.single()
        constraintSystemBuilder.runTransaction {
            chosenCandidate.outerConstraintBuilderEffect!!(this)
            true
        }

        val reference = createResolvedNamedReference(
            callableReferenceAccess.calleeReference,
            info.name,
            info,
            reducedCandidates,
            applicability,
            createResolvedReferenceWithoutCandidateForLocalVariables = false
        )
        resolvedCallableReferenceAtom.resultingReference = reference
        resolvedCallableReferenceAtom.resultingTypeForCallableReference = chosenCandidate.resultingTypeForCallableReference

        return applicability to true
    }

    fun resolveDelegatingConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        constructedType: ConeClassLikeType
    ): FirDelegatedConstructorCall {
        val name = SpecialNames.INIT
        val symbol = constructedType.lookupTag.toSymbol(components.session)
        val typeArguments =
            constructedType.typeArguments.take((symbol?.fir as? FirRegularClass)?.typeParameters?.count { it is FirTypeParameter } ?: 0)
                .map {
                    it.toFirTypeProjection()
                }

        val callInfo = CallInfo(
            delegatedConstructorCall,
            CallKind.DelegatingConstructorCall,
            name,
            explicitReceiver = null,
            delegatedConstructorCall.argumentList,
            isPotentialQualifierPart = false,
            isImplicitInvoke = false,
            typeArguments = typeArguments,
            session,
            components.file,
            components.containingDeclarations,
        )
        towerResolver.reset()

        val result = towerResolver.runResolverForDelegatingConstructor(
            callInfo,
            constructedType,
            transformer.resolutionContext
        )

        return components.callResolver.selectDelegatingConstructorCall(delegatedConstructorCall, name, result, callInfo)
    }

    private fun ConeTypeProjection.toFirTypeProjection(): FirTypeProjection = when (this) {
        is ConeStarProjection -> buildStarProjection()
        else -> {
            val type = when (this) {
                is ConeKotlinTypeProjectionIn -> type
                is ConeKotlinTypeProjectionOut -> type
                is ConeStarProjection -> throw IllegalStateException()
                else -> this as ConeKotlinType
            }
            buildTypeProjectionWithVariance {
                typeRef = buildResolvedTypeRef { this.type = type }
                variance = when (kind) {
                    ProjectionKind.IN -> Variance.IN_VARIANCE
                    ProjectionKind.OUT -> Variance.OUT_VARIANCE
                    ProjectionKind.INVARIANT -> Variance.INVARIANT
                    ProjectionKind.STAR -> throw IllegalStateException()
                }
            }
        }
    }

    fun resolveAnnotationCall(annotationCall: FirAnnotationCall): FirAnnotationCall? {
        val reference = annotationCall.calleeReference as? FirSimpleNamedReference ?: return null
        annotationCall.argumentList.transformArguments(transformer, ResolutionMode.ContextDependent)

        val callInfo = CallInfo(
            annotationCall,
            CallKind.Function,
            name = reference.name,
            explicitReceiver = null,
            annotationCall.argumentList,
            isPotentialQualifierPart = false,
            isImplicitInvoke = false,
            typeArguments = emptyList(),
            session,
            components.file,
            components.containingDeclarations
        )

        val annotationClassSymbol = annotationCall.getCorrespondingClassSymbolOrNull(session)
        val resolvedReference = if (annotationClassSymbol != null && annotationClassSymbol.fir.classKind == ClassKind.ANNOTATION_CLASS) {
            val resolutionResult = createCandidateForAnnotationCall(annotationClassSymbol, callInfo)
                ?: ResolutionResult(callInfo, CandidateApplicability.HIDDEN, emptyList())
            createResolvedNamedReference(
                reference,
                reference.name,
                callInfo,
                resolutionResult.candidates,
                resolutionResult.applicability,
                explicitReceiver = null
            )
        } else {
            buildErrorReference(
                callInfo,
                if (annotationClassSymbol != null) ConeIllegalAnnotationError(reference.name)
                //calleeReference and annotationTypeRef are both error nodes so we need to avoid doubling of the diagnostic report
                else ConeStubDiagnostic(ConeUnresolvedNameError(reference.name)),
                reference.source
            )
        }

        return annotationCall.transformCalleeReference(StoreNameReference, resolvedReference)
    }

    private fun createCandidateForAnnotationCall(
        annotationClassSymbol: FirRegularClassSymbol,
        callInfo: CallInfo
    ): ResolutionResult? {
        var constructorSymbol: FirConstructorSymbol? = null
        annotationClassSymbol.fir.unsubstitutedScope(
            session,
            components.scopeSession,
            withForcedTypeCalculator = false
        ).processDeclaredConstructors {
            if (it.fir.isPrimary && constructorSymbol == null) {
                constructorSymbol = it
            }
        }
        if (constructorSymbol == null) return null
        val candidateFactory = CandidateFactory(transformer.resolutionContext, callInfo)
        val candidate = candidateFactory.createCandidate(
            callInfo,
            constructorSymbol!!,
            ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            scope = null
        )
        val applicability = components.resolutionStageRunner.processCandidate(candidate, transformer.resolutionContext)
        return ResolutionResult(callInfo, applicability, listOf(candidate))
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withNoArgumentsTransform(block: () -> T): T {
        val oldValue = needTransformArguments
        needTransformArguments = false
        return try {
            block()
        } finally {
            needTransformArguments = oldValue
        }
    }

    private fun selectDelegatingConstructorCall(
        call: FirDelegatedConstructorCall, name: Name, result: CandidateCollector, callInfo: CallInfo
    ): FirDelegatedConstructorCall {
        val bestCandidates = result.bestCandidates()
        val reducedCandidates = if (!result.currentApplicability.isSuccess) {
            bestCandidates.toSet()
        } else {
            conflictResolver.chooseMaximallySpecificCandidates(bestCandidates, discriminateGenerics = true)
        }

        val nameReference = createResolvedNamedReference(
            call.calleeReference,
            name,
            callInfo,
            reducedCandidates,
            result.currentApplicability,
        )

        return call.transformCalleeReference(StoreNameReference, nameReference).apply {
            val singleCandidate = reducedCandidates.singleOrNull()
            if (singleCandidate != null) {
                val symbol = singleCandidate.symbol
                if (symbol is FirConstructorSymbol && symbol.fir.isInner) {
                    transformDispatchReceiver(StoreReceiver, singleCandidate.dispatchReceiverExpression())
                }
            }
        }
    }

    private fun createCallableReferencesInfoForLHS(
        callableReferenceAccess: FirCallableReferenceAccess,
        lhs: DoubleColonLHS?,
        expectedType: ConeKotlinType?,
        outerConstraintSystemBuilder: ConstraintSystemBuilder?,
    ): CallInfo {
        return CallInfo(
            callableReferenceAccess,
            CallKind.CallableReference,
            callableReferenceAccess.calleeReference.name,
            callableReferenceAccess.explicitReceiver,
            FirEmptyArgumentList,
            isPotentialQualifierPart = false,
            isImplicitInvoke = false,
            emptyList(),
            session,
            components.file,
            transformer.components.containingDeclarations,
            candidateForCommonInvokeReceiver = null,
            // Additional things for callable reference resolve
            expectedType,
            outerConstraintSystemBuilder,
            lhs,
        )
    }

    private fun createResolvedNamedReference(
        reference: FirReference,
        name: Name,
        callInfo: CallInfo,
        candidates: Collection<Candidate>,
        applicability: CandidateApplicability,
        explicitReceiver: FirExpression? = null,
        createResolvedReferenceWithoutCandidateForLocalVariables: Boolean = true,
        expectedCallKind: CallKind? = null,
        expectedCandidates: Collection<Candidate>? = null
    ): FirNamedReference {
        val source = reference.source
        return when {
            expectedCallKind != null -> {
                fun isValueParametersNotEmpty(candidate: Candidate): Boolean {
                    return (candidate.symbol.fir as? FirFunction)?.valueParameters?.size?.let { it > 0 } ?: false
                }

                val candidate = candidates.singleOrNull()

                val diagnostic = if (expectedCallKind == CallKind.Function) {
                    ConeFunctionCallExpectedError(
                        name,
                        candidate?.let { isValueParametersNotEmpty(it) } ?: candidates.any { isValueParametersNotEmpty(it) })
                } else {
                    val singleExpectedCandidate = expectedCandidates?.singleOrNull()

                    var fir = singleExpectedCandidate?.symbol?.fir
                    if (fir is FirTypeAlias) {
                        fir = (fir.expandedTypeRef.coneType.fullyExpandedType(session).toSymbol(session) as? FirRegularClassSymbol)?.fir
                    }

                    if (fir is FirRegularClass) {
                        ConeResolutionToClassifierError(fir.symbol)
                    } else {
                        val coneType = explicitReceiver?.typeRef?.coneType
                        if (coneType != null && !coneType.isUnit) {
                            ConeFunctionExpectedError(
                                name.asString(),
                                (fir as? FirTypedDeclaration)?.returnTypeRef?.coneType ?: coneType
                            )
                        } else {
                            ConeUnresolvedNameError(name)
                        }
                    }
                }

                if (candidate != null) {
                    createErrorReferenceWithExistingCandidate(
                        candidate,
                        diagnostic,
                        source,
                        transformer.resolutionContext,
                        components.resolutionStageRunner
                    )
                } else {
                    buildErrorReference(callInfo, diagnostic, source)
                }
            }

            candidates.isEmpty() -> {
                val diagnostic = if (name.asString() == "invoke" && explicitReceiver is FirConstExpression<*>) {
                    ConeFunctionExpectedError(explicitReceiver.value?.toString() ?: "", explicitReceiver.typeRef.coneType)
                } else {
                    ConeUnresolvedNameError(name)
                }

                buildErrorReference(
                    callInfo,
                    diagnostic,
                    source
                )
            }

            candidates.size > 1 -> buildErrorReference(
                callInfo,
                ConeAmbiguityError(name, applicability, candidates),
                source
            )

            !applicability.isSuccess -> {
                val candidate = candidates.single()
                val diagnostic = when (applicability) {
                    CandidateApplicability.HIDDEN -> ConeHiddenCandidateError(candidate.symbol)
                    CandidateApplicability.NO_COMPANION_OBJECT -> ConeNoCompanionObject(candidate.symbol as FirRegularClassSymbol)
                    else -> ConeInapplicableCandidateError(applicability, candidate)
                }
                createErrorReferenceWithExistingCandidate(
                    candidate,
                    diagnostic,
                    source,
                    transformer.resolutionContext,
                    components.resolutionStageRunner
                )
            }

            else -> {
                val candidate = candidates.single()
                val coneSymbol = candidate.symbol
                if (coneSymbol is FirBackingFieldSymbol) {
                    coneSymbol.fir.isReferredViaField = true
                    return buildBackingFieldReference {
                        this.source = source
                        resolvedSymbol = coneSymbol
                    }
                }
                /*
                 * This `if` is an optimization for local variables and properties without type parameters
                 * Since they have no type variables, so we can don't run completion on them at all and create
                 *   resolved reference immediately
                 *
                 * But for callable reference resolution we should keep candidate, because it was resolved
                 *   with special resolution stages, which saved in candidate additional reference info,
                 *   like `resultingTypeForCallableReference`
                 */
                if (
                    createResolvedReferenceWithoutCandidateForLocalVariables &&
                    explicitReceiver?.typeRef?.coneTypeSafe<ConeIntegerLiteralType>() == null &&
                    coneSymbol is FirVariableSymbol &&
                    (coneSymbol !is FirPropertySymbol || (coneSymbol.fir as FirMemberDeclaration).typeParameters.isEmpty())
                ) {
                    return buildResolvedNamedReference {
                        this.source = source
                        this.name = name
                        resolvedSymbol = coneSymbol
                    }
                }
                FirNamedReferenceWithCandidate(source, name, candidate)
            }
        }
    }

    private fun buildErrorReference(
        callInfo: CallInfo,
        diagnostic: ConeDiagnostic,
        source: FirSourceElement?
    ): FirErrorReferenceWithCandidate {
        return createErrorReferenceWithErrorCandidate(
            callInfo,
            diagnostic,
            source,
            transformer.resolutionContext,
            components.resolutionStageRunner
        )
    }
}
