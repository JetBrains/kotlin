/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirCodeFragmentSymbol
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.fir.declarations.ResolveStateAccess

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirCodeFragment : FirDeclaration() {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract override val moduleData: FirModuleData
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val symbol: FirCodeFragmentSymbol
    abstract val block: FirBlock

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitCodeFragment(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformCodeFragment(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceBlock(newBlock: FirBlock)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirCodeFragment

    abstract fun <D> transformBlock(transformer: FirTransformer<D>, data: D): FirCodeFragment
}
