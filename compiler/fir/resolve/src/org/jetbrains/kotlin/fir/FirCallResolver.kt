/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
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
import org.jetbrains.kotlin.fir.resolve.calls.tower.TowerGroup
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
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind

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
        var functionCall = functionCall.transformExplicitReceiver(transformer, ResolutionMode.ContextIndependent)
            .also {
                dataFlowAnalyzer.enterQualifiedAccessExpression(functionCall)
                functionCall.argumentList.transformArguments(transformer, ResolutionMode.ContextDependent)
            }

        var result = collectCandidates(functionCall)

        if (
            (result.candidates.isEmpty() || result.applicability < CandidateApplicability.SYNTHETIC_RESOLVED) &&
            functionCall.explicitReceiver?.typeRef?.coneTypeSafe<ConeIntegerLiteralType>() != null
        ) {
            functionCall = functionCall.transformExplicitReceiver(integerLiteralTypeApproximator, null)
            result = collectCandidates(functionCall)
        }

        val nameReference = createResolvedNamedReference(
            functionCall.calleeReference,
            functionCall.calleeReference.name,
            result.candidates,
            result.applicability,
        )

        val resultExpression = functionCall.transformCalleeReference(StoreNameReference, nameReference)
        val candidate = resultExpression.candidate()

        // We need desugaring
        val resultFunctionCall = if (candidate != null && candidate.callInfo != result.info) {
            functionCall.copy(
                explicitReceiver = candidate.callInfo.explicitReceiver,
                dispatchReceiver = candidate.dispatchReceiverExpression(),
                extensionReceiver = candidate.extensionReceiverExpression(),
                argumentList = candidate.callInfo.argumentList,
                safe = candidate.callInfo.isSafeCall,
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

    private fun collectCandidates(functionCall: FirFunctionCall): ResolutionResult {
        val explicitReceiver = functionCall.explicitReceiver
        val argumentList = functionCall.argumentList
        val typeArguments = functionCall.typeArguments

        val info = CallInfo(
            CallKind.Function,
            functionCall.calleeReference.name,
            explicitReceiver,
            argumentList,
            functionCall.safe,
            isPotentialQualifierPart = false,
            typeArguments,
            session,
            file,
            transformer.components.implicitReceiverStack,
        )
        towerResolver.reset()
        val result = towerResolver.runResolver(
            implicitReceiverStack.receiversAsReversed(),
            info,
        )
        val bestCandidates = result.bestCandidates()
        val reducedCandidates = if (result.currentApplicability < CandidateApplicability.SYNTHETIC_RESOLVED) {
            bestCandidates.toSet()
        } else {
            val onSuperReference = (functionCall.explicitReceiver as? FirQualifiedAccessExpression)?.calleeReference is FirSuperReference
            conflictResolver.chooseMaximallySpecificCandidates(
                bestCandidates, discriminateGenerics = true, discriminateAbstracts = onSuperReference
            )
        }
        return ResolutionResult(info, result.currentApplicability, reducedCandidates)
    }

    fun <T : FirQualifiedAccess> resolveVariableAccessAndSelectCandidate(qualifiedAccess: T): FirStatement {
        val callee = qualifiedAccess.calleeReference as? FirSimpleNamedReference ?: return qualifiedAccess

        qualifiedResolver.initProcessingQualifiedAccess(qualifiedAccess, callee)

        @Suppress("NAME_SHADOWING")
        val qualifiedAccess = qualifiedAccess.transformExplicitReceiver(transformer, ResolutionMode.ContextIndependent)
        qualifiedResolver.replacedQualifier(qualifiedAccess)?.let { resolvedQualifierPart ->
            return resolvedQualifierPart
        }

        val info = CallInfo(
            CallKind.VariableAccess,
            callee.name,
            qualifiedAccess.explicitReceiver,
            FirEmptyArgumentList,
            qualifiedAccess.safe,
            qualifiedAccess.explicitReceiver is FirResolvedQualifier && qualifiedResolver.isPotentialQualifierPartPosition(),
            emptyList(),
            session,
            file,
            transformer.components.implicitReceiverStack,
        )
        towerResolver.reset()
        val result = towerResolver.runResolver(
            implicitReceiverStack.receiversAsReversed(),
            info,
        )

        val bestCandidates = result.bestCandidates()
        val reducedCandidates = if (result.currentApplicability < CandidateApplicability.SYNTHETIC_RESOLVED) {
            bestCandidates.toSet()
        } else {
            val onSuperReference = (qualifiedAccess.explicitReceiver as? FirQualifiedAccessExpression)?.calleeReference is FirSuperReference
            conflictResolver.chooseMaximallySpecificCandidates(
                bestCandidates, discriminateGenerics = false, discriminateAbstracts = onSuperReference
            )
        }
        val nameReference = createResolvedNamedReference(
            callee,
            callee.name,
            reducedCandidates,
            result.currentApplicability,
        )

        if (qualifiedAccess.explicitReceiver == null) {
            if (result.currentApplicability < CandidateApplicability.SYNTHETIC_RESOLVED
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
            implicitReceiverStack.receiversAsReversed(),
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
        symbol: FirClassSymbol<*>,
        typeArguments: List<FirTypeProjection>,
    ): FirDelegatedConstructorCall? {
        val scope = symbol.fir.unsubstitutedScope(session, scopeSession)
        val className = symbol.classId.shortClassName
        val callInfo = CallInfo(
            CallKind.DelegatingConstructorCall,
            className,
            explicitReceiver = null,
            delegatedConstructorCall.argumentList,
            isSafeCall = false,
            isPotentialQualifierPart = false,
            typeArguments = typeArguments,
            session,
            file,
            implicitReceiverStack,
        )
        val candidateFactory = CandidateFactory(this, callInfo)
        val candidates = mutableListOf<Candidate>()

        scope.processFunctionsByName(className) {
            if (it is FirConstructorSymbol) {
                val candidate = candidateFactory.createCandidate(it, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER)
                candidate.typeArgumentMapping = TypeArgumentMapping.Mapped(typeArguments)
                candidates += candidate
            }
        }
        return callResolver.selectCandidateFromGivenCandidates(delegatedConstructorCall, className, candidates)
    }

    private fun <T> selectCandidateFromGivenCandidates(
        call: T, name: Name, candidates: Collection<Candidate>,
    ): T where T : FirResolvable, T : FirCall {
        val result = CandidateCollector(this, resolutionStageRunner)
        candidates.forEach { result.consumeCandidate(TowerGroup.Start, it) }
        val bestCandidates = result.bestCandidates()
        val reducedCandidates = if (result.currentApplicability < CandidateApplicability.SYNTHETIC_RESOLVED) {
            bestCandidates.toSet()
        } else {
            conflictResolver.chooseMaximallySpecificCandidates(bestCandidates, discriminateGenerics = true)
        }

        val nameReference = createResolvedNamedReference(
            call.calleeReference,
            name,
            reducedCandidates,
            result.currentApplicability,
        )

        return call.transformCalleeReference(StoreNameReference, nameReference) as T
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
            false,
            isPotentialQualifierPart = false,
            emptyList(),
            session,
            file,
            transformer.components.implicitReceiverStack,
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
        candidates: Collection<Candidate>,
        applicability: CandidateApplicability,
    ): FirNamedReference {
        val source = reference.source
        return when {
            candidates.isEmpty() -> buildErrorNamedReference {
                this.source = source
                diagnostic = ConeUnresolvedNameError(name)
            }
            applicability < CandidateApplicability.SYNTHETIC_RESOLVED -> {
                buildErrorNamedReference {
                    this.source = source
                    diagnostic = ConeInapplicableCandidateError(
                        applicability,
                        candidates.map {
                            ConeInapplicableCandidateError.CandidateInfo(
                                it.symbol,
                                if (it.systemInitialized) it.system.diagnostics else emptyList(),
                            )
                        },
                    )
                }
            }
            candidates.size == 1 -> {
                val candidate = candidates.single()
                val coneSymbol = candidate.symbol
                when {
                    coneSymbol is FirBackingFieldSymbol -> buildBackingFieldReference {
                        this.source = source
                        resolvedSymbol = coneSymbol
                    }

                    coneSymbol is FirVariableSymbol && (
                            coneSymbol !is FirPropertySymbol ||
                                    (coneSymbol.phasedFir() as FirMemberDeclaration).typeParameters.isEmpty()
                            )
                    -> buildResolvedNamedReference {
                        this.source = source
                        this.name = name
                        resolvedSymbol = coneSymbol
                    }

                    else -> FirNamedReferenceWithCandidate(source, name, candidate)
                }
            }
            else -> buildErrorNamedReference {
                this.source = source
                diagnostic = ConeAmbiguityError(name, candidates.map { it.symbol })
            }
        }
    }
}
