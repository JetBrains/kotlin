/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirErrorFunction : FirFunction<FirErrorFunction> {
    override val psi: PsiElement?
    override val session: FirSession
    override val resolvePhase: FirResolvePhase
    override val annotations: List<FirAnnotationCall>
    override val returnTypeRef: FirTypeRef
    override val receiverTypeRef: FirTypeRef?
    override val controlFlowGraphReference: FirControlFlowGraphReference
    override val typeParameters: List<FirTypeParameter>
    override val valueParameters: List<FirValueParameter>
    override val body: FirBlock?
    val reason: String
    override val symbol: FirErrorFunctionSymbol

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitErrorFunction(this, data)

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirErrorFunction

    override fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirErrorFunction

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirErrorFunction
}
