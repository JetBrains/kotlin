/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name

/**
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTree.regularClass]
 */
abstract class FirRegularClass : FirClass() {
    abstract override val source: KtSourceElement?
    abstract override val moduleData: FirModuleData
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val typeParameters: List<FirTypeParameterRef>
    abstract override val status: FirDeclarationStatus
    abstract override val deprecationsProvider: DeprecationsProvider
    abstract override val scopeProvider: FirScopeProvider
    abstract override val controlFlowGraphReference: FirControlFlowGraphReference?
    abstract override val classKind: ClassKind
    @DirectDeclarationsAccess
    abstract override val declarations: List<FirDeclaration>
    abstract override val annotations: List<FirAnnotation>
    abstract val name: Name
    abstract override val symbol: FirRegularClassSymbol
    abstract val hasLazyNestedClassifiers: Boolean
    abstract val companionObjectSymbol: FirRegularClassSymbol?
    abstract override val superTypeRefs: List<FirTypeRef>
    abstract val contextParameters: List<FirValueParameter>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitRegularClass(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformRegularClass(this, data) as E

    abstract override fun replaceStatus(newStatus: FirDeclarationStatus)

    abstract override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider)

    abstract override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceCompanionObjectSymbol(newCompanionObjectSymbol: FirRegularClassSymbol?)

    abstract override fun replaceSuperTypeRefs(newSuperTypeRefs: List<FirTypeRef>)

    abstract override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirRegularClass

    abstract override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirRegularClass

    abstract override fun <D> transformDeclarations(transformer: FirTransformer<D>, data: D): FirRegularClass

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirRegularClass

    abstract override fun <D> transformSuperTypeRefs(transformer: FirTransformer<D>, data: D): FirRegularClass

    abstract fun <D> transformContextParameters(transformer: FirTransformer<D>, data: D): FirRegularClass
}
