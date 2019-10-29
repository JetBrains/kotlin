/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class FirJavaField(
    override val source: FirSourceElement?,
    override val session: FirSession,
    override val symbol: FirFieldSymbol,
    override val name: Name,
    visibility: Visibility,
    modality: Modality?,
    override var returnTypeRef: FirTypeRef,
    override val isVar: Boolean,
    isStatic: Boolean
) : FirAbstractAnnotatedElement, FirField() {
    init {
        symbol.bind(this)
    }

    override var status: FirDeclarationStatus = FirDeclarationStatusImpl(visibility, modality).apply {
        this.isStatic = isStatic
        isExpect = false
        isActual = false
        isOverride = false
    }
    override val receiverTypeRef: FirTypeRef? get() = null
    override var resolvePhase: FirResolvePhase = FirResolvePhase.DECLARATIONS
    override val isVal: Boolean = true
    override val getter: FirPropertyAccessor? get() = null
    override val setter: FirPropertyAccessor? get() = null
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override val typeParameters: MutableList<FirTypeParameter> = mutableListOf()

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirField {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirField {
        return this
    }

    override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirField {
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirField {
        annotations.transformInplace(transformer, data)
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirField {
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirJavaField {
        transformReturnTypeRef(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirJavaField {
        status = status.transformSingle(transformer, data)
        return this
    }

    override val delegate: FirExpression?
        get() = null

    override val initializer: FirExpression?
        get() = null

    override val delegateFieldSymbol: FirDelegateFieldSymbol<FirField>?
        get() = null

    override var containerSource: DeserializedContainerSource? = null
}