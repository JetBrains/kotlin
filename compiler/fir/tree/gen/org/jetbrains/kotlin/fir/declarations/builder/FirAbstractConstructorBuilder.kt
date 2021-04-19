/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.builder

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.builder.FirFunctionBuilder
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
interface FirAbstractConstructorBuilder : FirFunctionBuilder {
    abstract override var source: FirSourceElement?
    abstract override var session: FirSession
    abstract override var resolvePhase: FirResolvePhase
    abstract override var origin: FirDeclarationOrigin
    abstract override var attributes: FirDeclarationAttributes
    abstract override val annotations: MutableList<FirAnnotationCall>
    abstract override var returnTypeRef: FirTypeRef
    abstract override val valueParameters: MutableList<FirValueParameter>
    abstract override var body: FirBlock?
    abstract var receiverTypeRef: FirTypeRef?
    abstract val typeParameters: MutableList<FirTypeParameterRef>
    abstract var controlFlowGraphReference: FirControlFlowGraphReference?
    abstract var status: FirDeclarationStatus
    abstract var containerSource: DeserializedContainerSource?
    abstract var dispatchReceiverType: ConeKotlinType?
    abstract var symbol: FirConstructorSymbol
    abstract var delegatedConstructor: FirDelegatedConstructorCall?
    override fun build(): FirConstructor
}
