/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.BaseTransformedType
import org.jetbrains.kotlin.fir.symbols.FirSymbolOwner
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.visitors.FirVisitor

// May be all containers should be properties and not base classes
// About descriptors: introduce something like FirDescriptor which is FirUnresolved at the beginning and FirSymbol(descriptor) at the end
@BaseTransformedType
interface FirClass : FirDeclarationContainer, FirMemberDeclaration, FirSymbolOwner<FirClass> {
    // including delegated types
    val superTypes: List<FirType>

    val classKind: ClassKind

    val isInner: Boolean

    val isCompanion: Boolean

    val isData: Boolean

    override val symbol: FirClassSymbol

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitClass(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super.acceptChildren(visitor, data)
        for (superType in superTypes) {
            superType.accept(visitor, data)
        }
        for (declaration in declarations) {
            declaration.accept(visitor, data)
        }
    }
}