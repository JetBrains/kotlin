/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.fir.declarations.ResolveStateAccess

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed class FirScriptCodeFragment : FirDeclaration() {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract override val symbol: FirBasedSymbol<out FirDeclaration>
    abstract override val moduleData: FirModuleData
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract val statements: List<FirStatement>
    abstract val resultPropertyName: Name?
    abstract val contextReceivers: List<FirContextReceiver>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitScriptCodeFragment(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformScriptCodeFragment(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceStatements(newStatements: List<FirStatement>)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirScriptCodeFragment

    abstract fun <D> transformStatements(transformer: FirTransformer<D>, data: D): FirScriptCodeFragment
}
