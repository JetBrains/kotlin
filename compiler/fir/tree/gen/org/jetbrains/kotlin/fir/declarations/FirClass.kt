/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirClass<F : FirClass<F>> : FirClassLikeDeclaration<F>, FirStatement, FirTypeParameterRefsOwner {
    override val source: FirSourceElement?
    override val session: FirSession
    override val resolvePhase: FirResolvePhase
    override val origin: FirDeclarationOrigin
    override val attributes: FirDeclarationAttributes
    override val typeParameters: List<FirTypeParameterRef>
    override val symbol: FirClassSymbol<F>
    val classKind: ClassKind
    val superTypeRefs: List<FirTypeRef>
    val declarations: List<FirDeclaration>
    override val annotations: List<FirAnnotationCall>
    val scopeProvider: FirScopeProvider

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitClass(this, data)

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    fun replaceSuperTypeRefs(newSuperTypeRefs: List<FirTypeRef>)

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirClass<F>

    fun <D> transformSuperTypeRefs(transformer: FirTransformer<D>, data: D): FirClass<F>

    fun <D> transformDeclarations(transformer: FirTransformer<D>, data: D): FirClass<F>

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirClass<F>
}
