/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

abstract class IrFile : IrPackageFragment(), IrMutableAnnotationContainer, IrMetadataSourceOwner {
    abstract override val symbol: IrFileSymbol

    abstract val module: IrModuleFragment

    abstract val fileEntry: IrFileEntry

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitFile(this, data)

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrFile =
        accept(transformer, data) as IrFile
}
