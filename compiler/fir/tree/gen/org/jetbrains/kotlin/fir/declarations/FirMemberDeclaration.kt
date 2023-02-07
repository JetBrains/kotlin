/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed class FirMemberDeclaration : FirDeclaration(), FirTypeParameterRefsOwner {
    abstract override val source: KtSourceElement?
    abstract override val resolvePhase: FirResolvePhase
    abstract override val annotations: List<FirAnnotation>
    abstract override val symbol: FirBasedSymbol<out FirDeclaration>
    abstract override val moduleData: FirModuleData
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val typeParameters: List<FirTypeParameterRef>
    abstract val status: FirDeclarationStatus

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitMemberDeclaration(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformMemberDeclaration(this, data) as E

    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceStatus(newStatus: FirDeclarationStatus)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirMemberDeclaration

    abstract override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirMemberDeclaration

    abstract fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirMemberDeclaration
}
