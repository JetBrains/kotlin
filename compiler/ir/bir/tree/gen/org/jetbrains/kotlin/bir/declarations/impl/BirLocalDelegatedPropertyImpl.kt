/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "CanBePrimaryConstructorProperty")

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.acceptLite
import org.jetbrains.kotlin.bir.declarations.BirLocalDelegatedProperty
import org.jetbrains.kotlin.bir.declarations.BirSimpleFunction
import org.jetbrains.kotlin.bir.declarations.BirVariable
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirLocalDelegatedPropertyImpl(
    sourceSpan: SourceSpan,
    signature: IdSignature?,
    annotations: List<BirConstructorCall>,
    origin: IrDeclarationOrigin,
    name: Name,
    type: BirType,
    isVar: Boolean,
    delegate: BirVariable,
    getter: BirSimpleFunction,
    setter: BirSimpleFunction?,
) : BirLocalDelegatedProperty(), BirLocalDelegatedPropertySymbol {
    constructor(
        sourceSpan: SourceSpan,
        origin: IrDeclarationOrigin,
        name: Name,
        type: BirType,
        isVar: Boolean,
        delegate: BirVariable,
        getter: BirSimpleFunction,
        setter: BirSimpleFunction?,
    ) : this(
        sourceSpan = sourceSpan,
        signature = null,
        annotations = emptyList(),
        origin = origin,
        name = name,
        type = type,
        isVar = isVar,
        delegate = delegate,
        getter = getter,
        setter = setter,
    )

    override val owner: BirLocalDelegatedPropertyImpl
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

    private var _origin: IrDeclarationOrigin = origin
    override var origin: IrDeclarationOrigin
        get() {
            recordPropertyRead()
            return _origin
        }
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate()
            }
        }

    private var _name: Name = name
    override var name: Name
        get() {
            recordPropertyRead()
            return _name
        }
        set(value) {
            if (_name != value) {
                _name = value
                invalidate()
            }
        }

    override val symbol: BirLocalDelegatedPropertySymbol
        get() = this

    private var _type: BirType = type
    override var type: BirType
        get() {
            recordPropertyRead()
            return _type
        }
        set(value) {
            if (_type != value) {
                _type = value
                invalidate()
            }
        }

    private var _isVar: Boolean = isVar
    override var isVar: Boolean
        get() {
            recordPropertyRead()
            return _isVar
        }
        set(value) {
            if (_isVar != value) {
                _isVar = value
                invalidate()
            }
        }

    private var _delegate: BirVariable? = delegate
    override var delegate: BirVariable
        get() {
            recordPropertyRead()
            return _delegate ?: throwChildElementRemoved("delegate")
        }
        set(value) {
            if (_delegate !== value) {
                childReplaced(_delegate, value)
                _delegate = value
                invalidate()
            }
        }

    private var _getter: BirSimpleFunction? = getter
    override var getter: BirSimpleFunction
        get() {
            recordPropertyRead()
            return _getter ?: throwChildElementRemoved("getter")
        }
        set(value) {
            if (_getter !== value) {
                childReplaced(_getter, value)
                _getter = value
                invalidate()
            }
        }

    private var _setter: BirSimpleFunction? = setter
    override var setter: BirSimpleFunction?
        get() {
            recordPropertyRead()
            return _setter
        }
        set(value) {
            if (_setter !== value) {
                childReplaced(_setter, value)
                _setter = value
                invalidate()
            }
        }


    init {
        initChild(_delegate)
        initChild(_getter)
        initChild(_setter)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _delegate?.acceptLite(visitor)
        _getter?.acceptLite(visitor)
        _setter?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._delegate === old -> {
                this._delegate = new as BirVariable?
            }
            this._getter === old -> {
                this._getter = new as BirSimpleFunction?
            }
            this._setter === old -> {
                this._setter = new as BirSimpleFunction?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
