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
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.FirEmptyControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name

open class FirConstructorImpl : FirAbstractCallableMember<FirConstructor>, FirConstructor {

    override val symbol: FirConstructorSymbol

    final override var delegatedConstructor: FirDelegatedConstructorCall? = null

    final override var isInner: Boolean
        get() = status.isInner
        set(isInner) {
            status.isInner = isInner
        }

    constructor(
        session: FirSession,
        psi: PsiElement?,
        symbol: FirConstructorSymbol,
        visibility: Visibility,
        isExpect: Boolean,
        isActual: Boolean,
        isInner: Boolean,
        delegatedSelfTypeRef: FirTypeRef,
        delegatedConstructor: FirDelegatedConstructorCall?
    ) : super(
        session, psi, NAME, visibility, Modality.FINAL,
        isExpect, isActual, isOverride = false, receiverTypeRef = null, returnTypeRef = delegatedSelfTypeRef
    ) {
        this.symbol = symbol
        this.isInner = isInner
        this.delegatedConstructor = delegatedConstructor
        symbol.bind(this)
    }

    constructor(
        session: FirSession,
        psi: PsiElement?,
        symbol: FirConstructorSymbol,
        receiverTypeRef: FirTypeRef?,
        returnTypeRef: FirTypeRef
    ) : super(session, psi, NAME, receiverTypeRef, returnTypeRef) {
        this.symbol = symbol
        symbol.bind(this)
    }

    override val valueParameters = mutableListOf<FirValueParameter>()

    override var body: FirBlock? = null

    override var controlFlowGraphReference: FirControlFlowGraphReference = FirEmptyControlFlowGraphReference()

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        annotations.transformInplace(transformer, data)
        valueParameters.transformInplace(transformer, data)
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        status = status.transformSingle(transformer, data)
        delegatedConstructor?.transformSingle(transformer, data)
        body = body?.transformSingle(transformer, data)
        transformControlFlowGraphReference(transformer, data)

        return this
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        valueParameters.forEach { it.accept(visitor, data) }
        returnTypeRef.accept(visitor, data)
        status.accept(visitor, data)
        delegatedConstructor?.accept(visitor, data)
        body?.accept(visitor, data)
        controlFlowGraphReference.accept(visitor, data)
    }

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirConstructorImpl {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirConstructorImpl {
        controlFlowGraphReference = controlFlowGraphReference.transformSingle(transformer, data)
        return this
    }

    companion object {
        val NAME = Name.special("<init>")
    }
}