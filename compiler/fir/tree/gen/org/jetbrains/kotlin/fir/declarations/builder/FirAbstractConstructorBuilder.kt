/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.declarations.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

@FirBuilderDsl
interface FirAbstractConstructorBuilder : FirFunctionBuilder {
    abstract override var source: KtSourceElement?
    abstract override var resolvePhase: FirResolvePhase
    abstract override val annotations: MutableList<FirAnnotation>
    abstract override var moduleData: FirModuleData
    abstract override var origin: FirDeclarationOrigin
    abstract override var attributes: FirDeclarationAttributes
    abstract override var status: FirDeclarationStatus
    abstract override var returnTypeRef: FirTypeRef
    abstract override var deprecationsProvider: DeprecationsProvider
    abstract override var containerSource: DeserializedContainerSource?
    abstract override var dispatchReceiverType: ConeSimpleKotlinType?
    abstract override val contextReceivers: MutableList<FirContextReceiver>
    abstract override val valueParameters: MutableList<FirValueParameter>
    abstract override var body: FirBlock?
    abstract val typeParameters: MutableList<FirTypeParameterRef>
    abstract var receiverParameter: FirReceiverParameter?
    abstract var controlFlowGraphReference: FirControlFlowGraphReference?
    abstract var contractDescription: FirContractDescription
    abstract var symbol: FirConstructorSymbol
    abstract var delegatedConstructor: FirDelegatedConstructorCall?
    override fun build(): FirConstructor
}
