/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirRegularClass : FirMemberDeclaration, FirTypeParametersOwner, FirClass<FirRegularClass> {
    override val source: FirSourceElement?
    override val session: FirSession
    override val resolvePhase: FirResolvePhase
    override val name: Name
    override val annotations: List<FirAnnotationCall>
    override val typeParameters: List<FirTypeParameter>
    override val status: FirDeclarationStatus
    override val classKind: ClassKind
    override val declarations: List<FirDeclaration>
    override val symbol: FirRegularClassSymbol
    val companionObject: FirRegularClass?
    override val superTypeRefs: List<FirTypeRef>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitRegularClass(this, data)

    override fun replaceSuperTypeRefs(newSuperTypeRefs: List<FirTypeRef>)

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirRegularClass
}
