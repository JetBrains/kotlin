/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType

private object InheritedKtPrivateClassDataKey : FirDeclarationDataKey()

var FirCallableDeclaration.inheritedKtPrivateCls: ConeKotlinType? by FirDeclarationDataRegistry.data(InheritedKtPrivateClassDataKey)

val FirCallableSymbol<*>.inheritedKtPrivateCls: ConeKotlinType?
    get() = fir.inheritedKtPrivateCls
