/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.FirContextReceiver
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirResolveState
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.asResolveState
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.ResolveStateAccess

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirScriptImpl(
    override val source: KtSourceElement?,
    resolvePhase: FirResolvePhase,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
    override val moduleData: FirModuleData,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override val name: Name,
    override var statements: MutableOrEmptyList<FirStatement>,
    override val symbol: FirScriptSymbol,
    override val parameters: MutableList<FirVariable>,
    override var contextReceivers: MutableOrEmptyList<FirContextReceiver>,
    override val resultPropertyName: Name?,
) : FirScript() {
    init {
        symbol.bind(this)
        @OptIn(ResolveStateAccess::class)
        resolveState = resolvePhase.asResolveState()
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        statements.forEach { it.accept(visitor, data) }
        parameters.forEach { it.accept(visitor, data) }
        contextReceivers.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirScriptImpl {
        transformAnnotations(transformer, data)
        transformStatements(transformer, data)
        parameters.transformInplace(transformer, data)
        contextReceivers.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirScriptImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformStatements(transformer: FirTransformer<D>, data: D): FirScriptImpl {
        statements.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceStatements(newStatements: List<FirStatement>) {
        statements = newStatements.toMutableOrEmpty()
    }
}
