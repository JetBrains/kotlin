/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirClassLikeDeclaration<F : FirClassLikeDeclaration<F>> : FirStatement, FirMemberDeclaration, FirSymbolOwner<F>, FirTypeParametersOwner {
    override val source: FirSourceElement?
    override val annotations: List<FirAnnotationCall>
    override val session: FirSession
    override val resolvePhase: FirResolvePhase
    override val name: Name
    override val typeParameters: List<FirTypeParameter>
    override val status: FirDeclarationStatus
    val supertypesComputationStatus: SupertypesComputationStatus
    override val symbol: FirClassLikeSymbol<F>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitClassLikeDeclaration(this, data)

    fun replaceSupertypesComputationStatus(newSupertypesComputationStatus: SupertypesComputationStatus)
}
