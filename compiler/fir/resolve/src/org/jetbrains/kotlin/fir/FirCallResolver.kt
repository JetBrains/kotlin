/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasExplicitBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isReferredViaField
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeStubDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedReifiedParameterReference
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.references.builder.buildBackingFieldReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.calls.tower.FirTowerResolver
import org.jetbrains.kotlin.fir.resolve.calls.tower.TowerGroup
import org.jetbrains.kotlin.fir.resolve.calls.tower.TowerResolveManager
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.inference.ResolvedCallableReferenceAtom
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildStarProjection
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.runTransaction
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.Variance

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

    fun resolveCallAndSelectCandidate(functionCall: FirFunctionCall): FirFunctionCall {
        val name = functionCall.calleeReference.name
        val result = collectCandidates(functionCall, name, origin = functionCall.origin)

        var forceCandidates: Collection<Candidate>? = null
        if (result.candidates.isEmpty()) {
            val newResult = collectCandidates(functionCall, name, CallKind.VariableAccess, origin = functionCall.origin)
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
        val resolvedReceiver = functionCall.explicitReceiver
        if (candidate != null && resolvedReceiver is FirResolvedQualifier) {
            resolvedReceiver.replaceResolvedToCompanionObject(candidate.isFromCompanionObjectTypeScope)
        }

        // We need desugaring
        val resultFunctionCall = if (candidate != null && candidate.callInfo != result.info) {
            functionCall.copyAsImplicitInvokeCall {
                explicitReceiver = candidate.callInfo.explicitReceiver
                dispatchReceiver = candidate.dispatchReceiverExpression()
                extensionReceiver = candidate.chosenExtensionReceiverExpression()
                argumentList = candidate.callInfo.argumentList
                contextReceiverArguments.addAll(candidate.contextReceiverArguments())
            }
        } else {
            functionCall
        }
        val typeRef = components.typeFromCallee(resultFunctionCall)
        if (typeRef.type is ConeErrorType) {
            resultFunctionCall.resultType = typeRef
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
        resolutionContext: ResolutionContext = transformer.resolutionContext
    ): List<OverloadCandidate> {
        val collector = AllCandidatesCollector(components, components.resolutionStageRunner)
        val origin = (qualifiedAccess as? FirFunctionCall)?.origin ?: FirFunctionCallOrigin.Regular
        val result =
            collectCandidates(qualifiedAccess, name, forceCallKind = null, origin, containingDeclarations, resolutionContext, collector)
        return collector.allCandidates.map { OverloadCandidate(it, isInBestCandidates = it in result.candidates) }
    }

    private fun collectCandidates(
        qualifiedAccess: FirQualifiedAccessExpression,
        name: Name,
        forceCallKind: CallKind? = null,
        origin: FirFunctionCallOrigin = FirFunctionCallOrigin.Regular,
        containingDeclarations: List<FirDeclaration> = transformer.components.containingDeclarations,
        resolutionContext: ResolutionContext = transformer.resolutionContext,
        collector: CandidateCollector? = null,
        callSite: FirElement = qualifiedAccess,
    ): ResolutionResult {
        val explicitReceiver = qualifiedAccess.explicitReceiver
        val argumentList = (qualifiedAccess as? FirFunctionCall)?.argumentList ?: FirEmptyArgumentList
        val typeArguments = (qualifiedAccess as? FirFunctionCall)?.typeArguments.orEmpty()

        val info = CallInfo(
            callSite,
            forceCallKind ?: if (qualifiedAccess is FirFunctionCall) CallKind.Function else CallKind.VariableAccess,
            name,
            explicitReceiver,
            argumentList,
            isImplicitInvoke = qualifiedAccess is FirImplicitInvokeCall,
            typeArguments,
            session,
            components.file,
            containingDeclarations,
            origin = origin
        )
        towerResolver.reset()
        val result = towerResolver.runResolver(info, resolutionContext, collector)
        val bestCandidates = result.bestCandidates()

        var reducedCandidates = reduceCandidates(bestCandidates, explicitReceiver, resolutionContext, result.currentApplicability.isSuccess)
        reducedCandidates = overloadByLambdaReturnTypeResolver.reduceCandidates(qualifiedAccess, bestCandidates, reducedCandidates)

        return ResolutionResult(info, result.currentApplicability, reducedCandidates)
    }

    private fun reduceCandidates(
        candidates: List<Candidate>,
        explicitReceiver: FirExpression?,
        resolutionContext: ResolutionContext,
        isSuccess: Boolean,
    ): Set<Candidate> {
        fun chooseMostSpecific(list: List<Candidate>): Set<Candidate> {
            val onSuperReference = (explicitReceiver as? FirQualifiedAccessExpression)?.calleeReference is FirSuperReference
            return conflictResolver.chooseMaximallySpecificCandidates(list, discriminateAbstracts = onSuperReference)
        }

        if (isSuccess) {
            return chooseMostSpecific(candidates)
        }

        val singleApplicability = candidates.mapTo(mutableSetOf()) { it.currentApplicability }.singleOrNull()

        if (singleApplicability == null || singleApplicability <= CandidateApplicability.INAPPLICABLE) {
            return candidates.toSet()
        }

        // If all candidates have the same kind on inapplicability - try to choose the most specific one
        if (candidates.size > 1) {
            // We have multiple candidates with the same inapplicability. We want to select the "least bad" candidates.

            // First, fully process all of them and group them by their worst applicability.
            val groupedByDiagnosticCount = candidates.groupBy {
                components.resolutionStageRunner.fullyProcessCandidate(it, resolutionContext)
                it.diagnostics.minOf(ResolutionDiagnostic::applicability)
            }

            // Then, select the group with the best worst applicability.
            groupedByDiagnosticCount.maxBy { it.key }.let {
                return chooseMostSpecific(it.value)
            }
        }

        return chooseMostSpecific(candidates)
    }

    fun resolveVariableAccessAndSelectCandidate(
        qualifiedAccess: FirQualifiedAccessExpression,
        isUsedAsReceiver: Boolean,
        callSite: FirElement,
    ): FirStatement {
        return resolveVariableAccessAndSelectCandidateImpl(qualifiedAccess, isUsedAsReceiver, callSite) { true }
    }

    fun resolveOnlyEnumOrQualifierAccessAndSelectCandidate(
        qualifiedAccess: FirQualifiedAccessExpression,
        isUsedAsReceiver: Boolean,
    ): FirStatement {
        return resolveVariableAccessAndSelectCandidateImpl(qualifiedAccess, isUsedAsReceiver) accept@{ candidates ->
            val symbol = candidates.singleOrNull()?.symbol ?: return@accept false
            symbol is FirEnumEntrySymbol || symbol is FirRegularClassSymbol
        }
    }

    private fun resolveVariableAccessAndSelectCandidateImpl(
        qualifiedAccess: FirQualifiedAccessExpression,
        isUsedAsReceiver: Boolean,
        callSite: FirElement = qualifiedAccess,
        acceptCandidates: (Collection<Candidate>) -> Boolean,
    ): FirStatement {
        val callee = qualifiedAccess.calleeReference as? FirSimpleNamedReference ?: return qualifiedAccess

        @Suppress("NAME_SHADOWING")
        val qualifiedAccess = qualifiedAccess.let(transformer::transformExplicitReceiver)
        val nonFatalDiagnosticFromExpression = (qualifiedAccess as? FirPropertyAccessExpression)?.nonFatalDiagnostics

        val basicResult by lazy(LazyThreadSafetyMode.NONE) {
            collectCandidates(qualifiedAccess, callee.name, callSite = callSite)
        }

        // Even if it's not receiver, it makes sense to continue qualifier if resolution is unsuccessful
        // just to try to resolve to package/class and then report meaningful error at FirStandaloneQualifierChecker
        if (isUsedAsReceiver || !basicResult.applicability.isSuccess) {
            (qualifiedAccess.explicitReceiver as? FirResolvedQualifier)
                ?.continueQualifier(
                    callee,
                    qualifiedAccess.source,
                    qualifiedAccess.typeArguments,
                    nonFatalDiagnosticFromExpression,
                    session,
                    components
                )?.let { return it }
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
            if (!result.applicability.isSuccess || (isUsedAsReceiver && result.candidates.all { it.symbol is FirClassLikeSymbol })) {
                components.resolveRootPartOfQualifier(
                    callee, qualifiedAccess.source, qualifiedAccess.typeArguments, nonFatalDiagnosticFromExpression,
                )?.let { return it }
            }
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

        (qualifiedAccess.explicitReceiver as? FirResolvedQualifier)?.replaceResolvedToCompanionObject(
            reducedCandidates.isNotEmpty() && reducedCandidates.all { it.isFromCompanionObjectTypeScope }
        )

        when {
            referencedSymbol is FirClassLikeSymbol<*> -> {
                return components.buildResolvedQualifierForClass(
                    referencedSymbol,
                    qualifiedAccess.source,
                    qualifiedAccess.typeArguments,
                    diagnostic,
                    nonFatalDiagnostics = extractNonFatalDiagnostics(
                        nameReference.source,
                        qualifiedAccess.explicitReceiver,
                        referencedSymbol,
                        nonFatalDiagnosticFromExpression,
                        session.languageVersionSettings.apiVersion
                    ),
                    annotations = qualifiedAccess.annotations
                )
            }
            referencedSymbol is FirTypeParameterSymbol && referencedSymbol.fir.isReified -> {
                return buildResolvedReifiedParameterReference {
                    source = nameReference.source
                    symbol = referencedSymbol
                    typeRef = typeForReifiedParameterReference(this)
                }
            }
        }

        qualifiedAccess.replaceCalleeReference(nameReference)
        if (reducedCandidates.size == 1) {
            val candidate = reducedCandidates.single()
            qualifiedAccess.apply {
                replaceDispatchReceiver(candidate.dispatchReceiverExpression())
                replaceExtensionReceiver(candidate.chosenExtensionReceiverExpression())
                replaceContextReceiverArguments(candidate.contextReceiverArguments())
            }
        }
        transformer.storeTypeFromCallee(qualifiedAccess, isLhsOfAssignment = callSite is FirVariableAssignment)
        return qualifiedAccess
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
            conflictResolver.chooseMaximallySpecificCandidates(bestCandidates)
        }

        (callableReferenceAccess.explicitReceiver as? FirResolvedQualifier)?.replaceResolvedToCompanionObject(
            bestCandidates.isNotEmpty() && bestCandidates.all { it.isFromCompanionObjectTypeScope }
        )

        resolvedCallableReferenceAtom.hasBeenResolvedOnce = true

        when {
            noSuccessfulCandidates -> {
                val errorReference = buildReferenceWithErrorCandidate(
                    info,
                    if (applicability == CandidateApplicability.K2_UNSUPPORTED) {
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
                    val errorReference = buildReferenceWithErrorCandidate(
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
        constructedType: ConeClassLikeType?,
        derivedClassLookupTag: ConeClassLikeLookupTag
    ): FirDelegatedConstructorCall {
        val name = SpecialNames.INIT
        val symbol = constructedType?.lookupTag?.toSymbol(components.session)
        val typeArguments = constructedType?.typeArguments
            ?.take((symbol?.fir as? FirRegularClass)?.typeParameters?.count { it is FirTypeParameter } ?: 0)
            ?.map { it.toFirTypeProjection() }
            ?: emptyList()

        val callInfo = CallInfo(
            delegatedConstructorCall,
            CallKind.DelegatingConstructorCall,
            name,
            explicitReceiver = null,
            delegatedConstructorCall.argumentList,
            isImplicitInvoke = false,
            typeArguments = typeArguments,
            session,
            components.file,
            components.containingDeclarations,
        )
        towerResolver.reset()

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

    fun resolveAnnotationCall(annotation: FirAnnotationCall): FirAnnotationCall? {
        val reference = annotation.calleeReference as? FirSimpleNamedReference ?: return null
        annotation.replaceArgumentList(annotation.argumentList.transform(transformer, ResolutionMode.ContextDependent))

        val callInfo = CallInfo(
            annotation,
            CallKind.Function,
            name = reference.name,
            explicitReceiver = null,
            annotation.argumentList,
            isImplicitInvoke = false,
            typeArguments = annotation.typeArguments,
            session,
            components.file,
            components.containingDeclarations
        )

        val annotationClassSymbol = annotation.getCorrespondingClassSymbolOrNull(session)
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
            buildReferenceWithErrorCandidate(
                callInfo,
                if (annotationClassSymbol != null) ConeIllegalAnnotationError(reference.name)
                //calleeReference and annotationTypeRef are both error nodes so we need to avoid doubling of the diagnostic report
                else ConeStubDiagnostic(ConeUnresolvedNameError(reference.name)),
                reference.source
            )
        }

        return annotation.apply {
            replaceCalleeReference(resolvedReference)
        }
    }

    private fun createCandidateForAnnotationCall(
        annotationClassSymbol: FirRegularClassSymbol,
        callInfo: CallInfo
    ): ResolutionResult? {
        var constructorSymbol: FirConstructorSymbol? = null
        annotationClassSymbol.fir.unsubstitutedScope(
            session,
            components.scopeSession,
            withForcedTypeCalculator = false,
            memberRequiredPhase = null,
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

    private fun selectDelegatingConstructorCall(
        call: FirDelegatedConstructorCall, name: Name, result: CandidateCollector, callInfo: CallInfo
    ): FirDelegatedConstructorCall {
        val bestCandidates = result.bestCandidates()
        val reducedCandidates = if (!result.currentApplicability.isSuccess) {
            bestCandidates.toSet()
        } else {
            conflictResolver.chooseMaximallySpecificCandidates(bestCandidates)
        }

        val nameReference = createResolvedNamedReference(
            call.calleeReference,
            name,
            callInfo,
            reducedCandidates,
            result.currentApplicability,
        )

        return call.apply {
            call.replaceCalleeReference(nameReference)
            val singleCandidate = reducedCandidates.singleOrNull()
            if (singleCandidate != null) {
                val symbol = singleCandidate.symbol
                if (symbol is FirConstructorSymbol && symbol.fir.isInner) {
                    replaceDispatchReceiver(singleCandidate.dispatchReceiverExpression())
                }
                replaceContextReceiverArguments(singleCandidate.contextReceiverArguments())
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
                            fir = (fir.expandedTypeRef.coneType.fullyExpandedType(session).toSymbol(session) as? FirRegularClassSymbol)?.fir
                        }

                        when (fir) {
                            is FirRegularClass -> {
                                ConeResolutionToClassifierError(singleExpectedCandidate!!, fir.symbol)
                            }
                            else -> {
                                val coneType = explicitReceiver?.typeRef?.coneType
                                when {
                                    coneType != null && !coneType.isUnit -> {
                                        ConeFunctionExpectedError(
                                            name.asString(),
                                            (fir as? FirCallableDeclaration)?.returnTypeRef?.coneType ?: coneType
                                        )
                                    }
                                    singleExpectedCandidate != null && !singleExpectedCandidate.currentApplicability.isSuccess -> {
                                        createConeDiagnosticForCandidateWithError(
                                            singleExpectedCandidate.currentApplicability,
                                            singleExpectedCandidate
                                        )
                                    }
                                    else -> ConeUnresolvedNameError(name)
                                }
                            }
                        }
                    }
                }
            }

            candidates.isEmpty() -> {
                if (name.asString() == "invoke" && explicitReceiver is FirConstExpression<*>) {
                    ConeFunctionExpectedError(explicitReceiver.value?.toString() ?: "", explicitReceiver.typeRef.coneType)
                } else {
                    ConeUnresolvedNameError(name)
                }
            }

            candidates.size > 1 -> ConeAmbiguityError(name, applicability, candidates)

            !applicability.isSuccess -> {
                val candidate = candidates.single()
                createConeDiagnosticForCandidateWithError(applicability, candidate)
            }

            else -> null
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

class AllCandidatesCollector(
    components: BodyResolveComponents,
    resolutionStageRunner: ResolutionStageRunner
) : CandidateCollector(components, resolutionStageRunner) {
    private val allCandidatesSet = mutableSetOf<Candidate>()

    override fun consumeCandidate(group: TowerGroup, candidate: Candidate, context: ResolutionContext): CandidateApplicability {
        allCandidatesSet += candidate
        return super.consumeCandidate(group, candidate, context)
    }

    // We want to get candidates at all tower levels.
    override fun shouldStopAtTheGroup(group: TowerGroup): Boolean = false

    val allCandidates: List<Candidate>
        get() = allCandidatesSet.toList()
}
