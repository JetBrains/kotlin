/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirCallResolver
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.resolve.DoubleColonLHS
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder

data class CallInfo(
    override val callSite: FirElement,
    val callKind: CallKind,
    override val name: Name,

    override val explicitReceiver: FirExpression?,
    override val argumentList: FirArgumentList,
    override val isImplicitInvoke: Boolean,
    val isUsedAsGetClassReceiver: Boolean,

    val typeArguments: List<FirTypeProjection>,
    val session: FirSession,
    override val containingFile: FirFile,
    val containingDeclarations: List<FirDeclaration>,

    val candidateForCommonInvokeReceiver: Candidate? = null,

    val resolutionMode: ResolutionMode,

    // Five properties for callable references only
    val expectedType: ConeKotlinType? = null,
    val outerCSBuilder: ConstraintSystemBuilder? = null,
    val lhs: DoubleColonLHS? = null,
    val hasSyntheticOuterCall: Boolean = false,
    val origin: FirFunctionCallOrigin = FirFunctionCallOrigin.Regular,
) : AbstractCallInfo() {
    /**
     * If [argumentList] is a [FirResolvedArgumentList],
     * returns the [FirArgumentList.arguments] of the [FirResolvedArgumentList.originalArgumentList].
     * This means the result still contains [FirNamedArgumentExpression]s that are removed from the
     * [FirResolvedArgumentList] during completion.
     *
     * This is important for Analysis API because it will trigger resolution on already resolved expressions,
     * and we wouldn't otherwise have access to named arguments.
     *
     * @see FirCallResolver.collectAllCandidates
     */
    val arguments: List<FirExpression> get() = (argumentList as? FirResolvedArgumentList)?.originalArgumentList?.arguments ?: argumentList.arguments
    val argumentCount: Int get() = arguments.size

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
