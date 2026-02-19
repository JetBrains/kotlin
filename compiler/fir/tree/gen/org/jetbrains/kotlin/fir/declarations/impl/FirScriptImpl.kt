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
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.name.Name

@OptIn(FirImplementationDetail::class, ResolveStateAccess::class, DirectDeclarationsAccess::class)
internal class FirScriptImpl(
    resolvePhase: FirResolvePhase,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
    override val moduleData: FirModuleData,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override val name: Name,
    @property:DirectDeclarationsAccess
    override val declarations: MutableList<FirDeclaration>,
    override val source: KtSourceElement,
    override val symbol: FirScriptSymbol,
    override val parameters: MutableList<FirProperty>,
    override var receivers: MutableOrEmptyList<FirScriptReceiverParameter>,
    override val resultPropertyName: Name?,
) : FirScript() {
    override var controlFlowGraphReference: FirControlFlowGraphReference? = null

    init {
        symbol.bind(this)
        resolveState = resolvePhase.asResolveState()
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        controlFlowGraphReference?.accept(visitor, data)
        declarations.forEach { it.accept(visitor, data) }
        parameters.forEach { it.accept(visitor, data) }
        receivers.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirScriptImpl {
        transformAnnotations(transformer, data)
        controlFlowGraphReference = controlFlowGraphReference?.transform(transformer, data)
        transformDeclarations(transformer, data)
        transformParameters(transformer, data)
        transformReceivers(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirScriptImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformDeclarations(transformer: FirTransformer<D>, data: D): FirScriptImpl {
        declarations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformParameters(transformer: FirTransformer<D>, data: D): FirScriptImpl {
        parameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformReceivers(transformer: FirTransformer<D>, data: D): FirScriptImpl {
        receivers.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
        controlFlowGraphReference = newControlFlowGraphReference
    }

    override fun replaceDeclarations(newDeclarations: List<FirDeclaration>) {
        if (declarations === newDeclarations) return
        declarations.clear()
        declarations.addAll(newDeclarations)
    }
}
