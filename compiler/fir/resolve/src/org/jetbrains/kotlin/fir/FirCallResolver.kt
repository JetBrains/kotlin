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
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.isReferredViaField
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
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
import org.jetbrains.kotlin.fir.resolve.inference.csBuilder
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.addNonFatalDiagnostic
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.transformers.doesResolutionResultOverrideOtherToPreserveCompatibility
import org.jetbrains.kotlin.fir.scopes.impl.originalConstructorIfTypeAlias
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildStarProjection
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.runTransaction
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.CodeFragmentAdjustment
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
        val resolvedReceiver = functionCall.explicitReceiver
        if (candidate != null && resolvedReceiver is FirResolvedQualifier) {
            resolvedReceiver.replaceResolvedToCompanionObject(candidate.isFromCompanionObjectTypeScope)
        }

        // We need desugaring
        val resultFunctionCall = if (candidate != null && candidate.callInfo != result.info) {
            // This branch support case for the call of the type `a.invoke()`
            // 1. Handle candidate for `a`
            (resolvedReceiver?.toReference(session) as? FirNamedReferenceWithCandidate)?.candidate?.updateSourcesOfReceivers()
            // 2. Handle candidate for `invoke`
            candidate.updateSourcesOfReceivers()
            functionCall.copyAsImplicitInvokeCall {
                explicitReceiver = candidate.callInfo.explicitReceiver
                dispatchReceiver = candidate.dispatchReceiverExpression()
                extensionReceiver = candidate.chosenExtensionReceiverExpression()
                argumentList = candidate.callInfo.argumentList
                contextReceiverArguments.addAll(candidate.contextReceiverArguments())
            }
        } else {
            candidate?.updateSourcesOfReceivers()
            functionCall
        }
        val type = components.typeFromCallee(resultFunctionCall).type
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
        val result =
            collectCandidates(
                qualifiedAccess,
                name,
                forceCallKind = null,
                isUsedAsGetClassReceiver = false,
                origin,
                containingDeclarations,
                resolutionContext,
                collector,
                resolutionMode = resolutionMode
            )
        return collector.allCandidates.map { OverloadCandidate(it, isInBestCandidates = it in result.candidates) }
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
        val typeArguments = (qualifiedAccess as? FirFunctionCall)?.typeArguments.orEmpty()

        val info = CallInfo(
            callSite,
            forceCallKind ?: if (qualifiedAccess is FirFunctionCall) CallKind.Function else CallKind.VariableAccess,
            name,
            explicitReceiver,
            argumentList,
            isImplicitInvoke = qualifiedAccess is FirImplicitInvokeCall,
            isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
            typeArguments,
            session,
            components.file,
            containingDeclarations,
            origin = origin,
            resolutionMode = resolutionMode,
        )
        towerResolver.reset()
        val result = towerResolver.runResolver(info, resolutionContext, collector)

        var (reducedCandidates, newApplicability) = reduceCandidates(result, explicitReceiver, resolutionContext)
        reducedCandidates = overloadByLambdaReturnTypeResolver.reduceCandidates(qualifiedAccess, reducedCandidates, reducedCandidates)

        return ResolutionResult(info, newApplicability ?: result.currentApplicability, reducedCandidates)
    }

    /**
     * Returns a [Pair] consisting of the reduced candidates and the new applicability if it has changed and `null` otherwise.
     */
    private fun reduceCandidates(
        collector: CandidateCollector,
        explicitReceiver: FirExpression? = null,
        resolutionContext: ResolutionContext = transformer.resolutionContext,
    ): Pair<Set<Candidate>, CandidateApplicability?> {
        fun chooseMostSpecific(list: List<Candidate>): Set<Candidate> {
            val onSuperReference = (explicitReceiver as? FirQualifiedAccessExpression)?.calleeReference is FirSuperReference
            return conflictResolver.chooseMaximallySpecificCandidates(list, discriminateAbstracts = onSuperReference)
        }

        val candidates = collector.bestCandidates()

        if (collector.currentApplicability.isSuccess) {
            return chooseMostSpecific(candidates) to null
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

        return candidates.toSet() to null
    }

    fun resolveVariableAccessAndSelectCandidate(
        qualifiedAccess: FirQualifiedAccessExpression,
        isUsedAsReceiver: Boolean,
        isUsedAsGetClassReceiver: Boolean,
        callSite: FirElement,
        resolutionMode: ResolutionMode,
    ): FirStatement {
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
    ): FirStatement {
        val callee = qualifiedAccess.calleeReference as? FirSimpleNamedReference ?: return qualifiedAccess

        @Suppress("NAME_SHADOWING")
        val qualifiedAccess = qualifiedAccess.let(transformer::transformExplicitReceiver)
        val nonFatalDiagnosticFromExpression = (qualifiedAccess as? FirPropertyAccessExpression)?.nonFatalDiagnostics

        val basicResult by lazy(LazyThreadSafetyMode.NONE) {
            collectCandidates(qualifiedAccess, callee.name, isUsedAsGetClassReceiver = isUsedAsGetClassReceiver, callSite = callSite, resolutionMode = resolutionMode)
        }

        // Even if it's not receiver, it makes sense to continue qualifier if resolution is unsuccessful
        // just to try to resolve to package/class and then report meaningful error at FirStandaloneQualifierChecker
        if (isUsedAsReceiver || !basicResult.applicability.isSuccess) {
            (qualifiedAccess.explicitReceiver as? FirResolvedQualifier)
                ?.continueQualifier(
                    callee,
                    qualifiedAccess,
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
                    callee, qualifiedAccess, nonFatalDiagnosticFromExpression,
                )?.let { return it }
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

        (qualifiedAccess.explicitReceiver as? FirResolvedQualifier)?.replaceResolvedToCompanionObject(
            reducedCandidates.isNotEmpty() && reducedCandidates.all { it.isFromCompanionObjectTypeScope }
        )

        when {
            referencedSymbol is FirClassLikeSymbol<*> -> {
                val extraDiagnostic =
                    runIf(reducedCandidates.singleOrNull()?.doesResolutionResultOverrideOtherToPreserveCompatibility() == true) {
                        ConeResolutionResultOverridesOtherToPreserveCompatibility
                    }
                val nonFatalDiagnosticFromExpressionWithExtra = when {
                    nonFatalDiagnosticFromExpression != null -> nonFatalDiagnosticFromExpression + listOfNotNull(extraDiagnostic)
                    extraDiagnostic == null -> null
                    else -> listOf(extraDiagnostic)
                }
                return components.buildResolvedQualifierForClass(
                    referencedSymbol,
                    qualifiedAccess.source,
                    qualifiedAccess.typeArguments,
                    diagnostic ?: extractNestedClassAccessDiagnostic(nameReference.source, qualifiedAccess.explicitReceiver, referencedSymbol),
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
            referencedSymbol is FirTypeParameterSymbol && referencedSymbol.fir.isReified -> {
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
                replaceContextReceiverArguments(candidate.contextReceiverArguments())

                if (CallToDeprecatedOverrideOfHidden in candidate.diagnostics) {
                    addNonFatalDiagnostic(ConeCallToDeprecatedOverrideOfHidden)
                }
            }
        }
        transformer.storeTypeFromCallee(qualifiedAccess, isLhsOfAssignment = callSite is FirVariableAssignment)
        return qualifiedAccess
    }

    fun resolveCallableReference(
        containingCallCandidate: Candidate,
        resolvedCallableReferenceAtom: ResolvedCallableReferenceAtom,
        hasSyntheticOuterCall: Boolean,
    ): Pair<CandidateApplicability, Boolean> = components.context.inferenceSession.runCallableReferenceResolution(containingCallCandidate) {
        val constraintSystemBuilder = containingCallCandidate.csBuilder
        val callableReferenceAccess = resolvedCallableReferenceAtom.reference
        val calleeReference = callableReferenceAccess.calleeReference
        val lhs = resolvedCallableReferenceAtom.lhs
        val coneSubstitutor = constraintSystemBuilder.buildCurrentSubstitutor() as ConeSubstitutor
        val expectedType = resolvedCallableReferenceAtom.expectedType?.let(coneSubstitutor::substituteOrSelf)

        val info = createCallableReferencesInfoForLHS(
            callableReferenceAccess, lhs, expectedType, constraintSystemBuilder, hasSyntheticOuterCall
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

        val (reducedCandidates, newApplicability) = reduceCandidates(result, callableReferenceAccess.explicitReceiver)
        val nonEmptyAndAllSuccessful = reducedCandidates.isNotEmpty() && reducedCandidates.all { it.isSuccessful }
        val applicability = newApplicability ?: result.currentApplicability

        (callableReferenceAccess.explicitReceiver as? FirResolvedQualifier)?.replaceResolvedToCompanionObject(
            reducedCandidates.isNotEmpty() && reducedCandidates.all { it.isFromCompanionObjectTypeScope }
        )

        resolvedCallableReferenceAtom.hasBeenResolvedOnce = true

        when {
            !nonEmptyAndAllSuccessful -> {
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
                resolvedCallableReferenceAtom.resultingReference = errorReference
                return@runCallableReferenceResolution applicability to false
            }
            reducedCandidates.size > 1 -> {
                if (resolvedCallableReferenceAtom.hasBeenPostponed) {
                    val errorReference = buildReferenceWithErrorCandidate(
                        info,
                        ConeAmbiguityError(info.name, applicability, reducedCandidates),
                        calleeReference.source
                    )
                    resolvedCallableReferenceAtom.resultingReference = errorReference
                    return@runCallableReferenceResolution applicability to false
                }
                resolvedCallableReferenceAtom.hasBeenPostponed = true
                return@runCallableReferenceResolution applicability to true
            }
        }

        val chosenCandidate = reducedCandidates.single()
        chosenCandidate.updateSourcesOfReceivers()

        constraintSystemBuilder.runTransaction {
            chosenCandidate.outerConstraintBuilderEffect!!(this)
            true
        }

        val reference = createResolvedNamedReference(
            calleeReference,
            info.name,
            info,
            reducedCandidates,
            applicability,
            createResolvedReferenceWithoutCandidateForLocalVariables = false
        )
        resolvedCallableReferenceAtom.resultingReference = reference
        resolvedCallableReferenceAtom.resultingTypeForCallableReference = chosenCandidate.resultingTypeForCallableReference

        return@runCallableReferenceResolution applicability to true
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
            isUsedAsGetClassReceiver = false,
            typeArguments = typeArguments,
            session,
            components.file,
            components.containingDeclarations,
            resolutionMode = ResolutionMode.ContextIndependent,
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

        return selectDelegatingConstructorCall(delegatedConstructorCall, name, result, callInfo)
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
        val annotationClassSymbol = annotation.getCorrespondingClassSymbolOrNull(session)
        val resolvedReference = if (annotationClassSymbol != null && annotationClassSymbol.fir.classKind == ClassKind.ANNOTATION_CLASS) {
            val constructorSymbol = getConstructorSymbol(annotationClassSymbol)
            constructorSymbol?.lazyResolveToPhase(FirResolvePhase.TYPES)

            if (constructorSymbol != null && annotation.arguments.isNotEmpty()) {
                // We want to "desugar" array literal arguments to arrayOf, intArrayOf, floatArrayOf and other *arrayOf* calls
                // so that we can properly complete them eventually.
                // In order to find out what the expected type is, we need to run argument mapping.
                // Array literals can be nested despite the fact they are not supported in annotation arguments.
                // But we should traverse them all recursively to report type mismatches.
                // For nested array literal, we need a new expected type obtained from the previous expected type (extract type of array element).
                // We don't want to force full completion before the whole call is completed so that type variables are preserved.
                // But we need to pass expectType to figure out the correct *arrayOf* function (because Array<T> and primitive arrays can't be matched).
                val mapping = transformer.resolutionContext.bodyResolveComponents.mapArguments(
                    annotation.arguments, constructorSymbol.fir, originScope = null, callSiteIsOperatorCall = false,
                )
                val argumentsToParameters = mapping.toArgumentToParameterMapping()

                fun FirCall.transformArgumentList(getExpectedType: (FirExpression) -> FirTypeRef?) {
                    replaceArgumentList(
                        buildArgumentList {
                            source = argumentList.source
                            argumentList.arguments.mapTo(arguments) { arg ->
                                val unwrappedArgument = arg.unwrapArgument()
                                val expectedType = getExpectedType(arg)
                                val resolutionMode = if (unwrappedArgument is FirArrayLiteral && expectedType is FirResolvedTypeRef) {
                                    unwrappedArgument.transformArgumentList {
                                        // Trying to extract expected type for the next nested array literal
                                        expectedType.coneType.arrayElementType()?.toFirResolvedTypeRef()
                                    }

                                    // Enabling expectedTypeMismatchIsReportedInChecker clarifies error messages:
                                    // It will be reported single ARGUMENT_TYPE_MISMATCH on the array literal in checkApplicabilityForArgumentType
                                    // instead of several TYPE_MISMATCH for every mismatched argument.
                                    ResolutionMode.WithExpectedType(
                                        expectedType,
                                        forceFullCompletion = false,
                                        expectedTypeMismatchIsReportedInChecker = true
                                    )
                                } else {
                                    ResolutionMode.ContextDependent
                                }

                                return@mapTo arg.transformSingle(transformer, resolutionMode)
                            }
                        }
                    )
                }

                annotation.transformArgumentList { argumentsToParameters[it]?.returnTypeRef }
            } else {
                annotation.replaceArgumentList(annotation.argumentList.transform(transformer, ResolutionMode.ContextDependent))
            }

            val callInfo = toCallInfo(annotation, reference)

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
                if (annotationClassSymbol != null) ConeIllegalAnnotationError(reference.name)
                //calleeReference and annotationTypeRef are both error nodes so we need to avoid doubling of the diagnostic report
                else ConeStubDiagnostic(
                    //prefer diagnostic with symbol, e.g. to use the symbol during navigation in IDE
                    (annotation.resolvedType as? ConeErrorType)?.diagnostic as? ConeDiagnosticWithSymbol<*>
                        ?: ConeUnresolvedNameError(reference.name)),
                reference.source
            )
        }

        return annotation.apply {
            replaceCalleeReference(resolvedReference)
        }
    }

    private fun toCallInfo(annotation: FirAnnotationCall, reference: FirSimpleNamedReference): CallInfo = CallInfo(
        annotation,
        CallKind.Function,
        name = reference.name,
        explicitReceiver = null,
        annotation.argumentList,
        isImplicitInvoke = false,
        isUsedAsGetClassReceiver = false,
        typeArguments = annotation.typeArguments,
        session,
        components.file,
        components.containingDeclarations,
        resolutionMode = ResolutionMode.ContextIndependent,
    )

    private fun getConstructorSymbol(annotationClassSymbol: FirRegularClassSymbol): FirConstructorSymbol? {
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
        return constructorSymbol
    }

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
        val (reducedCandidates, newApplicability) = reduceCandidates(result)

        val nameReference = createResolvedNamedReference(
            call.calleeReference,
            name,
            callInfo,
            reducedCandidates,
            newApplicability ?: result.currentApplicability,
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
                replaceContextReceiverArguments(singleCandidate.contextReceiverArguments())
            }
        }
    }

    private fun createCallableReferencesInfoForLHS(
        callableReferenceAccess: FirCallableReferenceAccess,
        lhs: DoubleColonLHS?,
        expectedType: ConeKotlinType?,
        outerConstraintSystemBuilder: ConstraintSystemBuilder?,
        hasSyntheticOuterCall: Boolean,
    ): CallInfo {
        return CallInfo(
            callableReferenceAccess,
            CallKind.CallableReference,
            callableReferenceAccess.calleeReference.name,
            callableReferenceAccess.explicitReceiver,
            FirEmptyArgumentList,
            isImplicitInvoke = false,
            isUsedAsGetClassReceiver = false,
            emptyList(),
            session,
            components.file,
            transformer.components.containingDeclarations,
            candidateForCommonInvokeReceiver = null,
            resolutionMode = ResolutionMode.ContextIndependent,
            // Additional things for callable reference resolve
            expectedType,
            outerConstraintSystemBuilder,
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
                            fir = (fir.expandedTypeRef.coneType.fullyExpandedType(session).toSymbol(session) as? FirRegularClassSymbol)?.fir
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
                                    singleExpectedCandidate != null && !singleExpectedCandidate.currentApplicability.isSuccess -> {
                                        createConeDiagnosticForCandidateWithError(
                                            singleExpectedCandidate.currentApplicability,
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
                    name.asString() == "invoke" && explicitReceiver is FirLiteralExpression<*> ->
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
                    if (needTreatErrorCandidateAsResolved(candidate)) {
                        @OptIn(CodeFragmentAdjustment::class)
                        candidate.resetToResolved()
                        null
                    } else {
                        createConeDiagnosticForCandidateWithError(applicability, candidate)
                    }
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

    private fun needTreatErrorCandidateAsResolved(candidate: Candidate): Boolean {
        return if (candidate.isCodeFragmentVisibilityError) {
            components.resolutionStageRunner.fullyProcessCandidate(candidate, transformer.resolutionContext)
            candidate.diagnostics.all { it.applicability.isSuccess || it.applicability == CandidateApplicability.K2_VISIBILITY_ERROR }
        } else false
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
        val key = (candidate.symbol.fir as? FirConstructor)?.originalConstructorIfTypeAlias?.symbol
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
