/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.name.Name

@OptIn(FirImplementationDetail::class, ResolveStateAccess::class, DirectDeclarationsAccess::class)
internal class FirRegularClassImpl(
    override val source: KtSourceElement?,
    resolvePhase: FirResolvePhase,
    override val moduleData: FirModuleData,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override val typeParameters: MutableList<FirTypeParameterRef>,
    override var status: FirDeclarationStatus,
    override var deprecationsProvider: DeprecationsProvider,
    override val scopeProvider: FirScopeProvider,
    override val classKind: ClassKind,
    @property:DirectDeclarationsAccess
    override val declarations: MutableList<FirDeclaration>,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
    override val name: Name,
    override val symbol: FirRegularClassSymbol,
    override var companionObjectSymbol: FirRegularClassSymbol?,
    override val superTypeRefs: MutableList<FirTypeRef>,
    override var contextParameters: MutableOrEmptyList<FirValueParameter>,
) : FirRegularClass() {
    override var controlFlowGraphReference: FirControlFlowGraphReference? = null
    override val hasLazyNestedClassifiers: Boolean
        get() = false

    init {
        symbol.bind(this)
        resolveState = resolvePhase.asResolveState()
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
        controlFlowGraphReference?.accept(visitor, data)
        declarations.forEach { it.accept(visitor, data) }
        annotations.forEach { it.accept(visitor, data) }
        superTypeRefs.forEach { it.accept(visitor, data) }
        contextParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirRegularClassImpl {
        transformTypeParameters(transformer, data)
        transformStatus(transformer, data)
        controlFlowGraphReference = controlFlowGraphReference?.transform(transformer, data)
        transformDeclarations(transformer, data)
        transformAnnotations(transformer, data)
        transformSuperTypeRefs(transformer, data)
        transformContextParameters(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirRegularClassImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirRegularClassImpl {
        status = status.transform(transformer, data)
        return this
    }

    override fun <D> transformDeclarations(transformer: FirTransformer<D>, data: D): FirRegularClassImpl {
        declarations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirRegularClassImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformSuperTypeRefs(transformer: FirTransformer<D>, data: D): FirRegularClassImpl {
        superTypeRefs.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformContextParameters(transformer: FirTransformer<D>, data: D): FirRegularClassImpl {
        contextParameters.transformInplace(transformer, data)
        return this
    }

    override fun replaceStatus(newStatus: FirDeclarationStatus) {
        status = newStatus
    }

    override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider) {
        deprecationsProvider = newDeprecationsProvider
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
        controlFlowGraphReference = newControlFlowGraphReference
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceCompanionObjectSymbol(newCompanionObjectSymbol: FirRegularClassSymbol?) {
        companionObjectSymbol = newCompanionObjectSymbol
    }

    override fun replaceSuperTypeRefs(newSuperTypeRefs: List<FirTypeRef>) {
        if (superTypeRefs === newSuperTypeRefs) return
        superTypeRefs.clear()
        superTypeRefs.addAll(newSuperTypeRefs)
    }
}
