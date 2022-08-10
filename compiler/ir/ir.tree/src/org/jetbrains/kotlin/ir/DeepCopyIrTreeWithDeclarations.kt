/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.DeepCopyTypeRemapper
import org.jetbrains.kotlin.ir.util.NullDescriptorsRemapper
import org.jetbrains.kotlin.ir.visitors.acceptVoid

@Suppress("UNCHECKED_CAST")
fun <T : IrElement> T.deepCopyWithVariables(): T {
    val symbolsRemapper = DeepCopySymbolRemapper(NullDescriptorsRemapper)
    acceptVoid(symbolsRemapper)

    val typesRemapper = DeepCopyTypeRemapper(symbolsRemapper)

    return this.transform(DeepCopyIrTreeWithSymbols(symbolsRemapper, typesRemapper), null) as T
}
