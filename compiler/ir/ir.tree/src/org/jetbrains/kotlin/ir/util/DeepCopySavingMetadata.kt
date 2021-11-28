/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid

fun <T : IrElement> T.deepCopySavingMetadata(
    initialParent: IrDeclarationParent? = null,
    symbolRemapper: DeepCopySymbolRemapper = DeepCopySymbolRemapper()
): T {
    acceptVoid(symbolRemapper)
    val typeRemapper = DeepCopyTypeRemapper(symbolRemapper)
    @Suppress("UNCHECKED_CAST")
    return transform(DeepCopySavingMetadata(symbolRemapper, typeRemapper, SymbolRenamer.DEFAULT), null)
        .patchDeclarationParents(initialParent) as T
}

private class DeepCopySavingMetadata(
    symbolRemapper: SymbolRemapper,
    typeRemapper: TypeRemapper,
    symbolRenamer: SymbolRenamer
) : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper, symbolRenamer) {
    override fun visitFile(declaration: IrFile): IrFile =
        super.visitFile(declaration).apply {
            metadata = declaration.metadata
        }

    override fun visitClass(declaration: IrClass): IrClass =
        super.visitClass(declaration).apply {
            metadata = declaration.metadata

        }

    override fun visitConstructor(declaration: IrConstructor): IrConstructor =
        super.visitConstructor(declaration).apply {
            metadata = declaration.metadata
        }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction =
        super.visitSimpleFunction(declaration).apply {
            metadata = declaration.metadata
        }

    override fun visitProperty(declaration: IrProperty): IrProperty =
        super.visitProperty(declaration).apply {
            metadata = declaration.metadata
        }

    override fun visitField(declaration: IrField): IrField =
        super.visitField(declaration).apply {
            metadata = declaration.metadata
        }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrLocalDelegatedProperty =
        super.visitLocalDelegatedProperty(declaration).apply {
            metadata = declaration.metadata
        }
}