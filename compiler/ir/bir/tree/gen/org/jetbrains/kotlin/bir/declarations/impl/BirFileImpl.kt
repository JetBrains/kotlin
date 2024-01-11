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

    private var _sourceSpan: CompressedSourceSpan = sourceSpan
    /**
     * The span of source code of the syntax node from which this BIR node was generated,
     * in number of characters from the start the source file. If there is no source information for this BIR node,
     * the [SourceSpan.UNDEFINED] is used. In order to get the line number and the column number from this offset,
     * [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
     *
     * @see IrFileEntry.getSourceRangeInfo
     */
    override var sourceSpan: CompressedSourceSpan
        get() {
            recordPropertyRead(5)
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate(5)
            }
        }

    private var _signature: IdSignature? = signature
    override var signature: IdSignature?
        get() {
            recordPropertyRead(6)
            return _signature
        }
        set(value) {
            if (_signature != value) {
                _signature = value
                invalidate(6)
            }
        }

    private var _packageFqName: FqName = packageFqName
    override var packageFqName: FqName
        get() {
            recordPropertyRead(3)
            return _packageFqName
        }
        set(value) {
            if (_packageFqName != value) {
                _packageFqName = value
                invalidate(3)
            }
        }

    private var _fileEntry: IrFileEntry = fileEntry
    override var fileEntry: IrFileEntry
        get() {
            recordPropertyRead(4)
            return _fileEntry
        }
        set(value) {
            if (_fileEntry != value) {
                _fileEntry = value
                invalidate(4)
            }
        }

    override val declarations: BirImplChildElementList<BirDeclaration> = BirImplChildElementList(this, 1, false)
    override val annotations: BirImplChildElementList<BirConstructorCall> = BirImplChildElementList(this, 2, false)

    init {
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        declarations.acceptChildrenLite(visitor)
        annotations.acceptChildrenLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?): Int {
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
