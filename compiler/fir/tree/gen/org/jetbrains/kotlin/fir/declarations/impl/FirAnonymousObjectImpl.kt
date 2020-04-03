/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.impl.FirEmptyControlFlowGraphReference
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirAnonymousObjectImpl(
    override val source: FirSourceElement?,
    override val session: FirSession,
    override var resolvePhase: FirResolvePhase,
    override val classKind: ClassKind,
    override val superTypeRefs: MutableList<FirTypeRef>,
    override val declarations: MutableList<FirDeclaration>,
    override val annotations: MutableList<FirAnnotationCall>,
    override val scopeProvider: FirScopeProvider,
    override var typeRef: FirTypeRef,
    override val symbol: FirAnonymousObjectSymbol,
) : FirAnonymousObject() {
    override var controlFlowGraphReference: FirControlFlowGraphReference = FirEmptyControlFlowGraphReference

    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        superTypeRefs.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
        annotations.forEach { it.accept(visitor, data) }
        typeRef.accept(visitor, data)
        controlFlowGraphReference.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirAnonymousObjectImpl {
        superTypeRefs.transformInplace(transformer, data)
        declarations.transformInplace(transformer, data)
        transformAnnotations(transformer, data)
        typeRef = typeRef.transformSingle(transformer, data)
        transformControlFlowGraphReference(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirAnonymousObjectImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirAnonymousObjectImpl {
        controlFlowGraphReference = controlFlowGraphReference.transformSingle(transformer, data)
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceSuperTypeRefs(newSuperTypeRefs: List<FirTypeRef>) {
        superTypeRefs.clear()
        superTypeRefs.addAll(newSuperTypeRefs)
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }
}
