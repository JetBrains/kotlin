/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

internal val FirCallableSymbol<*>.isTypeAliasedConstructor: Boolean
    get() = origin == FirDeclarationOrigin.Synthetic.TypeAliasConstructor
