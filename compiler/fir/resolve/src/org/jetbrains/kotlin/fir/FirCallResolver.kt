/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedQualifierImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedReifiedParameterReferenceImpl
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirBackingFieldReferenceImpl
import org.jetbrains.kotlin.fir.references.impl.FirErrorNamedReferenceImpl
import org.jetbrains.kotlin.fir.references.impl.FirResolvedNamedReferenceImpl
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.FirAmbiguityError
import org.jetbrains.kotlin.fir.resolve.diagnostics.FirInapplicableCandidateError
import org.jetbrains.kotlin.fir.resolve.diagnostics.FirUnresolvedNameError
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.StoreNameReference
import org.jetbrains.kotlin.fir.resolve.transformers.StoreReceiver
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.transformers.phasedFir
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.scopes.scope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind

class FirCallResolver(
    components: BodyResolveComponents,
    topLevelScopes: List<FirScope>,
    localScopes: List<FirLocalScope>,
    override val implicitReceiverStack: ImplicitReceiverStack,
    private val qualifiedResolver: FirQualifiedNameResolver
) : BodyResolveComponents by components {

    private lateinit var transformer: FirExpressionsResolveTransformer

    fun initTransformer(transformer: FirExpressionsResolveTransformer) {
        this.transformer = transformer
    }

    private val towerResolver = FirTowerResolver(
        returnTypeCalculator, this, resolutionStageRunner,
        topLevelScopes = topLevelScopes.asReversed(),
        localScopes = localScopes.asReversed()
    )

    private val conflictResolver =
        inferenceComponents.session.callConflictResolverFactory
            .create(TypeSpecificityComparator.NONE, inferenceComponents)

    fun resolveCallAndSelectCandidate(functionCall: FirFunctionCall, file: FirFile): FirFunctionCall {
        qualifiedResolver.reset()
        @Suppress("NAME_SHADOWING")
        var functionCall = functionCall.transformExplicitReceiver(transformer, ResolutionMode.ContextIndependent)
            .also { dataFlowAnalyzer.enterQualifiedAccessExpression(functionCall) }
            .transformArguments(transformer, ResolutionMode.ContextDependent)

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
            result.applicability
        )

        val resultExpression = functionCall.transformCalleeReference(StoreNameReference, nameReference)
        val candidate = resultExpression.candidate()

        // We need desugaring
        val resultFunctionCall = if (candidate != null && candidate.callInfo != result.info) {
            functionCall.copy(
                explicitReceiver = candidate.callInfo.explicitReceiver,
                dispatchReceiver = candidate.dispatchReceiverExpression(),
                extensionReceiver = candidate.extensionReceiverExpression(),
                arguments = candidate.callInfo.arguments,
                safe = candidate.callInfo.isSafeCall
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

    private data class ResolutionResult(val info: CallInfo, val applicability: CandidateApplicability, val candidates: Collection<Candidate>)

    private fun collectCandidates(functionCall: FirFunctionCall): ResolutionResult {
        val explicitReceiver = functionCall.explicitReceiver
        val arguments = functionCall.arguments
        val typeArguments = functionCall.typeArguments

        val info = CallInfo(
            CallKind.Function,
            functionCall.calleeReference.name,
            explicitReceiver,
            arguments,
            functionCall.safe,
            typeArguments,
            session,
            file,
            transformer.components.implicitReceiverStack
        )
        towerResolver.reset()
        val result = towerResolver.runResolver(
            implicitReceiverStack.receiversAsReversed(),
            info
        )
        val bestCandidates = result.bestCandidates()
        val reducedCandidates = if (result.currentApplicability < CandidateApplicability.SYNTHETIC_RESOLVED) {
            bestCandidates.toSet()
        } else {
            conflictResolver.chooseMaximallySpecificCandidates(bestCandidates, discriminateGenerics = true)
        }
        return ResolutionResult(info, result.currentApplicability, reducedCandidates)
    }

    fun <T : FirQualifiedAccess> resolveVariableAccessAndSelectCandidate(qualifiedAccess: T, file: FirFile): FirStatement {
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
            emptyList(),
            qualifiedAccess.safe,
            emptyList(),
            session,
            file,
            transformer.components.implicitReceiverStack
        )
        towerResolver.reset()
        val result = towerResolver.runResolver(
            implicitReceiverStack.receiversAsReversed(),
            info
        )

        val bestCandidates = result.bestCandidates()
        val reducedCandidates = if (result.currentApplicability < CandidateApplicability.SYNTHETIC_RESOLVED) {
            bestCandidates.toSet()
        } else {
            conflictResolver.chooseMaximallySpecificCandidates(bestCandidates, discriminateGenerics = false)
        }
        val nameReference = createResolvedNamedReference(
            callee,
            callee.name,
            reducedCandidates,
            result.currentApplicability
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
                val classId = referencedSymbol.classId
                return FirResolvedQualifierImpl(nameReference.source, classId.packageFqName, classId.relativeClassName).apply {
                    resultType = if (classId.isLocal) {
                        typeForQualifierByDeclaration(referencedSymbol.fir, resultType)
                            ?: resultType.resolvedTypeFromPrototype(
                                session.builtinTypes.unitType.type//StandardClassIds.Unit(symbolProvider).constructType(emptyArray(), isNullable = false)
                            )
                    } else {
                        typeForQualifier(this)
                    }
                }
            }
            referencedSymbol is FirTypeParameterSymbol && referencedSymbol.fir.isReified -> {
                return FirResolvedReifiedParameterReferenceImpl(nameReference.source, referencedSymbol).apply {
                    resultType = typeForReifiedParameterReference(this)
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
        resolvedCallableReferenceAtom: ResolvedCallableReferenceAtom
    ): Boolean {
        val callableReferenceAccess = resolvedCallableReferenceAtom.reference
        val lhs = resolvedCallableReferenceAtom.lhs
        val coneSubstitutor = constraintSystemBuilder.buildCurrentSubstitutor() as ConeSubstitutor
        val expectedType = resolvedCallableReferenceAtom.expectedType?.let(coneSubstitutor::substituteOrSelf)

        val info = createCallableReferencesInfoForLHS(
            callableReferenceAccess, lhs,
            expectedType, constraintSystemBuilder
        )
        // No reset here!
        val result = towerResolver.runResolver(
            implicitReceiverStack.receiversAsReversed(),
            info,
            collector = CandidateCollector(this, resolutionStageRunner)
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
        typeArguments: List<FirTypeProjection>
    ): FirDelegatedConstructorCall? {
        val scope = symbol.fir.scope(ConeSubstitutor.Empty, session, scopeSession) ?: return null
        val className = symbol.classId.shortClassName
        val callInfo = CallInfo(
            CallKind.Function,
            className,
            explicitReceiver = null,
            delegatedConstructorCall.arguments,
            isSafeCall = false,
            typeArguments = typeArguments,
            session,
            file,
            implicitReceiverStack
        )
        val candidateFactory = CandidateFactory(this, callInfo)
        val candidates = mutableListOf<Candidate>()

        scope.processFunctionsByName(className) {
            if (it is FirConstructorSymbol) {
                candidates += candidateFactory.createCandidate(it, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER)
            }
            ProcessorAction.NEXT
        }
        return callResolver.selectCandidateFromGivenCandidates(delegatedConstructorCall, className, candidates)
    }

    fun <T> selectCandidateFromGivenCandidates(call: T, name: Name, candidates: Collection<Candidate>): T where T : FirResolvable, T : FirCall {
        val result = CandidateCollector(this, resolutionStageRunner)
        candidates.forEach { result.consumeCandidate(0, it) }
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
            result.currentApplicability
        )

        return call.transformCalleeReference(StoreNameReference, nameReference) as T
    }

    private fun createCallableReferencesInfoForLHS(
        callableReferenceAccess: FirCallableReferenceAccess,
        lhs: DoubleColonLHS?,
        expectedType: ConeKotlinType?,
        outerConstraintSystemBuilder: ConstraintSystemBuilder?
    ): CallInfo {
        return CallInfo(
            CallKind.CallableReference,
            callableReferenceAccess.calleeReference.name,
            callableReferenceAccess.explicitReceiver,
            emptyList(),
            false,
            emptyList(),
            session,
            file,
            transformer.components.implicitReceiverStack,
            // Additional things for callable reference resolve
            expectedType,
            outerConstraintSystemBuilder,
            lhs,
            stubReceiver = if (lhs !is DoubleColonLHS.Type) null else FirExpressionStub(callableReferenceAccess.source).apply {
                replaceTypeRef(FirResolvedTypeRefImpl(null, lhs.type))
            }
        )
    }

    private fun createResolvedNamedReference(
        reference: FirReference,
        name: Name,
        candidates: Collection<Candidate>,
        applicability: CandidateApplicability
    ): FirNamedReference {
        val source = reference.source
        return when {
            candidates.isEmpty() -> FirErrorNamedReferenceImpl(
                source, FirUnresolvedNameError(name)
            )
            applicability < CandidateApplicability.SYNTHETIC_RESOLVED -> {
                FirErrorNamedReferenceImpl(
                    source,
                    FirInapplicableCandidateError(applicability, candidates.map {
                        FirInapplicableCandidateError.CandidateInfo(
                            it.symbol,
                            if (it.systemInitialized) it.system.diagnostics else emptyList()
                        )
                    })
                )
            }
            candidates.size == 1 -> {
                val candidate = candidates.single()
                val coneSymbol = candidate.symbol
                when {
                    coneSymbol is FirBackingFieldSymbol -> FirBackingFieldReferenceImpl(source, null, coneSymbol)
                    coneSymbol is FirVariableSymbol && (
                            coneSymbol !is FirPropertySymbol ||
                                    (coneSymbol.phasedFir() as FirMemberDeclaration).typeParameters.isEmpty()
                            ) ->
                        FirResolvedNamedReferenceImpl(source, name, coneSymbol)
                    else -> FirNamedReferenceWithCandidate(source, name, candidate)
                }
            }
            else -> FirErrorNamedReferenceImpl(
                source, FirAmbiguityError(name, candidates.map { it.symbol })
            )
        }
    }
}
