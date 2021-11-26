/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirBackingField : FirVariable(), FirTypeParametersOwner, FirStatement {
    abstract override val source: KtSourceElement?
    abstract override val moduleData: FirModuleData
    abstract override val resolvePhase: FirResolvePhase
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val returnTypeRef: FirTypeRef
    abstract override val receiverTypeRef: FirTypeRef?
    abstract override val deprecation: DeprecationsPerUseSite?
    abstract override val containerSource: DeserializedContainerSource?
    abstract override val dispatchReceiverType: ConeKotlinType?
    abstract override val name: Name
    abstract override val delegate: FirExpression?
    abstract override val isVar: Boolean
    abstract override val isVal: Boolean
    abstract override val getter: FirPropertyAccessor?
    abstract override val setter: FirPropertyAccessor?
    abstract override val backingField: FirBackingField?
    abstract override val symbol: FirBackingFieldSymbol
    abstract val propertySymbol: FirPropertySymbol
    abstract override val initializer: FirExpression?
    abstract override val annotations: List<FirAnnotation>
    abstract override val typeParameters: List<FirTypeParameter>
    abstract override val status: FirDeclarationStatus

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitBackingField(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D> transform(transformer: FirTransformer<D>, data: D): E = 
        transformer.transformBackingField(this, data) as E

    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    abstract override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?)

    abstract override fun replaceDeprecation(newDeprecation: DeprecationsPerUseSite?)

    abstract override fun replaceGetter(newGetter: FirPropertyAccessor?)

    abstract override fun replaceSetter(newSetter: FirPropertyAccessor?)

    abstract override fun replaceInitializer(newInitializer: FirExpression?)

    abstract override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirBackingField

    abstract override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirBackingField

    abstract override fun <D> transformDelegate(transformer: FirTransformer<D>, data: D): FirBackingField

    abstract override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirBackingField

    abstract override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirBackingField

    abstract override fun <D> transformBackingField(transformer: FirTransformer<D>, data: D): FirBackingField

    abstract override fun <D> transformInitializer(transformer: FirTransformer<D>, data: D): FirBackingField

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirBackingField

    abstract override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirBackingField

    abstract override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirBackingField

    abstract override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirBackingField
}
