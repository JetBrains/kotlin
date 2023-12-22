/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirField
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirFieldImpl(
    sourceSpan: SourceSpan,
    signature: IdSignature?,
    origin: IrDeclarationOrigin,
    name: Name,
    isExternal: Boolean,
    visibility: DescriptorVisibility,
    type: BirType,
    isFinal: Boolean,
    isStatic: Boolean,
    initializer: BirExpressionBody?,
    correspondingPropertySymbol: BirPropertySymbol?,
) : BirImplElementBase(BirField), BirField {
    override val owner: BirFieldImpl
        get() = this

    private var _sourceSpan: SourceSpan = sourceSpan
    /**
     * The span of source code of the syntax node from which this BIR node was generated,
     * in number of characters from the start the source file. If there is no source information for this BIR node,
     * the [SourceSpan.UNDEFINED] is used. In order to get the line number and the column number from this offset,
     * [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
     *
     * @see IrFileEntry.getSourceRangeInfo
     */
    override var sourceSpan: SourceSpan
        get() {
            recordPropertyRead(11)
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate(11)
            }
        }

    private var _signature: IdSignature? = signature
    override var signature: IdSignature?
        get() {
            recordPropertyRead(12)
            return _signature
        }
        set(value) {
            if (_signature != value) {
                _signature = value
                invalidate(12)
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

    private var _name: Name = name
    override var name: Name
        get() {
            recordPropertyRead(4)
            return _name
        }
        set(value) {
            if (_name != value) {
                _name = value
                invalidate(4)
            }
        }

    private var _isExternal: Boolean = isExternal
    override var isExternal: Boolean
        get() {
            recordPropertyRead(5)
            return _isExternal
        }
        set(value) {
            if (_isExternal != value) {
                _isExternal = value
                invalidate(5)
            }
        }

    private var _visibility: DescriptorVisibility = visibility
    override var visibility: DescriptorVisibility
        get() {
            recordPropertyRead(6)
            return _visibility
        }
        set(value) {
            if (_visibility != value) {
                _visibility = value
                invalidate(6)
            }
        }

    private var _type: BirType = type
    override var type: BirType
        get() {
            recordPropertyRead(7)
            return _type
        }
        set(value) {
            if (_type != value) {
                _type = value
                invalidate(7)
            }
        }

    private var _isFinal: Boolean = isFinal
    override var isFinal: Boolean
        get() {
            recordPropertyRead(8)
            return _isFinal
        }
        set(value) {
            if (_isFinal != value) {
                _isFinal = value
                invalidate(8)
            }
        }

    private var _isStatic: Boolean = isStatic
    override var isStatic: Boolean
        get() {
            recordPropertyRead(9)
            return _isStatic
        }
        set(value) {
            if (_isStatic != value) {
                _isStatic = value
                invalidate(9)
            }
        }

    private var _initializer: BirExpressionBody? = initializer
    override var initializer: BirExpressionBody?
        get() {
            recordPropertyRead(2)
            return _initializer
        }
        set(value) {
            if (_initializer != value) {
                childReplaced(_initializer, value)
                _initializer = value
                invalidate(2)
            }
        }

    private var _correspondingPropertySymbol: BirPropertySymbol? = correspondingPropertySymbol
    override var correspondingPropertySymbol: BirPropertySymbol?
        get() {
            recordPropertyRead(10)
            return _correspondingPropertySymbol
        }
        set(value) {
            if (_correspondingPropertySymbol != value) {
                _correspondingPropertySymbol = value
                invalidate(10)
            }
        }

    override val annotations: BirImplChildElementList<BirConstructorCall> = BirImplChildElementList(this, 1, false)

    init {
        initChild(_initializer)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        annotations.acceptChildrenLite(visitor)
        _initializer?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?): Int {
        return when {
            this._initializer === old -> {
                this._initializer = new as BirExpressionBody?
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
