/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.candidate

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildThisReceiverExpressionCopy
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.resolve.FirSamResolver
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.calls.stages.TypeArgumentMapping
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeVariable
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintSystemError
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.util.CodeFragmentAdjustment
import org.jetbrains.kotlin.utils.addToStdlib.runUnless

class Candidate(
    symbol: FirBasedSymbol<*>,
    // Here we may have an ExpressionReceiverValue
    // - in case a use-site receiver is explicit
    // - in some cases with static entities, no matter is a use-site receiver explicit or not
    // OR we may have here a kind of ImplicitReceiverValue (non-statics only)
    override var dispatchReceiver: ConeResolutionAtom?,
    // In most cases, it contains zero or single element
    // More than one, only in case of context receiver group
    val givenExtensionReceiverOptions: List<ConeResolutionAtom>,
    override val explicitReceiverKind: ExplicitReceiverKind,
    private val constraintSystemFactory: InferenceComponents.ConstraintSystemFactory,
    private val baseSystem: ConstraintStorage,
    override val callInfo: CallInfo,
    val originScope: FirScope?,
    val isFromCompanionObjectTypeScope: Boolean = false,
    // It's only true if we're in the member scope of smart cast receiver and this particular candidate came from original type
    val isFromOriginalTypeInPresenceOfSmartCast: Boolean = false,
    bodyResolveContext: BodyResolveContext,
) : AbstractCallCandidate<ConeResolutionAtom>() {

    // ---------------------------------------- Symbol ----------------------------------------

    override var symbol: FirBasedSymbol<*> = symbol
        private set

    @UpdatingCandidateInvariants
    fun updateSymbol(symbol: FirBasedSymbol<*>) {
        this.symbol = symbol
    }

    // ---------------------------------------- Constraint system ----------------------------------------

    val usedOuterCs: Boolean get() = system.usesOuterCs

    private var systemInitialized: Boolean = false
    val system: NewConstraintSystemImpl by lazy(LazyThreadSafetyMode.NONE) {
        val system = constraintSystemFactory.createConstraintSystem()

        val baseCSFromInferenceSession =
            runUnless(baseSystem.usesOuterCs) {
                bodyResolveContext.inferenceSession.baseConstraintStorageForCandidate(this, bodyResolveContext)
            }
        if (baseCSFromInferenceSession != null) {
            system.setBaseSystem(baseCSFromInferenceSession)
            system.addOtherSystem(baseSystem)
        } else {
            system.setBaseSystem(baseSystem)
        }

        systemInitialized = true
        system
    }

    override val errors: List<ConstraintSystemError>
        get() = system.errors

    /**
     * Substitutor from declared type parameters to type variables created for that candidate
     */
    lateinit var substitutor: ConeSubstitutor
        private set
    lateinit var freshVariables: List<ConeTypeVariable>
        private set

    fun initializeSubstitutorAndVariables(substitutor: ConeSubstitutor, freshVariables: List<ConeTypeVariable>) {
        this.substitutor = substitutor
        this.freshVariables = freshVariables
    }

    @UpdatingCandidateInvariants
    fun updateSubstitutor(substitutor: ConeSubstitutor) {
        this.substitutor = substitutor
    }

    // ---------------------------------------- Conversions ----------------------------------------

    var resultingTypeForCallableReference: ConeKotlinType? = null
        private set
    var outerConstraintBuilderEffect: (ConstraintSystemOperation.() -> Unit)? = null
        private set

    val usesSamConversion: Boolean get() = functionTypesOfSamConversions != null
    val usesSamConversionOrSamConstructor: Boolean get() = usesSamConversion || symbol.origin == FirDeclarationOrigin.SamConstructor

    internal var callableReferenceAdaptation: CallableReferenceAdaptation? = null
        private set

    internal fun initializeCallableReferenceAdaptation(
        callableReferenceAdaptation: CallableReferenceAdaptation?,
        resultingTypeForCallableReference: ConeKotlinType,
        outerConstraintBuilderEffect: ConstraintSystemOperation.() -> Unit
    ) {
        require(this.callableReferenceAdaptation == null) { "callableReferenceAdaptation already initialized" }
        this.callableReferenceAdaptation = callableReferenceAdaptation
        this.resultingTypeForCallableReference = resultingTypeForCallableReference
        this.outerConstraintBuilderEffect = outerConstraintBuilderEffect
        usesFunctionConversion = callableReferenceAdaptation?.suspendConversionStrategy is CallableReferenceConversionStrategy.CustomConversion
        if (callableReferenceAdaptation != null) {
            numDefaults = callableReferenceAdaptation.defaults
        }
    }

    var usesFunctionConversion: Boolean = false
    var functionTypesOfSamConversions: HashMap<FirExpression, FirSamResolver.SamConversionInfo>? = null
        private set

    fun initializeFunctionTypesOfSamConversions(types: HashMap<FirExpression, FirSamResolver.SamConversionInfo>) {
        require(functionTypesOfSamConversions == null) { "functionTypesOfSamConversions already initialized" }
        functionTypesOfSamConversions = types
    }

    // ---------------------------------------- Argument mapping ----------------------------------------

    private var _arguments: List<ConeResolutionAtom>? = null
    val arguments: List<ConeResolutionAtom>
        get() = _arguments ?: error("Argument list is not initialized yet")

    private var _argumentMapping: LinkedHashMap<ConeResolutionAtom, FirValueParameter>? = null
    override val argumentMappingInitialized: Boolean
        get() = _argumentMapping != null
    override val argumentMapping: LinkedHashMap<ConeResolutionAtom, FirValueParameter>
        get() = _argumentMapping ?: error("Argument mapping is not initialized yet")

    fun initializeArgumentMapping(
        arguments: List<ConeResolutionAtom>,
        argumentMapping: LinkedHashMap<ConeResolutionAtom, FirValueParameter>,
    ) {
        require(_argumentMapping == null) { "Argument mapping already initialized" }
        _argumentMapping = argumentMapping
        _arguments = arguments
    }

    @UpdatingCandidateInvariants
    fun updateArgumentMapping(argumentMapping: LinkedHashMap<ConeResolutionAtom, FirValueParameter>) {
        _argumentMapping = argumentMapping
    }

    var numDefaults: Int = 0

    // ---------------------------------------- Type argument mapping ----------------------------------------

    lateinit var typeArgumentMapping: TypeArgumentMapping

    // ---------------------------------------- Postponed atoms ----------------------------------------

    private val _postponedAtoms: MutableList<ConePostponedResolvedAtom> = mutableListOf()
    val postponedAtoms: List<ConePostponedResolvedAtom> get() = _postponedAtoms

    fun addPostponedAtom(atom: ConePostponedResolvedAtom) {
        _postponedAtoms += atom
    }

    // ---------------------------------------- PCLA-related parts ----------------------------------------

    val postponedPCLACalls: MutableList<ConeResolutionAtom> = mutableListOf()
    val lambdasAnalyzedWithPCLA: MutableList<FirAnonymousFunction> = mutableListOf()

    // Currently, it's only about completion results writing for property delegation inference info
    // See the call sites of [FirDelegatedPropertyInferenceSession.completeSessionOrPostponeIfNonRoot]
    val onPCLACompletionResultsWritingCallbacks: MutableList<(ConeSubstitutor) -> Unit> = mutableListOf()

    // ---------------------------------------- Applicability ----------------------------------------

    var lowestApplicability: CandidateApplicability = CandidateApplicability.RESOLVED
        private set

    override val applicability: CandidateApplicability
        get() = lowestApplicability

    private val _diagnostics: MutableList<ResolutionDiagnostic> = mutableListOf()
    override val diagnostics: List<ResolutionDiagnostic>
        get() = _diagnostics

    fun addDiagnostic(diagnostic: ResolutionDiagnostic) {
        _diagnostics += diagnostic
        if (diagnostic.applicability < lowestApplicability) {
            lowestApplicability = diagnostic.applicability
        }
    }

    /**
     * Note that [lowestApplicability]`.isSuccess == true` doesn't imply [isSuccessful].
     *
     * This is because [lowestApplicability] is equal to the lowest [ResolutionDiagnostic.applicability] of all [diagnostics],
     * but in presence of more than one diagnostic, the lowest one can be successful while a higher one isn't, e.g., the combination
     * of [CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY] and [CandidateApplicability.RESOLVED_WITH_ERROR].
     *
     * Also see [org.jetbrains.kotlin.fir.resolve.transformers.FirCallCompletionResultsWriterTransformer.toResolvedReference]
     * as it contains conditions that rely on subtle differences between the implementation of this property and
     * [org.jetbrains.kotlin.resolve.calls.tower.isSuccess].
     */
    val isSuccessful: Boolean
        get() = diagnostics.allSuccessful && (!systemInitialized || !system.hasContradiction)

    // ---------------------------------------- Receivers ----------------------------------------

    override var chosenExtensionReceiver: ConeResolutionAtom? = givenExtensionReceiverOptions.singleOrNull()

    var contextReceiverArguments: List<ConeResolutionAtom>? = null

    // FirExpressionStub can be located here in case of callable reference resolution
    fun dispatchReceiverExpression(): FirExpression? {
        return dispatchReceiver?.expression?.takeIf { it !is FirExpressionStub }
    }

    // FirExpressionStub can be located here in case of callable reference resolution
    fun chosenExtensionReceiverExpression(): FirExpression? {
        return chosenExtensionReceiver?.expression?.takeIf { it !is FirExpressionStub }
    }

    fun contextReceiverArguments(): List<FirExpression> {
        return contextReceiverArguments?.map { it.expression } ?: emptyList()
    }

    private var sourcesWereUpdated = false

    // In case of implicit receivers we want to update corresponding sources to generate correct offset. This method must be called only
    // once when candidate was selected and confirmed to be correct one.
    fun updateSourcesOfReceivers() {
        require(!sourcesWereUpdated)
        sourcesWereUpdated = true

        dispatchReceiver = dispatchReceiver?.tryToSetSourceForImplicitReceiver()
        chosenExtensionReceiver = chosenExtensionReceiver?.tryToSetSourceForImplicitReceiver()
        contextReceiverArguments = contextReceiverArguments?.map { it.tryToSetSourceForImplicitReceiver() }
    }

    private fun ConeResolutionAtom.tryToSetSourceForImplicitReceiver(): ConeResolutionAtom {
        if (this !is ConeSimpleLeafResolutionAtom) return this

        fun FirExpression.tryToSetSourceForImplicitReceiver(): FirExpression? {
            return when {
                this is FirSmartCastExpression -> {
                    val newOriginal = this.originalExpression.tryToSetSourceForImplicitReceiver() ?: return null
                    this.apply { replaceOriginalExpression(newOriginal) }
                }
                this is FirThisReceiverExpression && isImplicit -> {
                    buildThisReceiverExpressionCopy(this) {
                        source = callInfo.callSite.source?.fakeElement(KtFakeSourceElementKind.ImplicitReceiver)
                    }
                }
                else -> null
            }
        }

        val newExpression = this.expression.tryToSetSourceForImplicitReceiver() ?: return this
        return ConeSimpleLeafResolutionAtom(newExpression)
    }

    // ---------------------------------------- Backing field ----------------------------------------

    var hasVisibleBackingField: Boolean = false

    // ---------------------------------------- Util ----------------------------------------

    @CodeFragmentAdjustment
    internal fun resetToResolved() {
        lowestApplicability = CandidateApplicability.RESOLVED
        _diagnostics.clear()
    }

    var passedStages: Int = 0


    /**
     * Please avoid updating symbol in the candidate whenever it's possible.
     * The only case when currently it seems to be unavoidable is at
     * [org.jetbrains.kotlin.fir.resolve.transformers.FirCallCompletionResultsWriterTransformer.refineSubstitutedMemberIfReceiverContainsTypeVariable]
     */
    @RequiresOptIn
    annotation class UpdatingCandidateInvariants

    // ---------------------------------------- hashcode/equals/toString ----------------------------------------

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Candidate

        if (symbol != other.symbol) return false

        return true
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun toString(): String {
        val okOrFail = if (isSuccessful) "OK" else "FAIL"
        val step = "$passedStages/${callInfo.callKind.resolutionSequence.size}"
        return "$okOrFail($step): $symbol"
    }
}

val Candidate.fullyAnalyzed: Boolean
    get() = passedStages == callInfo.callKind.resolutionSequence.size
