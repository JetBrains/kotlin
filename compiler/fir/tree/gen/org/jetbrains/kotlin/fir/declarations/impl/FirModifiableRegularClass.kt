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
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirModifiableRegularClass : FirRegularClass, FirModifiableClass<FirRegularClass>, FirModifiableTypeParametersOwner, FirAbstractAnnotatedElement {
    override val source: FirSourceElement?
    override val session: FirSession
    override var resolvePhase: FirResolvePhase
    override val name: Name
    override val annotations: MutableList<FirAnnotationCall>
    override val typeParameters: MutableList<FirTypeParameter>
    override var status: FirDeclarationStatus
    override val classKind: ClassKind
    override val declarations: MutableList<FirDeclaration>
    override val symbol: FirRegularClassSymbol
    override var companionObject: FirRegularClass?
    override val superTypeRefs: MutableList<FirTypeRef>
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirModifiableRegularClass

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirModifiableRegularClass

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    override fun replaceSuperTypeRefs(newSuperTypeRefs: List<FirTypeRef>)
}
