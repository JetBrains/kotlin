/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirClass
import org.jetbrains.kotlin.bir.declarations.BirEnumEntry
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirEnumEntryImpl(
    sourceSpan: CompressedSourceSpan,
    signature: IdSignature?,
    origin: IrDeclarationOrigin,
    name: Name,
    initializerExpression: BirExpressionBody?,
    correspondingClass: BirClass?,
) : BirImplElementBase(BirEnumEntry), BirEnumEntry {
    override val owner: BirEnumEntryImpl
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

    override var origin: IrDeclarationOrigin = origin

    override var name: Name = name

    private var _initializerExpression: BirExpressionBody? = initializerExpression
    override var initializerExpression: BirExpressionBody?
        get() {
            return _initializerExpression
        }
        set(value) {
            if (_initializerExpression !== value) {
                childReplaced(_initializerExpression, value)
                _initializerExpression = value
            }
        }

    private var _correspondingClass: BirClass? = correspondingClass
    override var correspondingClass: BirClass?
        get() {
            return _correspondingClass
        }
        set(value) {
            if (_correspondingClass !== value) {
                childReplaced(_correspondingClass, value)
                _correspondingClass = value
            }
        }

    override val annotations: BirImplChildElementList<BirConstructorCall> = BirImplChildElementList(this, 1, false)

    init {
        initChild(_initializerExpression)
        initChild(_correspondingClass)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        annotations.acceptChildrenLite(visitor)
        _initializerExpression?.acceptLite(visitor)
        _correspondingClass?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._initializerExpression === old -> {
                this._initializerExpression = new as BirExpressionBody?
            }
            this._correspondingClass === old -> {
                this._correspondingClass = new as BirClass?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> {
        return when (id) {
            1 -> this.annotations
            else -> throwChildrenListWithIdNotFound(id)
        }
    }
}
