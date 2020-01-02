/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.impl.FirEmptyControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirModifiableConstructor : FirPureAbstractElement(), FirConstructor, FirModifiableTypeParametersOwner, FirAbstractAnnotatedElement {
    abstract override val source: FirSourceElement?
    abstract override val session: FirSession
    abstract override var resolvePhase: FirResolvePhase
    abstract override var returnTypeRef: FirTypeRef
    abstract override var receiverTypeRef: FirTypeRef?
    abstract override var controlFlowGraphReference: FirControlFlowGraphReference
    abstract override val typeParameters: MutableList<FirTypeParameter>
    abstract override val valueParameters: MutableList<FirValueParameter>
    abstract override val name: Name
    abstract override var status: FirDeclarationStatus
    abstract override var containerSource: DeserializedContainerSource?
    abstract override val annotations: MutableList<FirAnnotationCall>
    abstract override val symbol: FirConstructorSymbol
    abstract override var delegatedConstructor: FirDelegatedConstructorCall?
    abstract override var body: FirBlock?
    abstract override val isPrimary: Boolean
    abstract override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirModifiableConstructor

    abstract override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirModifiableConstructor

    abstract override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirModifiableConstructor

    abstract override fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirModifiableConstructor

    abstract override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirModifiableConstructor

    abstract override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirModifiableConstructor

    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)
}
