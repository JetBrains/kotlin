/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid

inline fun <reified T : IrElement> T.deepCopyWithSymbols(
    initialParent: IrDeclarationParent? = null,
    createTypeRemapper: (SymbolRemapper) -> TypeRemapper = ::DeepCopyTypeRemapper
): T {
    return (deepCopyImpl(createTypeRemapper) as T).patchDeclarationParents(initialParent)
}

inline fun <reified T : IrElement> T.deepCopyWithoutPatchingParents(): T {
    return deepCopyImpl(::DeepCopyTypeRemapper) as T
}

@PublishedApi
internal inline fun <T : IrElement> T.deepCopyImpl(createTypeRemapper: (SymbolRemapper) -> TypeRemapper): IrElement {
    val symbolRemapper = DeepCopySymbolRemapper()
    acceptVoid(symbolRemapper)
    val typeRemapper = createTypeRemapper(symbolRemapper)
    return transform(DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper), null)
}

