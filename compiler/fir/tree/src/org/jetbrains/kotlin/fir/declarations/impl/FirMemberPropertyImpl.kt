/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.Name

class FirMemberPropertyImpl(
    session: FirSession,
    psi: PsiElement?,
    override val symbol: FirPropertySymbol,
    name: Name,
    visibility: Visibility,
    modality: Modality?,
    isExpect: Boolean,
    isActual: Boolean,
    isOverride: Boolean,
    isConst: Boolean,
    isLateInit: Boolean,
    receiverTypeRef: FirTypeRef?,
    returnTypeRef: FirTypeRef,
    override val isVar: Boolean,
    override var initializer: FirExpression?,
    override var delegate: FirExpression?
) : FirAbstractCallableMember<FirProperty>(
    session, psi, name, visibility, modality, isExpect, isActual, isOverride, receiverTypeRef, returnTypeRef
), FirProperty, FirModifiableAccessorsOwner {
    // TODO: backing field may not exist
    override val backingFieldSymbol = FirBackingFieldSymbol(symbol.callableId)

    override val delegateFieldSymbol: FirDelegateFieldSymbol<FirProperty>? =
        delegate?.let { FirDelegateFieldSymbol(symbol.callableId) }

    override var getter: FirPropertyAccessor? = null

    override var setter: FirPropertyAccessor? = null

    init {
        symbol.bind(this)
        backingFieldSymbol.bind(this)
        delegateFieldSymbol?.bind(this)
        status.isConst = isConst
        status.isLateInit = isLateInit
    }

    override fun <D> transformChildrenWithoutAccessors(transformer: FirTransformer<D>, data: D) {
        initializer = initializer?.transformSingle(transformer, data)
        delegate = delegate?.transformSingle(transformer, data)
        receiverTypeRef = receiverTypeRef?.transformSingle(transformer, data)
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        typeParameters.transformInplace(transformer, data)
        status = status.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        getter = getter?.transformSingle(transformer, data)
        setter = setter?.transformSingle(transformer, data)
        transformChildrenWithoutAccessors(transformer, data)
        // Everything other (annotations, etc.) is done above
        return this
    }
}