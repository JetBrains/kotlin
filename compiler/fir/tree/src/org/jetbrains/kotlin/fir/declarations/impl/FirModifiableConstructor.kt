/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.impl.FirEmptyControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirModifiableConstructor : FirConstructor, FirModifiableTypeParametersOwner, FirAbstractAnnotatedElement {
    override val psi: PsiElement?
    override val session: FirSession
    override var resolvePhase: FirResolvePhase
    override var returnTypeRef: FirTypeRef
    override var receiverTypeRef: FirTypeRef?
    override var controlFlowGraphReference: FirControlFlowGraphReference
    override val typeParameters: MutableList<FirTypeParameter>
    override val valueParameters: MutableList<FirValueParameter>
    override var body: FirBlock?
    override val name: Name
    override var status: FirDeclarationStatus
    override var containerSource: DeserializedContainerSource?
    override val annotations: MutableList<FirAnnotationCall>
    override val symbol: FirConstructorSymbol
    override var delegatedConstructor: FirDelegatedConstructorCall?
    override val isPrimary: Boolean
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirModifiableConstructor

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirModifiableConstructor

    override fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirModifiableConstructor

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirModifiableConstructor

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)
}
