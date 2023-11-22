/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

class FirFileSymbol : FirBasedSymbol<FirFile>() {
    override fun toString(): String = "${this::class.simpleName} ${fir.name}"
}
