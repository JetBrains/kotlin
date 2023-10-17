/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.SourceSpan
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
    override val descriptor: VariableDescriptorWithAccessors,
    signature: IdSignature?,
    override var annotations: List<BirConstructorCall>,
    origin: IrDeclarationOrigin,
    name: Name,
    type: BirType,
    isVar: Boolean,
    delegate: BirVariable,
    getter: BirSimpleFunction,
    setter: BirSimpleFunction?,
) : BirLocalDelegatedProperty() {
    private var _sourceSpan: SourceSpan = sourceSpan

    override var sourceSpan: SourceSpan
        get() = _sourceSpan
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate()
            }
        }

    private var _signature: IdSignature? = signature

    override var signature: IdSignature?
        get() = _signature
        set(value) {
            if (_signature != value) {
                _signature = value
                invalidate()
            }
        }

    private var _origin: IrDeclarationOrigin = origin

    override var origin: IrDeclarationOrigin
        get() = _origin
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate()
            }
        }

    private var _name: Name = name

    override var name: Name
        get() = _name
        set(value) {
            if (_name != value) {
                _name = value
                invalidate()
            }
        }

    private var _type: BirType = type

    override var type: BirType
        get() = _type
        set(value) {
            if (_type != value) {
                _type = value
                invalidate()
            }
        }

    private var _isVar: Boolean = isVar

    override var isVar: Boolean
        get() = _isVar
        set(value) {
            if (_isVar != value) {
                _isVar = value
                invalidate()
            }
        }

    private var _delegate: BirVariable = delegate

    override var delegate: BirVariable
        get() = _delegate
        set(value) {
            if (_delegate != value) {
                replaceChild(_delegate, value)
                _delegate = value
                invalidate()
            }
        }

    private var _getter: BirSimpleFunction = getter

    override var getter: BirSimpleFunction
        get() = _getter
        set(value) {
            if (_getter != value) {
                replaceChild(_getter, value)
                _getter = value
                invalidate()
            }
        }

    private var _setter: BirSimpleFunction? = setter

    override var setter: BirSimpleFunction?
        get() = _setter
        set(value) {
            if (_setter != value) {
                replaceChild(_setter, value)
                _setter = value
                invalidate()
            }
        }
    init {
        initChild(_delegate)
        initChild(_getter)
        initChild(_setter)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._delegate === old -> this.delegate = new as BirVariable
            this._getter === old -> this.getter = new as BirSimpleFunction
            this._setter === old -> this.setter = new as BirSimpleFunction
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
