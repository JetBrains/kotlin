/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtomMarker
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind

class CallInfo(
    val callKind: CallKind,

    val explicitReceiver: FirExpression?,
    val arguments: List<FirExpression>,
    val isSafeCall: Boolean,

    val typeArguments: List<FirTypeProjection>,
    val session: FirSession,
    val containingFile: FirFile,
    val container: FirDeclaration,
    val typeProvider: (FirExpression) -> FirTypeRef?
) {
    val argumentCount get() = arguments.size
}

enum class CandidateApplicability {
    HIDDEN,
    WRONG_RECEIVER,
    PARAMETER_MAPPING_ERROR,
    INAPPLICABLE,
    SYNTHETIC_RESOLVED,
    RESOLVED
}

class Candidate(
    val symbol: AbstractFirBasedSymbol<*>,
    val dispatchReceiverValue: ClassDispatchReceiverValue?,
    val implicitExtensionReceiverValue: ImplicitReceiverValue<*>?,
    val explicitReceiverKind: ExplicitReceiverKind,
    private val inferenceComponents: InferenceComponents,
    private val baseSystem: ConstraintStorage,
    val callInfo: CallInfo
) {
    val system by lazy {
        val system = inferenceComponents.createConstraintSystem()
        system.addOtherSystem(baseSystem)
        system
    }
    lateinit var substitutor: ConeSubstitutor

    var argumentMapping: Map<FirExpression, FirValueParameter>? = null
    val postponedAtoms = mutableListOf<PostponedResolvedAtomMarker>()

    fun dispatchReceiverExpression(): FirExpression = when (explicitReceiverKind) {
        ExplicitReceiverKind.DISPATCH_RECEIVER, ExplicitReceiverKind.BOTH_RECEIVERS -> callInfo.explicitReceiver!!
        else -> dispatchReceiverValue?.receiverExpression ?: FirNoReceiverExpression
    }

    fun extensionReceiverExpression(): FirExpression = when (explicitReceiverKind) {
        ExplicitReceiverKind.EXTENSION_RECEIVER, ExplicitReceiverKind.BOTH_RECEIVERS -> callInfo.explicitReceiver!!
        else -> implicitExtensionReceiverValue?.receiverExpression ?: FirNoReceiverExpression
    }
}