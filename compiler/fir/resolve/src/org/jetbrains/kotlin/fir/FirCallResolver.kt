/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.isInner
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedReifiedParameterReference
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.builder.buildBackingFieldReference
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.calls.tower.FirTowerResolver
import org.jetbrains.kotlin.fir.resolve.calls.tower.TowerResolveManager
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableCandidateError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.resolve.inference.ResolvedCallableReferenceAtom
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.StoreNameReference
import org.jetbrains.kotlin.fir.resolve.transformers.StoreReceiver
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.transformers.phasedFir
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator

class FirCallResolver(
    private val components: BodyResolveComponents,
    private val qualifiedResolver: FirQualifiedNameResolver,
) : BodyResolveComponents by components {

    private lateinit var transformer: FirExpressionsResolveTransformer

    fun initTransformer(transformer: FirExpressionsResolveTransformer) {
        this.transformer = transformer
    }

    private val towerResolver = FirTowerResolver(
        components, resolutionStageRunner,
    )

    private val conflictResolver =
        inferenceComponents.session.callConflictResolverFactory
            .create(TypeSpecificityComparator.NONE, inferenceComponents)

    fun resolveCallAndSelectCandidate(functionCall: FirFunctionCall): FirFunctionCall {
        qualifiedResolver.reset()
        @Suppress("NAME_SHADOWING")
        val functionCall = functionCall.transformExplicitReceiver(transformer, ResolutionMode.ContextIndependent)
            .also {
                dataFlowAnalyzer.enterQualifiedAccessExpression(functionCall)
                functionCall.argumentList.transformArguments(transformer, ResolutionMode.ContextDependent)
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
        val typeRef = typeFromCallee(resultFunctionCall)
        if (typeRef.type is ConeKotlinErrorType) {
            resultFunctionCall.resultType = typeRef
        }
        return resultFunctionCall
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
            typeArguments,
            session,
            file,
            transformer.components.containingDeclarations,
        )
        towerResolver.reset()
        val result = towerResolver.runResolver(
            info,
        )
        val bestCandidates = result.bestCandidates()
        val reducedCandidates = if (result.currentApplicability < CandidateApplicability.SYNTHETIC_RESOLVED) {
            bestCandidates.toSet()
        } else {
            val onSuperReference = (explicitReceiver as? FirQualifiedAccessExpression)?.calleeReference is FirSuperReference
            conflictResolver.chooseMaximallySpecificCandidates(
                bestCandidates, discriminateGenerics = true, discriminateAbstracts = onSuperReference
            )
        }
        if ((reducedCandidates.isEmpty() || result.currentApplicability < CandidateApplicability.SYNTHETIC_RESOLVED) &&
            explicitReceiver?.typeRef?.coneTypeSafe<ConeIntegerLiteralType>() != null
        ) {
            val approximatedQualifiedAccess = qualifiedAccess.transformExplicitReceiver(integerLiteralTypeApproximator, null)
            if (approximatedQualifiedAccess.explicitReceiver?.typeRef?.coneTypeSafe<ConeIntegerLiteralType>() == null) {
                return collectCandidates(approximatedQualifiedAccess, name)
            }
        }

        return ResolutionResult(info, result.currentApplicability, reducedCandidates)
    }

    fun <T : FirQualifiedAccess> resolveVariableAccessAndSelectCandidate(qualifiedAccess: T): FirStatement {
        val callee = qualifiedAccess.calleeReference as? FirSimpleNamedReference ?: return qualifiedAccess

        qualifiedResolver.initProcessingQualifiedAccess(callee)

        @Suppress("NAME_SHADOWING")
        val qualifiedAccess = qualifiedAccess.transformExplicitReceiver(transformer, ResolutionMode.ContextIndependent)
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
            if (result.applicability < CandidateApplicability.SYNTHETIC_RESOLVED
            ) {
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

        when {
            referencedSymbol is FirClassLikeSymbol<*> -> {
                return buildResolvedQualifierForClass(referencedSymbol, nameReference.source, qualifiedAccess.typeArguments)
            }
            referencedSymbol is FirTypeParameterSymbol && referencedSymbol.fir.isReified -> {
                return buildResolvedReifiedParameterReference {
                    source = nameReference.source
                    symbol = referencedSymbol
                    typeRef = typeForReifiedParameterReference(this)
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        var resultExpression = qualifiedAccess.transformCalleeReference(StoreNameReference, nameReference) as T
        if (reducedCandidates.size == 1) {
            val candidate = reducedCandidates.single()
            resultExpression = resultExpression.transformDispatchReceiver(StoreReceiver, candidate.dispatchReceiverExpression()) as T
            resultExpression = resultExpression.transformExtensionReceiver(StoreReceiver, candidate.extensionReceiverExpression()) as T
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
        val localCollector = CandidateCollector(this, resolutionStageRunner)
        val result = towerResolver.runResolver(
            info,
            collector = localCollector,
            manager = TowerResolveManager(localCollector),
        )
        val bestCandidates = result.bestCandidates()
        val noSuccessfulCandidates = result.currentApplicability < CandidateApplicability.SYNTHETIC_RESOLVED
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
        constructorClassSymbol: FirClassSymbol<*>,
        typeArguments: List<FirTypeProjection>,
    ): FirDelegatedConstructorCall? {
        val name = Name.special("<init>")
        val callInfo = CallInfo(
            CallKind.DelegatingConstructorCall,
            name,
            explicitReceiver = null,
            delegatedConstructorCall.argumentList,
            isPotentialQualifierPart = false,
            typeArguments = typeArguments,
            session,
            file,
            containingDeclarations,
        )
        towerResolver.reset()
        val result = towerResolver.runResolverForDelegatingConstructor(
            callInfo,
            constructorClassSymbol,
        )

        return callResolver.selectDelegatingConstructorCall(delegatedConstructorCall, name, result, callInfo)
    }

    private fun selectDelegatingConstructorCall(
        call: FirDelegatedConstructorCall, name: Name, result: CandidateCollector, callInfo: CallInfo
    ): FirDelegatedConstructorCall {
        val bestCandidates = result.bestCandidates()
        val reducedCandidates = if (result.currentApplicability < CandidateApplicability.SYNTHETIC_RESOLVED) {
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
            emptyList(),
            session,
            file,
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
            applicability < CandidateApplicability.SYNTHETIC_RESOLVED -> {
                val diagnostic = ConeInapplicableCandidateError(
                    applicability,
                    candidates.map {
                        ConeInapplicableCandidateError.CandidateInfo(
                            it.symbol,
                            if (it.systemInitialized) it.system.diagnostics else emptyList(),
                        )
                    }
                )

                buildErrorReference(callInfo, diagnostic, source, name)
            }
            candidates.size == 1 -> {
                val candidate = candidates.single()
                val coneSymbol = candidate.symbol
                if (coneSymbol is FirBackingFieldSymbol) {
                    return buildBackingFieldReference {
                        this.source = source
                        resolvedSymbol = coneSymbol
                    }
                }
                if (explicitReceiver?.typeRef?.coneTypeSafe<ConeIntegerLiteralType>() == null) {
                    if (coneSymbol is FirVariableSymbol) {
                        if (coneSymbol !is FirPropertySymbol ||
                            (coneSymbol.phasedFir() as FirMemberDeclaration).typeParameters.isEmpty()
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
            else -> buildErrorReference(
                callInfo,
                ConeAmbiguityError(name, candidates.map { it.symbol }),
                source,
                name
            )
        }
    }

    private fun buildErrorReference(
        callInfo: CallInfo,
        diagnostic: ConeDiagnostic,
        source: FirSourceElement?,
        name: Name
    ): FirErrorReferenceWithCandidate {
        val candidate = CandidateFactory(components, callInfo).createErrorCandidate(diagnostic)
        resolutionStageRunner.processCandidate(candidate, stopOnFirstError = false)
        return FirErrorReferenceWithCandidate(source, name, candidate, diagnostic)
    }
}
