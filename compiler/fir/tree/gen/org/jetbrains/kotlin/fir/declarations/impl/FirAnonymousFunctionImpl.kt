/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.DeprecationsPerUseSite
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirContextReceiver
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.InlineStatus
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirAnonymousFunctionImpl(
    override val source: KtSourceElement?,
    override val annotations: MutableList<FirAnnotation>,
    override val moduleData: FirModuleData,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override var returnTypeRef: FirTypeRef,
    override var receiverTypeRef: FirTypeRef?,
    override var deprecation: DeprecationsPerUseSite?,
    override val containerSource: DeserializedContainerSource?,
    override val dispatchReceiverType: ConeSimpleKotlinType?,
    override val contextReceivers: MutableList<FirContextReceiver>,
    override var controlFlowGraphReference: FirControlFlowGraphReference?,
    override val valueParameters: MutableList<FirValueParameter>,
    override var body: FirBlock?,
    override val symbol: FirAnonymousFunctionSymbol,
    override var label: FirLabel?,
    override var invocationKind: EventOccurrencesRange?,
    override var inlineStatus: InlineStatus,
    override val isLambda: Boolean,
    override val hasExplicitParameterList: Boolean,
    override val typeParameters: MutableList<FirTypeParameterRef>,
    override var typeRef: FirTypeRef,
) : FirAnonymousFunction() {
    @Volatile
    override var resolvePhase: FirResolvePhase = FirResolvePhase.DECLARATIONS
    override var status: FirDeclarationStatus = FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS

    init {
        symbol.bind(this)
    }

    override val elementKind get() = FirElementKind.AnonymousFunction

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceStatus(newStatus: FirDeclarationStatus) {
        status = newStatus
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?) {
        receiverTypeRef = newReceiverTypeRef
    }

    override fun replaceDeprecation(newDeprecation: DeprecationsPerUseSite?) {
        deprecation = newDeprecation
    }

    override fun replaceContextReceivers(newContextReceivers: List<FirContextReceiver>) {
        contextReceivers.clear()
        contextReceivers.addAll(newContextReceivers)
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
        controlFlowGraphReference = newControlFlowGraphReference
    }

    override fun replaceValueParameters(newValueParameters: List<FirValueParameter>) {
        valueParameters.clear()
        valueParameters.addAll(newValueParameters)
    }

    override fun replaceBody(newBody: FirBlock?) {
        body = newBody
    }

    override fun replaceLabel(newLabel: FirLabel?) {
        label = newLabel
    }

    override fun replaceInvocationKind(newInvocationKind: EventOccurrencesRange?) {
        invocationKind = newInvocationKind
    }

    override fun replaceInlineStatus(newInlineStatus: InlineStatus) {
        inlineStatus = newInlineStatus
    }

    override fun replaceTypeParameters(newTypeParameters: List<FirTypeParameterRef>) {
        typeParameters.clear()
        typeParameters.addAll(newTypeParameters)
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }
}
