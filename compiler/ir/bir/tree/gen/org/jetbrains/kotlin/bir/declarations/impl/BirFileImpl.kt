/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirDeclaration
import org.jetbrains.kotlin.bir.declarations.BirFile
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.FqName

class BirFileImpl(
    sourceSpan: CompressedSourceSpan,
    signature: IdSignature?,
    packageFqName: FqName,
    fileEntry: IrFileEntry,
) : BirFile(BirFile) {
    override val owner: BirFileImpl
        get() = this

    /**
     * The span of source code of the syntax node from which this BIR node was generated,
     * in number of characters from the start the source file. If there is no source information for this BIR node,
     * the [SourceSpan.UNDEFINED] is used. In order to get the line number and the column number from this offset,
     * [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
     *
     * @see IrFileEntry.getSourceRangeInfo
     */
    override var sourceSpan: CompressedSourceSpan = sourceSpan

    override var signature: IdSignature? = signature

    override var packageFqName: FqName = packageFqName

    override var fileEntry: IrFileEntry = fileEntry

    override val declarations: BirImplChildElementList<BirDeclaration> = BirImplChildElementList(this, 1, false)
    override val annotations: BirImplChildElementList<BirConstructorCall> = BirImplChildElementList(this, 2, false)

    init {
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        declarations.acceptChildrenLite(visitor)
        annotations.acceptChildrenLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> {
        return when (id) {
            1 -> this.declarations
            2 -> this.annotations
            else -> throwChildrenListWithIdNotFound(id)
        }
    }
}
