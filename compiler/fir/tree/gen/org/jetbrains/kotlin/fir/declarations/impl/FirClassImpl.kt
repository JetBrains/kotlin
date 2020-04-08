/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.impl.FirEmptyControlFlowGraphReference
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirClassImpl(
    override val source: FirSourceElement?,
    override val session: FirSession,
    override var resolvePhase: FirResolvePhase,
    override val annotations: MutableList<FirAnnotationCall>,
    override val typeParameters: MutableList<FirTypeParameter>,
    override var status: FirDeclarationStatus,
    override val classKind: ClassKind,
    override val declarations: MutableList<FirDeclaration>,
    override val scopeProvider: FirScopeProvider,
    override val name: Name,
    override val symbol: FirRegularClassSymbol,
    override var companionObject: FirRegularClass?,
    override val superTypeRefs: MutableList<FirTypeRef>,
) : FirRegularClass() {
    override val hasLazyNestedClassifiers: Boolean get() = false
    override var controlFlowGraphReference: FirControlFlowGraphReference = FirEmptyControlFlowGraphReference

    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
        (declarations.firstOrNull { it is FirConstructorImpl } as? FirConstructorImpl)?.typeParameters?.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
        superTypeRefs.forEach { it.accept(visitor, data) }
        controlFlowGraphReference.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirClassImpl {
        transformAnnotations(transformer, data)
        typeParameters.transformInplace(transformer, data)
        transformStatus(transformer, data)
        (declarations.firstOrNull { it is FirConstructorImpl } as? FirConstructorImpl)?.typeParameters?.transformInplace(transformer, data)
        declarations.transformInplace(transformer, data)
        companionObject = declarations.asSequence().filterIsInstance<FirRegularClass>().firstOrNull { it.status.isCompanion }
        superTypeRefs.transformInplace(transformer, data)
        transformControlFlowGraphReference(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirClassImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirClassImpl {
        status = status.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirClassImpl {
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
}
