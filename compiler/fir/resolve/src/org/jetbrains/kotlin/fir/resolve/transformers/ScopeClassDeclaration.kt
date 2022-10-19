/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

data class ScopeClassDeclaration(
    val scopes: List<FirScope>,
    val containingDeclarations: List<FirDeclaration>,
    val owningSymbol: FirBasedSymbol<*>?,
    val topContainer: FirDeclaration? = null,
)