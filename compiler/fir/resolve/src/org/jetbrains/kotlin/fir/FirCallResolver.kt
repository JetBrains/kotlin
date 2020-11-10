/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
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
import org.jetbrains.kotlin.fir.resolve.inference.*
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
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.resolve.descriptorUtil.OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.same

class FirCallResolver(
    private val components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val qualifiedResolver: FirQualifiedNameResolver,
) {
    private val session = components.session

    private lateinit var transformer: FirExpressionsResolveTransformer

    fun initTransformer(transformer: FirExpressionsResolveTransformer) {
        this.transformer = transformer
    }

    private val towerResolver = FirTowerResolver(
        components, components.resolutionStageRunner,
    )

    private val conflictResolver: ConeCallConflictResolver =
        session.callConflictResolverFactory.create(TypeSpecificityComparator.NONE, session.inferenceComponents)

    @PrivateForInline
    var needTransformArguments: Boolean = true

    @OptIn(PrivateForInline::class)
    fun resolveCallAndSelectCandidate(functionCall: FirFunctionCall): FirFunctionCall {
        qualifiedResolver.reset()
        @Suppress("NAME_SHADOWING")
        val functionCall = if (needTransformArguments) {
            functionCall.transformExplicitReceiver()
                .also {
                    components.dataFlowAnalyzer.enterQualifiedAccessExpression()
                    functionCall.argumentList.transformArguments(transformer, ResolutionMode.ContextDependent)
                }
        } else {
            functionCall
        }

        val name = functionCall.calleeReference.name
        val result = collectCandidates(functionCall, name)

        val nameReference = createResolvedNamedReference(
            functionCall.calleeReference,
            name,
            result.info,
            result.candidates,
            result.applicability,
            functionCall.explicitReceiver,
        )

        val resultExpression = functionCall.transformCalleeReference(StoreNameReference, nameReference)
        val candidate = (nameReference as? FirNamedReferenceWithCandidate)?.candidate

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
        return resultFunctionCall
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

    private fun <T : FirQualifiedAccess> collectCandidates(qualifiedAccess: T, name: Name): ResolutionResult {
        val explicitReceiver = qualifiedAccess.explicitReceiver
        val argumentList = (qualifiedAccess as? FirFunctionCall)?.argumentList ?: FirEmptyArgumentList
        val typeArguments = (qualifiedAccess as? FirFunctionCall)?.typeArguments.orEmpty()

        val info = CallInfo(
            if (qualifiedAccess is FirFunctionCall) CallKind.Function else CallKind.VariableAccess,
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
        var reducedCandidates = if (!result.currentApplicability.isSuccess) {
            bestCandidates.toSet()
        } else {
            val onSuperReference = (explicitReceiver as? FirQualifiedAccessExpression)?.calleeReference is FirSuperReference
            conflictResolver.chooseMaximallySpecificCandidates(
                bestCandidates, discriminateGenerics = true, discriminateAbstracts = onSuperReference
            )
        }

        if (
            reducedCandidates.size > 1 &&
            session.languageVersionSettings.supportsFeature(LanguageFeature.OverloadResolutionByLambdaReturnType)
        ) {
            /*
             * Inference session may look into candidate of call, and for that it uses callee reference.
             * So we need replace reference with proper candidate before calling inference session
             */
            val shouldRunCompletion = if (components.context.inferenceSession != FirInferenceSession.DEFAULT) {
                var shouldRunCompletion = true
                val originalReference = qualifiedAccess.calleeReference
                val inferenceSession = components.context.inferenceSession
                for (candidate in bestCandidates) {
                    qualifiedAccess.replaceCalleeReference(FirNamedReferenceWithCandidate(null, candidate.callInfo.name, candidate))
                    shouldRunCompletion = shouldRunCompletion && inferenceSession.shouldRunCompletion(qualifiedAccess)
                    if (!shouldRunCompletion) break
                }
                qualifiedAccess.replaceCalleeReference(originalReference)
                shouldRunCompletion
            } else {
                true
            }
            if (shouldRunCompletion) {
                val newCandidates = chooseCandidateRegardingOverloadResolutionByLambdaReturnType(
                    qualifiedAccess,
                    reducedCandidates,
                    bestCandidates
                )
                if (newCandidates != null) {
                    reducedCandidates = newCandidates
                }
            }
        }

        return ResolutionResult(info, result.currentApplicability, reducedCandidates)
    }

    private fun <T> chooseCandidateRegardingOverloadResolutionByLambdaReturnType(
        call: T,
        reducedCandidates: Set<Candidate>,
        allCandidates: Collection<Candidate>
    ): Set<Candidate>? where T : FirResolvable, T : FirStatement {
        val candidatesWithAnnotation = allCandidates.filter { candidate ->
            (candidate.symbol.fir as FirAnnotationContainer).annotations.any {
                it.annotationTypeRef.coneType.classId == OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION_CLASS_ID
            }
        }
        if (candidatesWithAnnotation.isEmpty()) return null
        val candidatesWithoutAnnotation = reducedCandidates - candidatesWithAnnotation
        val newCandidates = analyzeLambdaAndReduceNumberOfCandidatesRegardingOverloadResolutionByLambdaReturnType(call, reducedCandidates) ?: return null
        var maximallySpecificCandidates = conflictResolver.chooseMaximallySpecificCandidates(newCandidates, discriminateGenerics = true, discriminateAbstracts = false)
        if (maximallySpecificCandidates.size > 1 && candidatesWithoutAnnotation.any { it in maximallySpecificCandidates }) {
            maximallySpecificCandidates = maximallySpecificCandidates.toMutableSet().apply { removeAll(candidatesWithAnnotation) }
            maximallySpecificCandidates.singleOrNull()?.addDiagnostic(CandidateChosenUsingOverloadResolutionByLambdaAnnotation)
        }
        return maximallySpecificCandidates
    }

    private fun <T> analyzeLambdaAndReduceNumberOfCandidatesRegardingOverloadResolutionByLambdaReturnType(
        call: T,
        candidates: Set<Candidate>,
    ): Set<Candidate>? where T : FirResolvable, T : FirStatement {
        val lambdas = candidates.flatMap { candidate ->
            candidate.postponedAtoms
                .filter { it is ResolvedLambdaAtom && !it.analyzed }
                .map { candidate to it as ResolvedLambdaAtom }
        }.groupBy { (_, atom) -> atom.atom }
            .values.singleOrNull()?.toMap() ?: return null

        if (!lambdas.values.same { it.parameters.size }) return null
        if (!lambdas.values.all { it.expectedType?.isBuiltinFunctionalType(session) == true }) return null

        val originalCalleeReference = call.calleeReference

        for (candidate in lambdas.keys) {
            call.replaceCalleeReference(FirNamedReferenceWithCandidate(null, candidate.callInfo.name, candidate))
            components.callCompleter.runCompletionForCall(
                candidate,
                ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA,
                call,
                components.initialTypeOfCandidate(candidate)
            )
        }

        try {
            val inputTypesAreSame = lambdas.entries.same { (candidate, lambda) ->
                val substitutor = candidate.system.buildCurrentSubstitutor() as ConeSubstitutor
                lambda.inputTypes.map { substitutor.substituteOrSelf(it) }
            }
            if (!inputTypesAreSame) return null
            lambdas.entries.forEach { (candidate, atom) ->
                components.callCompleter.prepareLambdaAtomForFactoryPattern(atom, candidate)
            }
            val iterator = lambdas.entries.iterator()
            val (firstCandidate, firstAtom) = iterator.next()

            val postponedArgumentsAnalyzer = components.callCompleter.createPostponedArgumentsAnalyzer(transformer.resolutionContext)

            call.replaceCalleeReference(FirNamedReferenceWithCandidate(null, firstCandidate.callInfo.name, firstCandidate))
            val results = postponedArgumentsAnalyzer.analyzeLambda(
                firstCandidate.system.asPostponedArgumentsAnalyzerContext(),
                firstAtom,
                firstCandidate
            )
            postponedArgumentsAnalyzer.applyResultsOfAnalyzedLambdaToCandidateSystem(
                firstCandidate.system.asPostponedArgumentsAnalyzerContext(),
                firstAtom,
                firstCandidate,
                results
            )
            while (iterator.hasNext()) {
                val (candidate, atom) = iterator.next()
                call.replaceCalleeReference(FirNamedReferenceWithCandidate(null, candidate.callInfo.name, candidate))
                postponedArgumentsAnalyzer.applyResultsOfAnalyzedLambdaToCandidateSystem(
                    candidate.system.asPostponedArgumentsAnalyzerContext(),
                    atom,
                    candidate,
                    results
                )
            }

            val errorCandidates = mutableSetOf<Candidate>()
            val successfulCandidates = mutableSetOf<Candidate>()

            for (candidate in candidates) {
                if (candidate.isSuccessful()) {
                    successfulCandidates += candidate
                } else {
                    errorCandidates += candidate
                }
            }
            return when {
                successfulCandidates.isNotEmpty() -> successfulCandidates
                else -> errorCandidates
            }
        } finally {
            call.replaceCalleeReference(originalCalleeReference)
        }
    }

    fun <T : FirQualifiedAccess> resolveVariableAccessAndSelectCandidate(qualifiedAccess: T): FirStatement {
        val callee = qualifiedAccess.calleeReference as? FirSimpleNamedReference ?: return qualifiedAccess

        qualifiedResolver.initProcessingQualifiedAccess(callee)

        @Suppress("NAME_SHADOWING")
        val qualifiedAccess = qualifiedAccess.transformExplicitReceiver<FirQualifiedAccess>()
        qualifiedResolver.replacedQualifier(qualifiedAccess)?.let { resolvedQualifierPart ->
            return resolvedQualifierPart
        }

        val result = collectCandidates(qualifiedAccess, callee.name)
        val reducedCandidates = result.candidates
        val nameReference = createResolvedNamedReference(
            callee,
            callee.name,
            result.info,
            reducedCandidates,
            result.applicability,
            qualifiedAccess.explicitReceiver,
        )

        if (qualifiedAccess.explicitReceiver == null) {
            if (!result.applicability.isSuccess) {
                // We should run QualifierResolver if no successful candidates are available
                // Otherwise expression (even ambiguous) beat qualifier
                qualifiedResolver.tryResolveAsQualifier(qualifiedAccess.source)?.let { resolvedQualifier ->
                    return resolvedQualifier
                }
            } else {
                qualifiedResolver.reset()
            }
        }

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

        when {
            referencedSymbol is FirClassLikeSymbol<*> -> {
                return components.buildResolvedQualifierForClass(referencedSymbol, nameReference.source, qualifiedAccess.typeArguments, diagnostic)
            }
            referencedSymbol is FirTypeParameterSymbol && referencedSymbol.fir.isReified -> {
                return buildResolvedReifiedParameterReference {
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
        return resultExpression
    }

    fun resolveCallableReference(
        constraintSystemBuilder: ConstraintSystemBuilder,
        resolvedCallableReferenceAtom: ResolvedCallableReferenceAtom,
    ): Boolean {
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
        val result = towerResolver.runResolver(
            info,
            transformer.resolutionContext,
            collector = localCollector,
            manager = TowerResolveManager(localCollector),
        )
        val bestCandidates = result.bestCandidates()
        val noSuccessfulCandidates = !result.currentApplicability.isSuccess
        val reducedCandidates = if (noSuccessfulCandidates) {
            bestCandidates.toSet()
        } else {
            conflictResolver.chooseMaximallySpecificCandidates(bestCandidates, discriminateGenerics = false)
        }

        when {
            noSuccessfulCandidates -> {
                return false
            }
            reducedCandidates.size > 1 -> {
                if (resolvedCallableReferenceAtom.postponed) return false
                resolvedCallableReferenceAtom.postponed = true
                return true
            }
        }

        val chosenCandidate = reducedCandidates.single()
        constraintSystemBuilder.runTransaction {
            chosenCandidate.outerConstraintBuilderEffect!!(this)

            true
        }

        resolvedCallableReferenceAtom.resultingCandidate = Pair(chosenCandidate, result.currentApplicability)

        return true
    }

    fun resolveDelegatingConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        constructedType: ConeClassLikeType
    ): FirDelegatedConstructorCall? {
        val name = Name.special("<init>")
        val symbol = constructedType.lookupTag.toSymbol(components.session)
        val typeArguments =
            constructedType.typeArguments.take((symbol?.fir as? FirRegularClass)?.typeParameters?.count { it is FirTypeParameter } ?: 0)
                .map {
                    it.toFirTypeProjection()
                }

        val callInfo = CallInfo(
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
                else ConeUnresolvedNameError(reference.name),
                reference.source,
                reference.name
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
        val candidate = CandidateFactory(transformer.resolutionContext, callInfo).createCandidate(
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
            stubReceiver = if (lhs !is DoubleColonLHS.Type) null else buildExpressionStub {
                source = callableReferenceAccess.source
                typeRef = buildResolvedTypeRef {
                    type = lhs.type
                }
            },
        )
    }

    private fun createResolvedNamedReference(
        reference: FirReference,
        name: Name,
        callInfo: CallInfo,
        candidates: Collection<Candidate>,
        applicability: CandidateApplicability,
        explicitReceiver: FirExpression? = null,
    ): FirNamedReference {
        val source = reference.source
        return when {
            candidates.isEmpty() -> buildErrorReference(
                callInfo,
                ConeUnresolvedNameError(name),
                source,
                name
            )

            candidates.size > 1 -> buildErrorReference(
                callInfo,
                ConeAmbiguityError(name, applicability, candidates.map { it.symbol }),
                source,
                name
            )

            !applicability.isSuccess -> {
                val candidate = candidates.single()
                val diagnostic = when (applicability) {
                    CandidateApplicability.HIDDEN -> ConeHiddenCandidateError(candidate.symbol)
                    else -> ConeInapplicableCandidateError(applicability, candidate)
                }
                buildErrorReference(source, candidate, diagnostic)
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
                if (explicitReceiver?.typeRef?.coneTypeSafe<ConeIntegerLiteralType>() == null) {
                    if (coneSymbol is FirVariableSymbol) {
                        if (coneSymbol !is FirPropertySymbol ||
                            (coneSymbol.fir as FirMemberDeclaration).typeParameters.isEmpty()
                        ) {
                            return buildResolvedNamedReference {
                                this.source = source
                                this.name = name
                                resolvedSymbol = coneSymbol
                            }
                        }
                    }
                }
                FirNamedReferenceWithCandidate(source, name, candidate)
            }
        }
    }

    private fun buildErrorReference(
        callInfo: CallInfo,
        diagnostic: ConeDiagnostic,
        source: FirSourceElement?,
        name: Name
    ): FirErrorReferenceWithCandidate {
        val candidate = CandidateFactory(transformer.resolutionContext, callInfo).createErrorCandidate(diagnostic)
        components.resolutionStageRunner.processCandidate(candidate, transformer.resolutionContext, stopOnFirstError = false)
        return FirErrorReferenceWithCandidate(source, name, candidate, diagnostic)
    }

    private fun buildErrorReference(
        source: FirSourceElement?,
        candidate: Candidate,
        diagnostic: ConeDiagnostic
    ): FirErrorReferenceWithCandidate {
        if (!candidate.fullyAnalyzed) {
            components.resolutionStageRunner.processCandidate(candidate, transformer.resolutionContext, stopOnFirstError = false)
        }
        return FirErrorReferenceWithCandidate(source, candidate.callInfo.name, candidate, diagnostic)
    }
}
