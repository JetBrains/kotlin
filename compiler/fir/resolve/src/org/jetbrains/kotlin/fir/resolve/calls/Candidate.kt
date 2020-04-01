/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirEmptyArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.DoubleColonLHS
import org.jetbrains.kotlin.fir.resolve.ImplicitReceiverStack
import org.jetbrains.kotlin.fir.resolve.inference.PostponedResolvedAtom
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeVariable
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind

data class CallInfo(
    val callKind: CallKind,
    val name: Name,

    val explicitReceiver: FirExpression?,
    val argumentList: FirArgumentList,
    val isSafeCall: Boolean,
    val isPotentialQualifierPart: Boolean,

    val typeArguments: List<FirTypeProjection>,
    val session: FirSession,
    val containingFile: FirFile,
    val implicitReceiverStack: ImplicitReceiverStack,

    val candidateForCommonInvokeReceiver: Candidate? = null,

    // Four properties for callable references only
    val expectedType: ConeKotlinType? = null,
    val outerCSBuilder: ConstraintSystemBuilder? = null,
    val lhs: DoubleColonLHS? = null,
    val stubReceiver: FirExpression? = null
) {
    val arguments: List<FirExpression> get() = argumentList.arguments

    val argumentCount get() = arguments.size

    fun noStubReceiver(): CallInfo =
        if (stubReceiver == null) this else copy(stubReceiver = null)

    fun replaceWithVariableAccess(): CallInfo =
        copy(callKind = CallKind.VariableAccess, typeArguments = emptyList(), argumentList = FirEmptyArgumentList)

    fun replaceExplicitReceiver(explicitReceiver: FirExpression?): CallInfo =
        copy(explicitReceiver = explicitReceiver)

    fun withReceiverAsArgument(receiverExpression: FirExpression): CallInfo =
        copy(
            argumentList = buildArgumentList {
                arguments += receiverExpression
                arguments += argumentList.arguments
            }
        )
}

enum class CandidateApplicability {
    HIDDEN,
    WRONG_RECEIVER,
    PARAMETER_MAPPING_ERROR,
    INAPPLICABLE,
    SYNTHETIC_RESOLVED,
    RESOLVED_LOW_PRIORITY,
    RESOLVED
}

class Candidate(
    val symbol: AbstractFirBasedSymbol<*>,
    val dispatchReceiverValue: ReceiverValue?,
    val implicitExtensionReceiverValue: ImplicitReceiverValue<*>?,
    val explicitReceiverKind: ExplicitReceiverKind,
    val bodyResolveComponents: BodyResolveComponents,
    private val baseSystem: ConstraintStorage,
    val callInfo: CallInfo
) {

    var systemInitialized: Boolean = false
    val system: NewConstraintSystemImpl by lazy {
        val system = bodyResolveComponents.inferenceComponents.createConstraintSystem()
        system.addOtherSystem(baseSystem)
        systemInitialized = true
        system
    }

    val samResolver get() = bodyResolveComponents.samResolver

    lateinit var substitutor: ConeSubstitutor
    lateinit var freshVariables: List<ConeTypeVariable>
    var resultingTypeForCallableReference: ConeKotlinType? = null
    var outerConstraintBuilderEffect: (ConstraintSystemOperation.() -> Unit)? = null
    var usesSAM: Boolean = false

    var argumentMapping: Map<FirExpression, FirValueParameter>? = null
    lateinit var typeArgumentMapping: TypeArgumentMapping
    val postponedAtoms = mutableListOf<PostponedResolvedAtom>()

    val diagnostics: MutableList<ResolutionDiagnostic> = mutableListOf()

    fun dispatchReceiverExpression(): FirExpression = when (explicitReceiverKind) {
        ExplicitReceiverKind.DISPATCH_RECEIVER, ExplicitReceiverKind.BOTH_RECEIVERS -> callInfo.explicitReceiver!!
        else -> dispatchReceiverValue?.receiverExpression ?: FirNoReceiverExpression
    }

    fun extensionReceiverExpression(): FirExpression = when (explicitReceiverKind) {
        ExplicitReceiverKind.EXTENSION_RECEIVER, ExplicitReceiverKind.BOTH_RECEIVERS -> callInfo.explicitReceiver!!
        else -> implicitExtensionReceiverValue?.receiverExpression ?: FirNoReceiverExpression
    }
}
