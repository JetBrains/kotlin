/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed class FirClass : FirClassLikeDeclaration(), FirStatement, FirTypeParameterRefsOwner {
    abstract override val source: KtSourceElement?
    abstract override val resolvePhase: FirResolvePhase
    abstract override val moduleData: FirModuleData
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val typeParameters: List<FirTypeParameterRef>
    abstract override val status: FirDeclarationStatus
    abstract override val deprecationsProvider: DeprecationsProvider
    abstract override val symbol: FirClassSymbol<out FirClass>
    abstract val classKind: ClassKind
    abstract val superTypeRefs: List<FirTypeRef>
    abstract val declarations: List<FirDeclaration>
    abstract override val annotations: List<FirAnnotation>
    abstract val scopeProvider: FirScopeProvider

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitClass(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformClass(this, data) as E

    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceStatus(newStatus: FirDeclarationStatus)

    abstract override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider)

    abstract fun replaceSuperTypeRefs(newSuperTypeRefs: List<FirTypeRef>)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirClass

    abstract override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirClass

    abstract fun <D> transformSuperTypeRefs(transformer: FirTransformer<D>, data: D): FirClass

    abstract fun <D> transformDeclarations(transformer: FirTransformer<D>, data: D): FirClass

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirClass
}
