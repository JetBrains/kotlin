/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirProperty : FirVariable<FirProperty>, FirControlFlowGraphOwner, FirTypeParametersOwner, FirCallableMemberDeclaration<FirProperty> {
    override val psi: PsiElement?
    override val session: FirSession
    override val resolvePhase: FirResolvePhase
    override val returnTypeRef: FirTypeRef
    override val receiverTypeRef: FirTypeRef?
    override val name: Name
    override val initializer: FirExpression?
    override val delegate: FirExpression?
    override val delegateFieldSymbol: FirDelegateFieldSymbol<FirProperty>?
    override val isVar: Boolean
    override val isVal: Boolean
    override val getter: FirPropertyAccessor?
    override val setter: FirPropertyAccessor?
    override val annotations: List<FirAnnotationCall>
    override val controlFlowGraphReference: FirControlFlowGraphReference
    override val containerSource: DeserializedContainerSource?
    override val symbol: FirPropertySymbol
    val backingFieldSymbol: FirBackingFieldSymbol
    val isLocal: Boolean
    override val typeParameters: List<FirTypeParameter>
    override val status: FirDeclarationStatus

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitProperty(this, data)

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirProperty

    override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirProperty

    override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirProperty

    override fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirProperty

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirProperty
}
