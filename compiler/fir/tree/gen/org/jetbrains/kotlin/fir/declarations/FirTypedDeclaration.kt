/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed interface FirTypedDeclaration : FirAnnotatedDeclaration {
    override val source: FirSourceElement?
    override val moduleData: FirModuleData
    override val resolvePhase: FirResolvePhase
    override val origin: FirDeclarationOrigin
    override val attributes: FirDeclarationAttributes
    override val annotations: List<FirAnnotationCall>
    val returnTypeRef: FirTypeRef

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitTypedDeclaration(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D> transform(transformer: FirTransformer<D>, data: D): E = 
        transformer.transformTypedDeclaration(this, data) as E

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirTypedDeclaration

    fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirTypedDeclaration
}
