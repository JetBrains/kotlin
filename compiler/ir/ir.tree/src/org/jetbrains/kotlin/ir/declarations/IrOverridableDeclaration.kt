/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.symbols.IrSymbol

interface IrOverridableDeclaration<S : IrSymbol> : IrOverridableMember {
    override val symbol: S
    val isFakeOverride: Boolean
    var overriddenSymbols: List<S>
}
