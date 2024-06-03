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

    override var sourceSpan: SourceSpan = sourceSpan

    override val signature: IdSignature? = signature

    override var packageFqName: FqName = packageFqName

    override var annotations: List<BirConstructorCall> = annotations

    override val symbol: BirFileSymbol
        get() = this

    override var fileEntry: IrFileEntry = fileEntry

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
