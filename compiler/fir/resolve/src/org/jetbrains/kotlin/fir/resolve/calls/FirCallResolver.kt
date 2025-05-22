/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.copyAsImplicitInvokeCall
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasExplicitBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.isReferredViaField
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedReifiedParameterReference
import org.jetbrains.kotlin.fir.getPrimaryConstructorSymbol
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.references.builder.buildBackingFieldReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.candidate.*
import org.jetbrains.kotlin.fir.resolve.calls.overloads.ConeCallConflictResolver
import org.jetbrains.kotlin.fir.resolve.calls.overloads.FirOverloadByLambdaReturnTypeResolver
import org.jetbrains.kotlin.fir.resolve.calls.overloads.callConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.calls.stages.ResolutionStageRunner
import org.jetbrains.kotlin.fir.resolve.calls.tower.FirTowerResolver
import org.jetbrains.kotlin.fir.resolve.calls.tower.TowerGroup
import org.jetbrains.kotlin.fir.resolve.calls.tower.TowerResolveManager
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.inference.constraintsLogger
import org.jetbrains.kotlin.fir.resolve.inference.csBuilder
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.addNonFatalDiagnostics
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.transformers.doesResolutionResultOverrideOtherToPreserveCompatibility
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildStarProjection
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.ApplicabilityDetail
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class FirCallResolver(
    private val components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val towerResolver: FirTowerResolver = FirTowerResolver(components, components.resolutionStageRunner)
) {
    private val session = components.session
    private val overloadByLambdaReturnTypeResolver = FirOverloadByLambdaReturnTypeResolver(components)

    private lateinit var transformer: FirExpressionsResolveTransformer

    fun initTransformer(transformer: FirExpressionsResolveTransformer) {
        this.transformer = transformer
    }

    val conflictResolver: ConeCallConflictResolver =
        session.callConflictResolverFactory.create(TypeSpecificityComparator.NONE, session.inferenceComponents, components)

    fun resolveCallAndSelectCandidate(functionCall: FirFunctionCall, resolutionMode: ResolutionMode): FirFunctionCall {
        val name = functionCall.calleeReference.name
        val result = collectCandidates(functionCall, name, origin = functionCall.origin, resolutionMode = resolutionMode)

        var forceCandidates: Collection<Candidate>? = null
        if (result.candidates.isEmpty()) {
            val newResult = collectCandidates(
                functionCall,
                name,
                CallKind.VariableAccess,
                origin = functionCall.origin,
                resolutionMode = resolutionMode
            )
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

        functionCall.replaceCalleeReference(nameReference)
        val candidate = (nameReference as? FirNamedReferenceWithCandidate)?.candidate
        val resolvedReceiver = functionCall.explicitReceiver?.unwrapSmartcastExpression()
        if (candidate != null && resolvedReceiver is FirResolvedQualifier) {
            resolvedReceiver.replaceResolvedToCompanionObject(candidate.isFromCompanionObjectTypeScope)
        }

        candidate?.updateSourcesOfReceivers()

        // We need desugaring
        val resultFunctionCall = if (candidate != null && candidate.callInfo != result.info) {
            functionCall.copyAsImplicitInvokeCall {
                explicitReceiver = candidate.callInfo.explicitReceiver
                dispatchReceiver = candidate.dispatchReceiverExpression()
                extensionReceiver = candidate.chosenExtensionReceiverExpression()
                argumentList = candidate.callInfo.argumentList
                contextArguments.addAll(candidate.contextArguments())
            }
        } else {
            functionCall
        }
        val type = components.typeFromCallee(resultFunctionCall)
        if (type is ConeErrorType) {
            resultFunctionCall.resultType = type
        }

        return resultFunctionCall
    }

    private data class ResolutionResult(
        val info: CallInfo, val applicability: CandidateApplicability, val candidates: Collection<Candidate>,
    )

    /** WARNING: This function is public for the analysis API and should only be used there. */
    fun collectAllCandidates(
        qualifiedAccess: FirQualifiedAccessExpression,
        name: Name,
        containingDeclarations: List<FirDeclaration> = transformer.components.containingDeclarations,
        resolutionContext: ResolutionContext = transformer.resolutionContext,
        resolutionMode: ResolutionMode,
    ): List<OverloadCandidate> {
        val collector = AllCandidatesCollector(components, components.resolutionStageRunner)
        val origin = (qualifiedAccess as? FirFunctionCall)?.origin ?: FirFunctionCallOrigin.Regular
        fun collectCandidates(forceCallKind: CallKind?): ResolutionResult = collectCandidates(
            qualifiedAccess = qualifiedAccess,
            name = name,
            origin = origin,
            containingDeclarations = containingDeclarations,
            resolutionContext = resolutionContext,
            collector = collector,
            resolutionMode = resolutionMode,
            forceCallKind = forceCallKind,
        )

        var result = collectCandidates(forceCallKind = null)
        if (result.candidates.isEmpty() && qualifiedAccess !is FirFunctionCall) {
            val newResult = collectCandidates(forceCallKind = CallKind.Function)
            if (newResult.candidates.isNotEmpty()) {
                result = newResult
            }
        }

        return collector.allCandidates.map { candidate ->
            OverloadCandidate(candidate, isInBestCandidates = candidate in result.candidates)
        }
    }

    private fun collectCandidates(
        qualifiedAccess: FirQualifiedAccessExpression,
        name: Name,
        forceCallKind: CallKind? = null,
        isUsedAsGetClassReceiver: Boolean = false,
        origin: FirFunctionCallOrigin = FirFunctionCallOrigin.Regular,
        containingDeclarations: List<FirDeclaration> = transformer.components.containingDeclarations,
        resolutionContext: ResolutionContext = transformer.resolutionContext,
        collector: CandidateCollector? = null,
        callSite: FirElement = qualifiedAccess,
        resolutionMode: ResolutionMode,
    ): ResolutionResult {
        val explicitReceiver = qualifiedAccess.explicitReceiver
        val argumentList = (qualifiedAccess as? FirFunctionCall)?.argumentList ?: FirEmptyArgumentList
        val typeArguments = if (qualifiedAccess is FirFunctionCall || forceCallKind == CallKind.Function) {
            qualifiedAccess.typeArguments
        } else emptyList()

        val info = CallInfo(
            callSite,
            forceCallKind ?: if (qualifiedAccess is FirFunctionCall) CallKind.Function else CallKind.VariableAccess,
            name,
            explicitReceiver,
            argumentList,
            isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
            typeArguments,
            session,
            components.file,
            containingDeclarations,
            origin = origin,
            resolutionMode = resolutionMode,
            implicitInvokeMode = if (qualifiedAccess is FirImplicitInvokeCall) ImplicitInvokeMode.Regular else ImplicitInvokeMode.None,
        )
        towerResolver.reset()
        session.constraintsLogger?.logCall(qualifiedAccess)

        val result = towerResolver.runResolver(info, resolutionContext, collector)
        var (reducedCandidates, applicability) = reduceCandidates(result, explicitReceiver, resolutionContext)
        reducedCandidates = overloadByLambdaReturnTypeResolver.reduceCandidates(qualifiedAccess, reducedCandidates, reducedCandidates)

        return ResolutionResult(info, applicability, reducedCandidates)
    }

    /**
     * Returns a [Pair] consisting of the reduced candidates and the new applicability if it has changed and `null` otherwise.
     */
    private fun reduceCandidates(
        collector: CandidateCollector,
        explicitReceiver: FirExpression? = null,
        resolutionContext: ResolutionContext = transformer.resolutionContext,
    ): Pair<Set<Candidate>, CandidateApplicability> {
        fun chooseMostSpecific(list: List<Candidate>): Set<Candidate> {
            val onSuperReference = explicitReceiver is FirSuperReceiverExpression
            return conflictResolver.chooseMaximallySpecificCandidates(list, discriminateAbstracts = onSuperReference)
        }

        val candidates = collector.bestCandidates()

        if (collector.isSuccess) {
            return chooseMostSpecific(candidates) to collector.currentApplicability
        }

        if (candidates.size > 1) {
            // First, fully process all of them and group them by their worst applicability.
            val groupedByDiagnosticCount = candidates.groupBy {
                components.resolutionStageRunner.fullyProcessCandidate(it, resolutionContext)
                it.diagnostics.minOf(ResolutionDiagnostic::applicability)
            }

            // Then, select the group with the least bad applicability.
            groupedByDiagnosticCount.maxBy { it.key }.let {
                return chooseMostSpecific(it.value) to it.key
            }
        }

        return candidates.toSet() to collector.currentApplicability
    }

    fun resolveVariableAccessAndSelectCandidate(
        qualifiedAccess: FirQualifiedAccessExpression,
        isUsedAsReceiver: Boolean,
        isUsedAsGetClassReceiver: Boolean,
        callSite: FirElement,
        resolutionMode: ResolutionMode,
    ): FirExpression {
        return resolveVariableAccessAndSelectCandidateImpl(
            qualifiedAccess,
            isUsedAsReceiver,
            resolutionMode,
            isUsedAsGetClassReceiver,
            callSite
        ) { true }
    }

    private fun resolveVariableAccessAndSelectCandidateImpl(
        qualifiedAccess: FirQualifiedAccessExpression,
        isUsedAsReceiver: Boolean,
        resolutionMode: ResolutionMode,
        isUsedAsGetClassReceiver: Boolean,
        callSite: FirElement = qualifiedAccess,
        acceptCandidates: (Collection<Candidate>) -> Boolean,
    ): FirExpression {
        val callee = qualifiedAccess.calleeReference as? FirSimpleNamedReference ?: return qualifiedAccess

        @Suppress("NAME_SHADOWING")
        val qualifiedAccess = qualifiedAccess.let(transformer::transformExplicitReceiverOf)
        val nonFatalDiagnosticFromExpression = (qualifiedAccess as? FirPropertyAccessExpression)?.nonFatalDiagnostics.orEmpty()

        val basicResult by lazy(LazyThreadSafetyMode.NONE) {
            collectCandidates(
                qualifiedAccess,
                callee.name,
                isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
                callSite = callSite,
                resolutionMode = resolutionMode
            )
        }

        // Even if it's not receiver, it makes sense to continue qualifier if resolution is unsuccessful
        // just to try to resolve to package/class and then report meaningful error at FirStandaloneQualifierChecker
        @OptIn(ApplicabilityDetail::class)
        if (isUsedAsReceiver || !basicResult.applicability.isSuccess) {
            val explicitReceiver = qualifiedAccess.explicitReceiver?.unwrapSmartcastExpression() as? FirResolvedQualifier
            val diagnosticFromTypeArguments = if (explicitReceiver != null && explicitReceiver.typeArguments.isNotEmpty()) {
                ConeTypeArgumentsForOuterClass(explicitReceiver.source!!)
            } else null
            explicitReceiver
                ?.continueQualifier(
                    callee,
                    qualifiedAccess,
                    nonFatalDiagnosticFromExpression + listOfNotNull(diagnosticFromTypeArguments),
                    session,
                    components
                )
                ?.takeIf { it.applicability == CandidateApplicability.RESOLVED || !basicResult.applicability.isSuccess }
                ?.let { return it.qualifier }
        }

        var result = basicResult

        if (qualifiedAccess.explicitReceiver == null) {
            // Even if we successfully resolved to some companion/named object, we should re-try with qualifier resolution
            // import D.*
            // class A {
            //     object B
            // }
            // class D {
            //     object A
            // }
            // fun main() {
            //     A // should resolved to D.A
            //     A.B // should be resolved to A.B
            // }
            @OptIn(ApplicabilityDetail::class)
            if (!result.applicability.isSuccess || (isUsedAsReceiver && result.candidates.all { it.symbol is FirClassLikeSymbol })) {
                components.resolveRootPartOfQualifier(
                    callee, qualifiedAccess, nonFatalDiagnosticFromExpression, isUsedAsReceiver
                )
                    ?.takeIf { it.applicability == CandidateApplicability.RESOLVED || !result.applicability.isSuccess }
                    ?.let { return it.qualifier }
            }
        }

        var functionCallExpected = false
        if (result.candidates.isEmpty() && qualifiedAccess !is FirFunctionCall) {
            val newResult = collectCandidates(qualifiedAccess, callee.name, CallKind.Function, resolutionMode = resolutionMode)
            if (newResult.candidates.isNotEmpty()) {
                result = newResult
                functionCallExpected = true
            }
        }

        val reducedCandidates = result.candidates
        if (!acceptCandidates(reducedCandidates)) return qualifiedAccess

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
            is FirResolvedErrorReference -> nameReference.diagnostic
            is FirErrorNamedReference -> nameReference.diagnostic
            else -> null
        }

        (qualifiedAccess.explicitReceiver?.unwrapSmartcastExpression() as? FirResolvedQualifier)?.replaceResolvedToCompanionObject(
            reducedCandidates.isNotEmpty() && reducedCandidates.all { it.isFromCompanionObjectTypeScope }
        )

        when {
            referencedSymbol is FirClassLikeSymbol<*> -> {
                val extraDiagnostic =
                    runIf(reducedCandidates.singleOrNull()?.doesResolutionResultOverrideOtherToPreserveCompatibility() == true) {
                        ConeResolutionResultOverridesOtherToPreserveCompatibility
                    }
                val nonFatalDiagnosticFromExpressionWithExtra = nonFatalDiagnosticFromExpression + listOfNotNull(extraDiagnostic)
                return components.buildResolvedQualifierForClass(
                    referencedSymbol,
                    qualifiedAccess.source,
                    qualifiedAccess.typeArguments,
                    diagnostic ?: extractNestedClassAccessDiagnostic(
                        nameReference.source,
                        qualifiedAccess.explicitReceiver,
                        referencedSymbol
                    ),
                    nonFatalDiagnostics = extractNonFatalDiagnostics(
                        nameReference.source,
                        qualifiedAccess.explicitReceiver,
                        referencedSymbol,
                        nonFatalDiagnosticFromExpressionWithExtra,
                        session
                    ),
                    annotations = qualifiedAccess.annotations
                )
            }
            referencedSymbol is FirTypeParameterSymbol && referencedSymbol.fir.isReified && diagnostic == null -> {
                return buildResolvedReifiedParameterReference {
                    source = nameReference.source
                    symbol = referencedSymbol
                    coneTypeOrNull = typeForReifiedParameterReference(this)
                }
            }
        }

        qualifiedAccess.replaceCalleeReference(nameReference)
        if (reducedCandidates.size == 1) {
            val candidate = reducedCandidates.single()
            candidate.updateSourcesOfReceivers()
            qualifiedAccess.apply {
                replaceDispatchReceiver(candidate.dispatchReceiverExpression())
                replaceExtensionReceiver(candidate.chosenExtensionReceiverExpression())
                replaceContextArguments(candidate.contextArguments())
                addNonFatalDiagnostics(candidate)
            }
        }
        transformer.storeTypeFromCallee(qualifiedAccess, isLhsOfAssignment = callSite is FirVariableAssignment)
        return qualifiedAccess
    }

    /**
     *
     * It should always change the [ConeResolvedCallableReferenceAtom.state]
     * (either by calling [ConeResolvedCallableReferenceAtom.initializeResultingReference]
     * or by setting it to POSTPONED_BECAUSE_OF_AMBIGUITY).
     *
     * Might be called twice on the same callable reference.
     *
     * @return The best [CandidateApplicability] and a [Boolean] indicating
     *         whether the result is successful or can be successful in future (after another round of resolution)
     *
     * Thus, `false` would mean that this callable reference just doesn't have any chances to be resolved successfully
     */
    fun resolveCallableReference(
        containingCallCandidate: Candidate,
        resolvedCallableReferenceAtom: ConeResolvedCallableReferenceAtom,
        hasSyntheticOuterCall: Boolean,
    ): Pair<CandidateApplicability, Boolean> {
        require(resolvedCallableReferenceAtom.needsResolution)

        val containingCallCS = containingCallCandidate.csBuilder
        val callableReferenceAccess = resolvedCallableReferenceAtom.expression
        val calleeReference = callableReferenceAccess.calleeReference
        val lhs = resolvedCallableReferenceAtom.lhs
        val coneSubstitutor = containingCallCS.buildCurrentSubstitutor() as ConeSubstitutor
        val expectedType = resolvedCallableReferenceAtom.expectedType?.let(coneSubstitutor::substituteOrSelf)

        val info = createCallableReferencesInfoForLHS(
            callableReferenceAccess, lhs, expectedType, hasSyntheticOuterCall
        )
        // No reset here!
        val localCollector = CandidateCollector(components, components.resolutionStageRunner)

        val result = transformer.context.withCallableReferenceTowerDataContext(callableReferenceAccess) {
            towerResolver.runResolver(
                info,
                transformer.resolutionContext,
                collector = localCollector,
                manager = TowerResolveManager(localCollector),
                candidateFactory = CandidateFactory.createForCallableReferenceCandidate(
                    transformer.resolutionContext, containingCallCandidate
                )
            )
        }

        val (reducedCandidates, applicability) = reduceCandidates(result, callableReferenceAccess.explicitReceiver)

        (callableReferenceAccess.explicitReceiver?.unwrapSmartcastExpression() as? FirResolvedQualifier)?.replaceResolvedToCompanionObject(
            reducedCandidates.isNotEmpty() && reducedCandidates.all { it.isFromCompanionObjectTypeScope }
        )

        when {
            reducedCandidates.isEmpty() || reducedCandidates.any { !it.isSuccessful } -> {
                val errorReference = buildReferenceWithErrorCandidate(
                    info,
                    when {
                        applicability == CandidateApplicability.K2_UNSUPPORTED -> {
                            val unsupportedResolutionDiagnostic =
                                reducedCandidates.firstOrNull()?.diagnostics?.firstOrNull() as? Unsupported
                            ConeUnsupported(unsupportedResolutionDiagnostic?.message ?: "", unsupportedResolutionDiagnostic?.source)
                        }
                        reducedCandidates.size > 1 -> ConeAmbiguityError(info.name, applicability, reducedCandidates)
                        reducedCandidates.size == 1 -> createConeDiagnosticForCandidateWithError(applicability, reducedCandidates.single())
                        else -> ConeUnresolvedReferenceError(info.name)
                    },
                    calleeReference.source
                )
                resolvedCallableReferenceAtom.initializeResultingReference(errorReference)
                return applicability to false
            }
            reducedCandidates.size > 1 -> {
                if (resolvedCallableReferenceAtom.isPostponedBecauseOfAmbiguity) {
                    val errorReference = buildReferenceWithErrorCandidate(
                        info,
                        ConeAmbiguityError(info.name, applicability, reducedCandidates),
                        calleeReference.source
                    )
                    resolvedCallableReferenceAtom.initializeResultingReference(errorReference)
                    return applicability to false
                }
                resolvedCallableReferenceAtom.state = ConeResolvedCallableReferenceAtom.State.POSTPONED_BECAUSE_OF_AMBIGUITY
                return applicability to true
            }
        }

        val chosenCandidate = reducedCandidates.single()
        chosenCandidate.updateSourcesOfReceivers()

        // Due to CandidateFactory.Companion.createForCallableReferenceCandidate, it's guaranteed that
        // all callable reference candidates' CS are effectively clones of the containing call's constraint systems.
        //
        // And after we processed the reference candidate, its CS becomes a superset of the original one.
        // Thus, we apply it back for the single successful chosen candidate
        containingCallCS.replaceContentWith(chosenCandidate.system.currentStorage())

        val reference = createResolvedNamedReference(
            calleeReference,
            info.name,
            info,
            reducedCandidates,
            applicability,
            createResolvedReferenceWithoutCandidateForLocalVariables = false
        )
        resolvedCallableReferenceAtom.initializeResultingReference(reference)
        resolvedCallableReferenceAtom.resultingTypeForCallableReference = chosenCandidate.resultingTypeForCallableReference

        return applicability to true
    }

    fun callInfoForDelegatingConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        constructedType: ConeClassLikeType?
    ): CallInfo {
        val name = SpecialNames.INIT
        val symbol = constructedType?.lookupTag?.toSymbol(components.session)
        val typeArguments = constructedType?.typeArguments
            ?.take((symbol?.fir as? FirRegularClass)?.typeParameters?.count { it is FirTypeParameter } ?: 0)
            ?.map { it.toFirTypeProjection() }
            ?: emptyList()

        return CallInfo(
            delegatedConstructorCall,
            CallKind.DelegatingConstructorCall,
            name,
            explicitReceiver = null,
            delegatedConstructorCall.argumentList,
            isUsedAsGetClassReceiver = false,
            typeArguments = typeArguments,
            session,
            components.file,
            components.containingDeclarations,
            resolutionMode = ResolutionMode.ContextIndependent,
            implicitInvokeMode = ImplicitInvokeMode.None,
        )
    }

    fun resolveDelegatingConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        constructedType: ConeClassLikeType?,
        derivedClassLookupTag: ConeClassLikeLookupTag
    ): FirDelegatedConstructorCall {
        val callInfo = callInfoForDelegatingConstructorCall(delegatedConstructorCall, constructedType)
        towerResolver.reset()
        session.constraintsLogger?.logCall(delegatedConstructorCall)

        if (constructedType == null) {
            val errorReference = createErrorReferenceWithErrorCandidate(
                callInfo,
                ConeSimpleDiagnostic("Erroneous delegated constructor call", DiagnosticKind.UnresolvedSupertype),
                delegatedConstructorCall.calleeReference.source,
                transformer.resolutionContext,
                components.resolutionStageRunner
            )
            return delegatedConstructorCall.apply {
                replaceCalleeReference(errorReference)
            }
        }

        val result = towerResolver.runResolverForDelegatingConstructor(
            callInfo,
            constructedType,
            derivedClassLookupTag,
            transformer.resolutionContext
        )

        return selectDelegatingConstructorCall(delegatedConstructorCall, callInfo.name, result, callInfo)
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
                typeRef = buildResolvedTypeRef { this.coneType = type }
                variance = when (kind) {
                    ProjectionKind.IN -> Variance.IN_VARIANCE
                    ProjectionKind.OUT -> Variance.OUT_VARIANCE
                    ProjectionKind.INVARIANT -> Variance.INVARIANT
                    ProjectionKind.STAR -> throw IllegalStateException()
                }
            }
        }
    }

    fun resolveAnnotationCall(annotation: FirAnnotationCall): FirAnnotationCall? {
        val reference = annotation.calleeReference as? FirSimpleNamedReference ?: return null
        val annotationClassSymbol = annotation.getCorrespondingClassSymbolOrNull(session)
        val annotationTypeRef = annotation.annotationTypeRef
        val annotationConeType = annotationTypeRef.coneType
        val resolvedReference = if (annotationClassSymbol != null && annotationClassSymbol.fir.classKind == ClassKind.ANNOTATION_CLASS) {
            val constructorSymbol = getAnnotationConstructorSymbol(annotationConeType, annotationClassSymbol)

            transformer.transformAnnotationCallArguments(annotation, constructorSymbol)

            val callInfo = toCallInfo(annotation, reference)
            session.constraintsLogger?.logCall(annotation)

            val resolutionResult = constructorSymbol
                ?.let { runResolutionForGivenSymbol(callInfo, it) }
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
            annotation.replaceArgumentList(annotation.argumentList.transform(transformer, ResolutionMode.ContextDependent))

            val callInfo = toCallInfo(annotation, reference)

            buildReferenceWithErrorCandidate(
                callInfo,
                if (annotationClassSymbol != null) {
                    ConeIllegalAnnotationError(reference.name)
                } else if (annotationConeType is ConeErrorType || annotationConeType !is ConeClassLikeType) {
                    //calleeReference and annotationTypeRef are both error nodes so we need to avoid doubling of the diagnostic report
                    ConeUnreportedDuplicateDiagnostic(
                        //prefer diagnostic with symbol, e.g. to use the symbol during navigation in IDE
                        (annotationConeType as? ConeErrorType)?.diagnostic as? ConeDiagnosticWithSymbol<*>
                            ?: ConeUnresolvedNameError(reference.name)
                    )
                } else {
                    ConeIllegalAnnotationError(reference.name)
                },
                reference.source
            )
        }

        return annotation.apply {
            replaceCalleeReference(resolvedReference)
        }
    }

    fun getAnnotationConstructorSymbol(
        annotationConeType: ConeKotlinType,
        annotationClassSymbol: FirRegularClassSymbol?,
    ): FirConstructorSymbol? {
        val immediateSymbol = annotationConeType.abbreviatedTypeOrSelf.toSymbol(session) as? FirClassLikeSymbol<*>
            ?: annotationClassSymbol // Shouldn't be the case for green code
        val constructorSymbol = immediateSymbol?.getPrimaryConstructorSymbol(session, components.scopeSession)
        constructorSymbol?.lazyResolveToPhase(FirResolvePhase.TYPES)
        return constructorSymbol
    }

    private fun toCallInfo(annotation: FirAnnotationCall, reference: FirSimpleNamedReference): CallInfo = CallInfo(
        annotation,
        CallKind.Function,
        name = reference.name,
        explicitReceiver = null,
        annotation.argumentList,
        isUsedAsGetClassReceiver = false,
        typeArguments = annotation.typeArguments,
        session,
        components.file,
        components.containingDeclarations,
        resolutionMode = ResolutionMode.ContextIndependent,
        implicitInvokeMode = ImplicitInvokeMode.None,
    )

    private fun runResolutionForGivenSymbol(callInfo: CallInfo, symbol: FirBasedSymbol<*>): ResolutionResult {
        val candidateFactory = CandidateFactory(transformer.resolutionContext, callInfo)
        val candidate = candidateFactory.createCandidate(
            callInfo,
            symbol,
            ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            scope = null
        )
        val applicability = components.resolutionStageRunner.processCandidate(candidate, transformer.resolutionContext)
        return ResolutionResult(callInfo, applicability, listOf(candidate))
    }

    private fun selectDelegatingConstructorCall(
        call: FirDelegatedConstructorCall, name: Name, result: CandidateCollector, callInfo: CallInfo
    ): FirDelegatedConstructorCall {
        val (reducedCandidates, applicability) = reduceCandidates(result)

        val nameReference = createResolvedNamedReference(
            call.calleeReference,
            name,
            callInfo,
            reducedCandidates,
            applicability,
        )

        return call.apply {
            call.replaceCalleeReference(nameReference)
            val singleCandidate = reducedCandidates.singleOrNull()
            singleCandidate?.updateSourcesOfReceivers()
            if (singleCandidate != null) {
                val symbol = singleCandidate.symbol
                if (symbol is FirConstructorSymbol && symbol.fir.isInner) {
                    replaceDispatchReceiver(singleCandidate.dispatchReceiverExpression())
                }
                replaceContextArguments(singleCandidate.contextArguments())
            }
        }
    }

    private fun createCallableReferencesInfoForLHS(
        callableReferenceAccess: FirCallableReferenceAccess,
        lhs: DoubleColonLHS?,
        expectedType: ConeKotlinType?,
        hasSyntheticOuterCall: Boolean,
    ): CallInfo {
        return CallableReferenceInfo(
            callableReferenceAccess,
            callableReferenceAccess.calleeReference.name,
            callableReferenceAccess.explicitReceiver,
            session,
            components.file,
            transformer.components.containingDeclarations,
            // Additional things for callable reference resolve
            expectedType,
            lhs,
            hasSyntheticOuterCall,
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
        val operatorToken = runIf(callInfo.origin == FirFunctionCallOrigin.Operator) {
            OperatorNameConventions.TOKENS_BY_OPERATOR_NAME[name]
        }

        val diagnostic = when {
            expectedCallKind != null -> {
                fun isValueParametersNotEmpty(candidate: Candidate): Boolean {
                    return (candidate.symbol.fir as? FirFunction)?.valueParameters?.size?.let { it > 0 } ?: false
                }

                when (expectedCallKind) {
                    CallKind.Function -> ConeFunctionCallExpectedError(name, candidates.any { isValueParametersNotEmpty(it) }, candidates)
                    else -> {
                        val singleExpectedCandidate = expectedCandidates?.singleOrNull()

                        var fir = singleExpectedCandidate?.symbol?.fir
                        if (fir is FirTypeAlias) {
                            fir = fir.expandedTypeRef.coneType.fullyExpandedType(session).toRegularClassSymbol(session)?.fir
                        }

                        when (fir) {
                            is FirRegularClass -> {
                                ConeResolutionToClassifierError(singleExpectedCandidate!!, fir.symbol)
                            }
                            else -> {
                                val coneType = explicitReceiver?.resolvedType
                                when {
                                    coneType != null && !coneType.isUnit -> {
                                        ConeFunctionExpectedError(
                                            name.asString(),
                                            (fir as? FirCallableDeclaration)?.let {
                                                components.returnTypeCalculator.tryCalculateReturnType(it)
                                            }?.coneType ?: coneType
                                        )
                                    }
                                    singleExpectedCandidate != null && !singleExpectedCandidate.isSuccessful -> {
                                        createConeDiagnosticForCandidateWithError(
                                            singleExpectedCandidate.lowestApplicability,
                                            singleExpectedCandidate
                                        )
                                    }
                                    else -> ConeUnresolvedNameError(name, operatorToken)
                                }
                            }
                        }
                    }
                }
            }

            candidates.isEmpty() -> {
                when {
                    name.asString() == "invoke" && explicitReceiver is FirLiteralExpression ->
                        ConeFunctionExpectedError(
                            explicitReceiver.value?.toString() ?: "",
                            explicitReceiver.resolvedType,
                        )
                    reference is FirSuperReference && (reference.superTypeRef.firClassLike(session) as? FirClass)?.isInterface == true -> ConeNoConstructorError
                    else -> ConeUnresolvedNameError(name, operatorToken)
                }
            }

            candidates.size > 1 -> ConeAmbiguityError(name, applicability, candidates)

            else -> {
                val candidate = candidates.single()
                runIf(!candidate.isSuccessful) {
                    createConeDiagnosticForCandidateWithError(applicability, candidate)
                }
            }
        }

        if (diagnostic != null) {
            return createErrorReferenceForSingleCandidate(candidates.singleOrNull(), diagnostic, callInfo, source)
        }

        // successful candidate

        val candidate = candidates.single()
        val coneSymbol = candidate.symbol
        if (coneSymbol is FirBackingFieldSymbol) {
            coneSymbol.fir.propertySymbol.fir.isReferredViaField = true
            return buildBackingFieldReference {
                this.source = source
                resolvedSymbol = coneSymbol
            }
        }
        if ((coneSymbol as? FirPropertySymbol)?.hasExplicitBackingField == true) {
            return FirPropertyWithExplicitBackingFieldResolvedNamedReference(
                source, name, candidate.symbol, candidate.hasVisibleBackingField
            )
        }
        /*
         * This `if` is an optimization for local variables and properties without type parameters.
         * Since they have no type variables, so we can don't run completion on them at all and create
         *   resolved reference immediately.
         *
         * But for callable reference resolution (createResolvedReferenceWithoutCandidateForLocalVariables = true)
         *   we should keep candidate, because it was resolved
         *   with special resolution stages, which saved in candidate additional reference info,
         *   like `resultingTypeForCallableReference`.
         *
         * The same is true for builder inference session, because inference from expected type inside lambda
         *   can be important in builder inference mode, and it will never work if we skip completion here.
         * See inferenceFromLambdaReturnStatement.kt test.
         */
        if (!candidate.usedOuterCs &&
            createResolvedReferenceWithoutCandidateForLocalVariables &&
            explicitReceiver?.resolvedType !is ConeIntegerLiteralType &&
            coneSymbol is FirVariableSymbol &&
            (coneSymbol !is FirPropertySymbol || (coneSymbol.fir as FirMemberDeclaration).typeParameters.isEmpty()) &&
            !candidate.doesResolutionResultOverrideOtherToPreserveCompatibility()
        ) {
            return buildResolvedNamedReference {
                this.source = source
                this.name = name
                resolvedSymbol = coneSymbol
            }
        }
        return FirNamedReferenceWithCandidate(source, name, candidate)
    }

    private fun createErrorReferenceForSingleCandidate(
        candidate: Candidate?,
        diagnostic: ConeDiagnostic,
        callInfo: CallInfo,
        source: KtSourceElement?
    ): FirNamedReference {
        if (candidate == null) return buildReferenceWithErrorCandidate(callInfo, diagnostic, source)
        return when (diagnostic) {
            is ConeUnresolvedError, is ConeHiddenCandidateError -> buildReferenceWithErrorCandidate(callInfo, diagnostic, source)
            else -> createErrorReferenceWithExistingCandidate(
                candidate,
                diagnostic,
                source,
                transformer.resolutionContext,
                components.resolutionStageRunner
            )
        }
    }

    private fun buildReferenceWithErrorCandidate(
        callInfo: CallInfo,
        diagnostic: ConeDiagnostic,
        source: KtSourceElement?
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

/** A candidate in the overload candidate set. */
data class OverloadCandidate(val candidate: Candidate, val isInBestCandidates: Boolean)

/** Used for IDE */
class AllCandidatesCollector(
    components: BodyResolveComponents,
    resolutionStageRunner: ResolutionStageRunner
) : CandidateCollector(components, resolutionStageRunner) {
    private val allCandidatesMap = mutableMapOf<FirBasedSymbol<*>, Candidate>()

    override fun consumeCandidate(group: TowerGroup, candidate: Candidate, context: ResolutionContext): CandidateApplicability {
        // Filter duplicate symbols. In the case of typealias constructor calls, we consider the original constructor for uniqueness.
        val key = (candidate.symbol.fir as? FirConstructor)?.typeAliasConstructorInfo?.originalConstructor?.symbol
            ?: candidate.symbol

        // To preserve the behavior of a HashSet which keeps the first added item, we use getOrPut instead of put.
        // Changing this behavior breaks testData/components/callResolver/resolveCandidates/singleCandidate/functionTypeVariableCall_extensionReceiver.kt
        allCandidatesMap.getOrPut(key) { candidate }
        return super.consumeCandidate(group, candidate, context)
    }

    // We want to get candidates at all tower levels.
    override fun shouldStopAtTheGroup(group: TowerGroup): Boolean = false

    val allCandidates: Collection<Candidate>
        get() = allCandidatesMap.values
}
