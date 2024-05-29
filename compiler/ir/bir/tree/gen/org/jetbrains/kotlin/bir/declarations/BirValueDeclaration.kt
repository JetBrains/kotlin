/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.symbols.BirValueSymbol
import org.jetbrains.kotlin.bir.types.BirType

interface BirValueDeclaration : BirDeclarationWithName, BirSymbolOwner {
    override val symbol: BirValueSymbol

    var type: BirType

    companion object : BirElementClass<BirValueDeclaration>(BirValueDeclaration::class.java, 99, false)
}
