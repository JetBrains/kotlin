/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirAnonymousInitializer
import org.jetbrains.kotlin.bir.expressions.BirBlockBody
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature

class BirAnonymousInitializerImpl(
    sourceSpan: CompressedSourceSpan,
    signature: IdSignature?,
    origin: IrDeclarationOrigin,
    isStatic: Boolean,
    body: BirBlockBody?,
) : BirAnonymousInitializer(BirAnonymousInitializer) {
    override val owner: BirAnonymousInitializerImpl
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

    private var _origin: IrDeclarationOrigin = origin
    override var origin: IrDeclarationOrigin
        get() {
            recordPropertyRead(3)
            return _origin
        }
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate(3)
            }
        }

    private var _isStatic: Boolean = isStatic
    override var isStatic: Boolean
        get() {
            recordPropertyRead(4)
            return _isStatic
        }
        set(value) {
            if (_isStatic != value) {
                _isStatic = value
                invalidate(4)
            }
        }

    private var _body: BirBlockBody? = body
    override var body: BirBlockBody?
        get() {
            recordPropertyRead(2)
            return _body
        }
        set(value) {
            if (_body !== value) {
                childReplaced(_body, value)
                _body = value
                invalidate(2)
            }
        }

    override val annotations: BirImplChildElementList<BirConstructorCall> = BirImplChildElementList(this, 1, false)

    init {
        initChild(_body)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        annotations.acceptChildrenLite(visitor)
        _body?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?): Int {
        return when {
            this._body === old -> {
                this._body = new as BirBlockBody?
                2
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
