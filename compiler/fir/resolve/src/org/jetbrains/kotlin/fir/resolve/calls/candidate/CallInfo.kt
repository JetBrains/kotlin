/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.candidate

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.resolve.DoubleColonLHS
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.AbstractCallInfo
import org.jetbrains.kotlin.fir.resolve.calls.ConeResolutionAtom
import org.jetbrains.kotlin.fir.resolve.calls.ConeResolutionAtom.Companion.createRawAtom
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder

open class CallInfo(
    final override val callSite: FirElement,
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
    val arguments: List<FirExpression>
        get() = (argumentList as? FirResolvedArgumentList)?.originalArgumentList?.arguments ?: argumentList.arguments
    val argumentCount: Int get() = arguments.size
    val argumentAtoms: List<ConeResolutionAtom> = arguments.map { createRawAtom(it) }

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

    open fun copy(
        callKind: CallKind = this.callKind,
        typeArguments: List<FirTypeProjection> = this.typeArguments,
        argumentList: FirArgumentList = this.argumentList,
        explicitReceiver: FirExpression? = this.explicitReceiver,
        name: Name = this.name,
        isImplicitInvoke: Boolean = this.isImplicitInvoke,
        candidateForCommonInvokeReceiver: Candidate? = this.candidateForCommonInvokeReceiver,
    ): CallInfo = CallInfo(
        callSite, callKind, name, explicitReceiver, argumentList,
        isImplicitInvoke, isUsedAsGetClassReceiver, typeArguments,
        session, containingFile, containingDeclarations,
        candidateForCommonInvokeReceiver, resolutionMode, origin
    )
}

class CallableReferenceInfo(
    callSite: FirElement,
    name: Name,
    explicitReceiver: FirExpression?,
    session: FirSession,
    containingFile: FirFile,
    containingDeclarations: List<FirDeclaration>,

    // Five properties for callable references only
    val expectedType: ConeKotlinType?,
    val outerCSBuilder: ConstraintSystemBuilder?,
    val lhs: DoubleColonLHS?,
    val hasSyntheticOuterCall: Boolean,

    origin: FirFunctionCallOrigin = FirFunctionCallOrigin.Regular,
) : CallInfo(
    callSite, CallKind.CallableReference, name, explicitReceiver, FirEmptyArgumentList,
    isImplicitInvoke = false, isUsedAsGetClassReceiver = false, typeArguments = emptyList(),
    session, containingFile, containingDeclarations,
    candidateForCommonInvokeReceiver = null, resolutionMode = ResolutionMode.ContextIndependent, origin
) {
    override fun copy(
        callKind: CallKind,
        typeArguments: List<FirTypeProjection>,
        argumentList: FirArgumentList,
        explicitReceiver: FirExpression?,
        name: Name,
        isImplicitInvoke: Boolean,
        candidateForCommonInvokeReceiver: Candidate?,
    ): CallableReferenceInfo = CallableReferenceInfo(
        callSite, name, explicitReceiver,
        session, containingFile, containingDeclarations,
        expectedType, outerCSBuilder, lhs, hasSyntheticOuterCall, origin
    )
}
