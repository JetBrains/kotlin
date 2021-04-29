/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirTypeAliasImpl(
    override val source: FirSourceElement?,
    override val moduleData: FirModuleData,
    override var resolvePhase: FirResolvePhase,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override var status: FirDeclarationStatus,
    override val typeParameters: MutableList<FirTypeParameter>,
    override val name: Name,
    override val symbol: FirTypeAliasSymbol,
    override var expandedTypeRef: FirTypeRef,
    override val annotations: MutableList<FirAnnotationCall>,
) : FirTypeAlias() {
    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        status.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
        expandedTypeRef.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirTypeAliasImpl {
        transformStatus(transformer, data)
        transformTypeParameters(transformer, data)
        transformExpandedTypeRef(transformer, data)
        transformAnnotations(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirTypeAliasImpl {
        status = status.transform(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirTypeAliasImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformExpandedTypeRef(transformer: FirTransformer<D>, data: D): FirTypeAliasImpl {
        expandedTypeRef = expandedTypeRef.transform(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirTypeAliasImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceExpandedTypeRef(newExpandedTypeRef: FirTypeRef) {
        expandedTypeRef = newExpandedTypeRef
    }
}
