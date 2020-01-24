/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirModifiableClass<F : FirClass<F>>  : FirClass<F>, FirAbstractAnnotatedElement {
    override val source: FirSourceElement?
    override val session: FirSession
    override var resolvePhase: FirResolvePhase
    override val symbol: FirClassSymbol<F>
    override val classKind: ClassKind
    override val superTypeRefs: MutableList<FirTypeRef>
    override val declarations: MutableList<FirDeclaration>
    override val annotations: MutableList<FirAnnotationCall>
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirModifiableClass<F>

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    override fun replaceSuperTypeRefs(newSuperTypeRefs: List<FirTypeRef>)
}
