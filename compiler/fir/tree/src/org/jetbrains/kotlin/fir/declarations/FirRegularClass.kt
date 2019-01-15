/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.BaseTransformedType
import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.symbols.FirSymbolOwner
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor

// May be all containers should be properties and not base classes
// About descriptors: introduce something like FirDescriptor which is FirUnresolved at the beginning and FirSymbol(descriptor) at the end
@BaseTransformedType
interface FirRegularClass : FirClass, @VisitedSupertype FirMemberDeclaration, FirSymbolOwner<FirRegularClass> {
    val isInner: Boolean get() = status.isInner

    val isCompanion: Boolean get() = status.isCompanion

    val isData: Boolean get() = status.isData

    val isInline: Boolean get() = status.isInline

    override val symbol: FirClassSymbol

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitRegularClass(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super<FirMemberDeclaration>.acceptChildren(visitor, data)
        super<FirClass>.acceptChildren(visitor, data)
    }
}