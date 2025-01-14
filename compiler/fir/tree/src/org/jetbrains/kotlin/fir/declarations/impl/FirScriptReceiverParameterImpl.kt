/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace

@OptIn(FirImplementationDetail::class, ResolveStateAccess::class)
internal class FirScriptReceiverParameterImpl(
    override val source: KtSourceElement?,
    resolvePhase: FirResolvePhase,
    override val moduleData: FirModuleData,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override val symbol: FirReceiverParameterSymbol,
    override val containingDeclarationSymbol: FirBasedSymbol<*>,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
    override var typeRef: FirTypeRef,
    override val isBaseClassReceiver: Boolean,
) : FirScriptReceiverParameter() {

    init {
        symbol.bind(this)
        resolveState = resolvePhase.asResolveState()
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirScriptReceiverParameterImpl {
        transformAnnotations(transformer, data)
        transformTypeRef(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirScriptReceiverParameterImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeRef(transformer: FirTransformer<D>, data: D): FirScriptReceiverParameterImpl {
        typeRef = typeRef.transform(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }
}
