/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirTypeAlias : FirPureAbstractElement(), FirClassLikeDeclaration<FirTypeAlias>, FirMemberDeclaration, FirTypeParametersOwner {
    abstract override val source: FirSourceElement?
    abstract override val session: FirSession
    abstract override val resolvePhase: FirResolvePhase
    abstract override val name: Name
    abstract override val typeParameters: List<FirTypeParameter>
    abstract override val status: FirDeclarationStatus
    abstract override val symbol: FirTypeAliasSymbol
    abstract val expandedTypeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotationCall>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitTypeAlias(this, data)

    abstract fun replaceExpandedTypeRef(newExpandedTypeRef: FirTypeRef)

    abstract override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirTypeAlias
}
