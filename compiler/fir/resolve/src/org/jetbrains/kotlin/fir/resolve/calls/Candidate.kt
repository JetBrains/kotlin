/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.inference.PostponedResolvedAtom
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeVariable
import org.jetbrains.kotlin.resolve.calls.components.SuspendConversionStrategy
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess

class Candidate(
    override val symbol: FirBasedSymbol<*>,
    override val dispatchReceiverValue: ReceiverValue?,
    override val extensionReceiverValue: ReceiverValue?,
    override val explicitReceiverKind: ExplicitReceiverKind,
    val constraintSystemFactory: InferenceComponents.ConstraintSystemFactory,
    private val baseSystem: ConstraintStorage,
    override val callInfo: CallInfo,
    val originScope: FirScope?,
    val isFromCompanionObjectTypeScope: Boolean = false
) : AbstractCandidate() {

    var systemInitialized: Boolean = false
    override val system: NewConstraintSystemImpl by lazy(LazyThreadSafetyMode.NONE) {
        val system = constraintSystemFactory.createConstraintSystem()
        system.addOtherSystem(baseSystem)
        systemInitialized = true
        system
    }

    lateinit var substitutor: ConeSubstitutor
    lateinit var freshVariables: List<ConeTypeVariable>
    var resultingTypeForCallableReference: ConeKotlinType? = null
    var outerConstraintBuilderEffect: (ConstraintSystemOperation.() -> Unit)? = null
    var usesSAM: Boolean = false

    internal var callableReferenceAdaptation: CallableReferenceAdaptation? = null
        set(value) {
            field = value
            usesSuspendConversion = value?.suspendConversionStrategy == SuspendConversionStrategy.SUSPEND_CONVERSION
            if (value != null) {
                numDefaults = value.defaults
            }
        }

    var usesSuspendConversion: Boolean = false

    var argumentMapping: LinkedHashMap<FirExpression, FirValueParameter>? = null
    var numDefaults: Int = 0
    lateinit var typeArgumentMapping: TypeArgumentMapping
    val postponedAtoms = mutableListOf<PostponedResolvedAtom>()

    var currentApplicability = CandidateApplicability.RESOLVED
        private set

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

    val isSuccessful: Boolean
        get() = currentApplicability.isSuccess && (!systemInitialized || !system.hasContradiction)

    var passedStages: Int = 0

    fun dispatchReceiverExpression(): FirExpression =
        dispatchReceiverValue?.receiverExpression?.takeIf { it !is FirExpressionStub } ?: FirNoReceiverExpression

    fun extensionReceiverExpression(): FirExpression =
        extensionReceiverValue?.receiverExpression?.takeIf { it !is FirExpressionStub } ?: FirNoReceiverExpression

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
}

val Candidate.fullyAnalyzed: Boolean
    get() = passedStages == callInfo.callKind.resolutionSequence.size
