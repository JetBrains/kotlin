/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyFieldDeclarationSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirBackingField : FirTypedDeclaration(), FirTypeParametersOwner, FirStatement {
    abstract override val source: FirSourceElement?
    abstract override val moduleData: FirModuleData
    abstract override val resolvePhase: FirResolvePhase
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val returnTypeRef: FirTypeRef
    abstract override val symbol: FirPropertyFieldDeclarationSymbol
    abstract val backingFieldSymbol: FirBackingFieldSymbol?
    abstract val propertySymbol: FirPropertySymbol?
    abstract val initializer: FirExpression?
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val typeParameters: List<FirTypeParameter>
    abstract val status: FirDeclarationStatus

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitBackingField(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D> transform(transformer: FirTransformer<D>, data: D): E = 
        transformer.transformBackingField(this, data) as E

    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    abstract fun replaceInitializer(newInitializer: FirExpression?)

    abstract override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirBackingField

    abstract fun <D> transformInitializer(transformer: FirTransformer<D>, data: D): FirBackingField

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirBackingField

    abstract override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirBackingField

    abstract fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirBackingField
}
