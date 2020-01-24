/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirSealedClass : FirPureAbstractElement(), FirRegularClass {
    abstract override val source: FirSourceElement?
    abstract override val session: FirSession
    abstract override val resolvePhase: FirResolvePhase
    abstract override val name: Name
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val typeParameters: List<FirTypeParameter>
    abstract override val status: FirDeclarationStatus
    abstract override val classKind: ClassKind
    abstract override val declarations: List<FirDeclaration>
    abstract override val symbol: FirRegularClassSymbol
    abstract override val companionObject: FirRegularClass?
    abstract override val superTypeRefs: List<FirTypeRef>
    abstract val inheritors: List<ClassId>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitSealedClass(this, data)

    abstract fun replaceInheritors(newInheritors: List<ClassId>)

    abstract override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirSealedClass
}
