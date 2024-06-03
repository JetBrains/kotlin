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

    override var sourceSpan: SourceSpan = sourceSpan

    override val signature: IdSignature? = signature

    override var annotations: List<BirConstructorCall> = annotations

    override var origin: IrDeclarationOrigin = origin

    override var name: Name = name

    override val symbol: BirLocalDelegatedPropertySymbol
        get() = this

    override var type: BirType = type

    override var isVar: Boolean = isVar

    private var _delegate: BirVariable? = delegate
    override var delegate: BirVariable
        get() {
            return _delegate ?: throwChildElementRemoved("delegate")
        }
        set(value) {
            if (_delegate !== value) {
                childReplaced(_delegate, value)
                _delegate = value
            }
        }

    private var _getter: BirSimpleFunction? = getter
    override var getter: BirSimpleFunction
        get() {
            return _getter ?: throwChildElementRemoved("getter")
        }
        set(value) {
            if (_getter !== value) {
                childReplaced(_getter, value)
                _getter = value
            }
        }

    private var _setter: BirSimpleFunction? = setter
    override var setter: BirSimpleFunction?
        get() {
            return _setter
        }
        set(value) {
            if (_setter !== value) {
                childReplaced(_setter, value)
                _setter = value
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
