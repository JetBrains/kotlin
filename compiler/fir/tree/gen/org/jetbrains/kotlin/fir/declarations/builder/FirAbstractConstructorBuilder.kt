/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.DeprecationsPerUseSite
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.builder.FirFunctionBuilder
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
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
    abstract override var source: KtSourceElement?
    abstract override val annotations: MutableList<FirAnnotation>
    abstract override var moduleData: FirModuleData
    abstract override var resolvePhase: FirResolvePhase
    abstract override var origin: FirDeclarationOrigin
    abstract override var attributes: FirDeclarationAttributes
    abstract override var status: FirDeclarationStatus
    abstract override var returnTypeRef: FirTypeRef
    abstract override var deprecation: DeprecationsPerUseSite?
    abstract override var containerSource: DeserializedContainerSource?
    abstract override var dispatchReceiverType: ConeKotlinType?
    abstract override val valueParameters: MutableList<FirValueParameter>
    abstract override var body: FirBlock?
    abstract val typeParameters: MutableList<FirTypeParameterRef>
    abstract var receiverTypeRef: FirTypeRef?
    abstract var controlFlowGraphReference: FirControlFlowGraphReference?
    abstract var symbol: FirConstructorSymbol
    abstract var delegatedConstructor: FirDelegatedConstructorCall?
    override fun build(): FirConstructor
}
