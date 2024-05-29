/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.symbols.BirFileSymbol
import org.jetbrains.kotlin.bir.util.BirImplementationDetail
import org.jetbrains.kotlin.ir.IrFileEntry

abstract class BirFile() : BirPackageFragment(), BirMutableAnnotationContainer, BirMetadataSourceOwner {
    abstract override val symbol: BirFileSymbol

    abstract var fileEntry: IrFileEntry

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        declarations.acceptChildren(visitor, data)
    }

    @BirImplementationDetail
    override fun getElementClassInternal(): BirElementClass<*> = BirFile

    companion object : BirElementClass<BirFile>(BirFile::class.java, 47, true)
}
