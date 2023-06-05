/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.FirCodeFragment
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirResolveState
import org.jetbrains.kotlin.fir.declarations.asResolveState
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirCodeFragmentSymbol
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.ResolveStateAccess

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirCodeFragmentImpl(
    override val source: KtSourceElement?,
    resolvePhase: FirResolvePhase,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
    override val moduleData: FirModuleData,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override val imports: MutableList<FirImport>,
    override val symbol: FirCodeFragmentSymbol,
    override var codeBlock: FirBlock,
) : FirCodeFragment() {
    init {
        symbol.bind(this)
        @OptIn(ResolveStateAccess::class)
        resolveState = resolvePhase.asResolveState()
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        imports.forEach { it.accept(visitor, data) }
        codeBlock.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirCodeFragmentImpl {
        transformAnnotations(transformer, data)
        transformImports(transformer, data)
        transformCodeBlock(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirCodeFragmentImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformImports(transformer: FirTransformer<D>, data: D): FirCodeFragmentImpl {
        imports.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformCodeBlock(transformer: FirTransformer<D>, data: D): FirCodeFragmentImpl {
        codeBlock = codeBlock.transform(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }
}
