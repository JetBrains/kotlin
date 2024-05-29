/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "CanBePrimaryConstructorProperty")

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirDeclaration
import org.jetbrains.kotlin.bir.declarations.BirFile
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirFileSymbol
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.FqName

class BirFileImpl(
    sourceSpan: SourceSpan,
    signature: IdSignature?,
    packageFqName: FqName,
    annotations: List<BirConstructorCall>,
    fileEntry: IrFileEntry,
) : BirFile(), BirFileSymbol {
    constructor(
        sourceSpan: SourceSpan,
        packageFqName: FqName,
        fileEntry: IrFileEntry,
    ) : this(
        sourceSpan = sourceSpan,
        signature = null,
        packageFqName = packageFqName,
        annotations = emptyList(),
        fileEntry = fileEntry,
    )

    override val owner: BirFileImpl
        get() = this

    override val isBound: Boolean
        get() = true

    private var _sourceSpan: SourceSpan = sourceSpan
    override var sourceSpan: SourceSpan
        get() {
            recordPropertyRead()
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate()
            }
        }

    override val signature: IdSignature? = signature

    private var _packageFqName: FqName = packageFqName
    override var packageFqName: FqName
        get() {
            recordPropertyRead()
            return _packageFqName
        }
        set(value) {
            if (_packageFqName != value) {
                _packageFqName = value
                invalidate()
            }
        }

    private var _annotations: List<BirConstructorCall> = annotations
    override var annotations: List<BirConstructorCall>
        get() {
            recordPropertyRead()
            return _annotations
        }
        set(value) {
            if (_annotations != value) {
                _annotations = value
                invalidate()
            }
        }

    override val symbol: BirFileSymbol
        get() = this

    private var _fileEntry: IrFileEntry = fileEntry
    override var fileEntry: IrFileEntry
        get() {
            recordPropertyRead()
            return _fileEntry
        }
        set(value) {
            if (_fileEntry != value) {
                _fileEntry = value
                invalidate()
            }
        }

    override val declarations: BirImplChildElementList<BirDeclaration> = BirImplChildElementList(this, 1, false)


    init {
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        declarations.acceptChildrenLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> {
        return when (id) {
            1 -> this.declarations
            else -> throwChildrenListWithIdNotFound(id)
        }
    }
}
