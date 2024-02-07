/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildThisReceiverExpressionCopy
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.resolve.inference.FirInferenceSession
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.inference.PostponedResolvedAtom
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
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
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.util.CodeFragmentAdjustment
import org.jetbrains.kotlin.utils.addToStdlib.runUnless

class Candidate(
    symbol: FirBasedSymbol<*>,
    // Here we may have an ExpressionReceiverValue
    // - in case a use-site receiver is explicit
    // - in some cases with static entities, no matter is a use-site receiver explicit or not
    // OR we may have here a kind of ImplicitReceiverValue (non-statics only)
    override var dispatchReceiver: FirExpression?,
    // In most cases, it contains zero or single element
    // More than one, only in case of context receiver group
    val givenExtensionReceiverOptions: List<FirExpression>,
    override val explicitReceiverKind: ExplicitReceiverKind,
    private val constraintSystemFactory: InferenceComponents.ConstraintSystemFactory,
    private val baseSystem: ConstraintStorage,
    override val callInfo: CallInfo,
    val originScope: FirScope?,
    val isFromCompanionObjectTypeScope: Boolean = false,
    // It's only true if we're in the member scope of smart cast receiver and this particular candidate came from original type
    val isFromOriginalTypeInPresenceOfSmartCast: Boolean = false,
    inferenceSession: FirInferenceSession,
) : AbstractCandidate() {

    override var symbol: FirBasedSymbol<*> = symbol
        private set


    /**
     * Please avoid updating symbol in the candidate whenever it's possible.
     * The only case when currently it seems to be unavoidable is at
     * [org.jetbrains.kotlin.fir.resolve.transformers.FirCallCompletionResultsWriterTransformer.refineSubstitutedMemberIfReceiverContainsTypeVariable]
     */
    @RequiresOptIn
    annotation class UpdatingSymbol

    @UpdatingSymbol
    fun updateSymbol(symbol: FirBasedSymbol<*>) {
        this.symbol = symbol
    }

    val usedOuterCs: Boolean get() = system.usesOuterCs

    private var systemInitialized: Boolean = false
    val system: NewConstraintSystemImpl by lazy(LazyThreadSafetyMode.NONE) {
        val system = constraintSystemFactory.createConstraintSystem()

        val baseCSFromInferenceSession =
            runUnless(baseSystem.usesOuterCs) { inferenceSession.baseConstraintStorageForCandidate(this) }
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
    lateinit var freshVariables: List<ConeTypeVariable>
    var resultingTypeForCallableReference: ConeKotlinType? = null
    var outerConstraintBuilderEffect: (ConstraintSystemOperation.() -> Unit)? = null
    val usesSAM: Boolean get() = functionTypesOfSamConversions != null

    internal var callableReferenceAdaptation: CallableReferenceAdaptation? = null
        set(value) {
            field = value
            usesFunctionConversion = value?.suspendConversionStrategy is CallableReferenceConversionStrategy.CustomConversion
            if (value != null) {
                numDefaults = value.defaults
            }
        }

    var usesFunctionConversion: Boolean = false

    var argumentMapping: LinkedHashMap<FirExpression, FirValueParameter>? = null
    var numDefaults: Int = 0
    var functionTypesOfSamConversions: HashMap<FirExpression, ConeKotlinType>? = null
    lateinit var typeArgumentMapping: TypeArgumentMapping
    val postponedAtoms = mutableListOf<PostponedResolvedAtom>()

    // PCLA-related parts
    val postponedPCLACalls = mutableListOf<FirStatement>()
    val lambdasAnalyzedWithPCLA = mutableListOf<FirAnonymousFunction>()

    // Currently, it's only about completion results writing for property delegation inference info
    // See the call sites of [FirDelegatedPropertyInferenceSession.completeSessionOrPostponeIfNonRoot]
    val onPCLACompletionResultsWritingCallbacks = mutableListOf<(ConeSubstitutor) -> Unit>()

    var currentApplicability = CandidateApplicability.RESOLVED
        private set

    override var chosenExtensionReceiver: FirExpression? = givenExtensionReceiverOptions.singleOrNull()

    var contextReceiverArguments: List<FirExpression>? = null

    override val applicability: CandidateApplicability
        get() = currentApplicability

    private val _diagnostics: MutableList<ResolutionDiagnostic> = mutableListOf()
    override val diagnostics: List<ResolutionDiagnostic>
        get() = _diagnostics

    fun addDiagnostic(diagnostic: ResolutionDiagnostic) {
        _diagnostics += diagnostic
        if (diagnostic.applicability < currentApplicability) {
            currentApplicability = diagnostic.applicability
        }
    }

    @CodeFragmentAdjustment
    internal fun resetToResolved() {
        currentApplicability = CandidateApplicability.RESOLVED
        _diagnostics.clear()
    }

    /**
     * Note that [currentApplicability]`.isSuccessful == true` doesn't imply [isSuccessful].
     *
     * This is because [currentApplicability] is equal to the lowest [ResolutionDiagnostic.applicability] of all [diagnostics],
     * but in presence of more than one diagnostic, the lowest one can be successful while a higher one isn't, e.g., the combination
     * of [CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY] and [CandidateApplicability.RESOLVED_WITH_ERROR].
     */
    val isSuccessful: Boolean
        get() = diagnostics.all { it.applicability.isSuccess } && (!systemInitialized || !system.hasContradiction)

    var passedStages: Int = 0

    private var sourcesWereUpdated = false

    // FirExpressionStub can be located here in case of callable reference resolution
    fun dispatchReceiverExpression(): FirExpression? {
        return dispatchReceiver?.takeIf { it !is FirExpressionStub }
    }

    // FirExpressionStub can be located here in case of callable reference resolution
    fun chosenExtensionReceiverExpression(): FirExpression? {
        return chosenExtensionReceiver?.takeIf { it !is FirExpressionStub }
    }

    fun contextReceiverArguments(): List<FirExpression> {
        return contextReceiverArguments ?: emptyList()
    }

    // In case of implicit receivers we want to update corresponding sources to generate correct offset. This method must be called only
    // once when candidate was selected and confirmed to be correct one.
    fun updateSourcesOfReceivers() {
        require(!sourcesWereUpdated)
        sourcesWereUpdated = true

        dispatchReceiver = dispatchReceiver?.tryToSetSourceForImplicitReceiver()
        chosenExtensionReceiver = chosenExtensionReceiver?.tryToSetSourceForImplicitReceiver()
        contextReceiverArguments = contextReceiverArguments?.map { it.tryToSetSourceForImplicitReceiver() }
    }

    private fun FirExpression.tryToSetSourceForImplicitReceiver(): FirExpression {
        return when {
            this is FirSmartCastExpression -> {
                this.apply { replaceOriginalExpression(this.originalExpression.tryToSetSourceForImplicitReceiver()) }
            }
            this is FirThisReceiverExpression && isImplicit -> {
                buildThisReceiverExpressionCopy(this) {
                    source = callInfo.callSite.source?.fakeElement(KtFakeSourceElementKind.ImplicitReceiver)
                }
            }
            else -> this
        }
    }

    var hasVisibleBackingField = false

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
        val okOrFail = if (applicability.isSuccess) "OK" else "FAIL"
        val step = "$passedStages/${callInfo.callKind.resolutionSequence.size}"
        return "$okOrFail($step): $symbol"
    }
}

val Candidate.fullyAnalyzed: Boolean
    get() = passedStages == callInfo.callKind.resolutionSequence.size
