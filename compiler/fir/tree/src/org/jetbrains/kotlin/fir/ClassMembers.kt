/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirCallableMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeIntersectionType

fun FirCallableSymbol<*>.dispatchReceiverClassOrNull(): ConeClassLikeLookupTag? =
    (fir as? FirCallableMemberDeclaration<*>)?.dispatchReceiverClassOrNull()

fun FirCallableDeclaration<*>.dispatchReceiverClassOrNull(): ConeClassLikeLookupTag? {
    if (this !is FirCallableMemberDeclaration<*>) return null
    if (dispatchReceiverType is ConeIntersectionType && symbol.isIntersectionOverride) return symbol.overriddenSymbol!!.fir.dispatchReceiverClassOrNull()

    return (dispatchReceiverType as? ConeClassLikeType)?.lookupTag
}

fun FirCallableSymbol<*>.containingClass(): ConeClassLikeLookupTag? = fir.containingClass()
fun FirCallableDeclaration<*>.containingClass(): ConeClassLikeLookupTag? {
    return (containingClassAttr ?: dispatchReceiverClassOrNull())
}

private object ContainingClassKey : FirDeclarationDataKey()
var FirCallableDeclaration<*>.containingClassAttr: ConeClassLikeLookupTag? by FirDeclarationDataRegistry.data(ContainingClassKey)
