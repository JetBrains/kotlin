/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.BirChildElementList
import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.BirImplChildElementList
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.acceptLite
import org.jetbrains.kotlin.bir.declarations.BirLocalDelegatedProperty
import org.jetbrains.kotlin.bir.declarations.BirSimpleFunction
import org.jetbrains.kotlin.bir.declarations.BirVariable
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirLocalDelegatedPropertyImpl @ObsoleteDescriptorBasedAPI constructor(
    sourceSpan: SourceSpan,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: VariableDescriptorWithAccessors?,
    signature: IdSignature?,
    origin: IrDeclarationOrigin,
    name: Name,
    type: BirType,
    isVar: Boolean,
    delegate: BirVariable?,
    getter: BirSimpleFunction?,
    setter: BirSimpleFunction?,
) : BirLocalDelegatedProperty() {
    override val owner: BirLocalDelegatedPropertyImpl
        get() = this

    private var _sourceSpan: SourceSpan = sourceSpan

    override var sourceSpan: SourceSpan
        get() {
            recordPropertyRead(9)
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate(9)
            }
        }

    private var _signature: IdSignature? = signature

    override var signature: IdSignature?
        get() {
            recordPropertyRead(10)
            return _signature
        }
        set(value) {
            if (_signature != value) {
                _signature = value
                invalidate(10)
            }
        }

    override val annotations: BirImplChildElementList<BirConstructorCall> =
            BirImplChildElementList(this, 1, false)

    private var _origin: IrDeclarationOrigin = origin

    override var origin: IrDeclarationOrigin
        get() {
            recordPropertyRead(5)
            return _origin
        }
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate(5)
            }
        }

    private var _name: Name = name

    override var name: Name
        get() {
            recordPropertyRead(6)
            return _name
        }
        set(value) {
            if (_name != value) {
                _name = value
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

    private var _isVar: Boolean = isVar

    override var isVar: Boolean
        get() {
            recordPropertyRead(8)
            return _isVar
        }
        set(value) {
            if (_isVar != value) {
                _isVar = value
                invalidate(8)
            }
        }

    private var _delegate: BirVariable? = delegate

    override var delegate: BirVariable?
        get() {
            recordPropertyRead(2)
            return _delegate
        }
        set(value) {
            if (_delegate != value) {
                childReplaced(_delegate, value)
                _delegate = value
                invalidate(2)
            }
        }

    private var _getter: BirSimpleFunction? = getter

    override var getter: BirSimpleFunction?
        get() {
            recordPropertyRead(3)
            return _getter
        }
        set(value) {
            if (_getter != value) {
                childReplaced(_getter, value)
                _getter = value
                invalidate(3)
            }
        }

    private var _setter: BirSimpleFunction? = setter

    override var setter: BirSimpleFunction?
        get() {
            recordPropertyRead(4)
            return _setter
        }
        set(value) {
            if (_setter != value) {
                childReplaced(_setter, value)
                _setter = value
                invalidate(4)
            }
        }
    init {
        initChild(_delegate)
        initChild(_getter)
        initChild(_setter)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        annotations.acceptChildrenLite(visitor)
        _delegate?.acceptLite(visitor)
        _getter?.acceptLite(visitor)
        _setter?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?): Int = when {
        this._delegate === old -> {
            this._delegate = new as BirVariable?
            2
        }
        this._getter === old -> {
            this._getter = new as BirSimpleFunction?
            3
        }
        this._setter === old -> {
            this._setter = new as BirSimpleFunction?
            4
        }
        else -> throwChildForReplacementNotFound(old)
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when(id) {
        1 -> this.annotations
        else -> throwChildrenListWithIdNotFound(id)
    }
}
