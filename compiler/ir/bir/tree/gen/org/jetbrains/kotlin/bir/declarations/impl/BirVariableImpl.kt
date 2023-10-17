/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirVariable
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirVariableImpl @ObsoleteDescriptorBasedAPI constructor(
    sourceSpan: SourceSpan,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: VariableDescriptor,
    signature: IdSignature,
    override var annotations: List<BirConstructorCall>,
    origin: IrDeclarationOrigin,
    name: Name,
    type: BirType,
    override val isAssignable: Boolean,
    isVar: Boolean,
    isConst: Boolean,
    isLateinit: Boolean,
    initializer: BirExpression?,
) : BirVariable() {
    private var _sourceSpan: SourceSpan = sourceSpan

    override var sourceSpan: SourceSpan
        get() = _sourceSpan
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate()
            }
        }

    private var _signature: IdSignature = signature

    override var signature: IdSignature
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

    private var _isConst: Boolean = isConst

    override var isConst: Boolean
        get() = _isConst
        set(value) {
            if (_isConst != value) {
                _isConst = value
                invalidate()
            }
        }

    private var _isLateinit: Boolean = isLateinit

    override var isLateinit: Boolean
        get() = _isLateinit
        set(value) {
            if (_isLateinit != value) {
                _isLateinit = value
                invalidate()
            }
        }

    private var _initializer: BirExpression? = initializer

    override var initializer: BirExpression?
        get() = _initializer
        set(value) {
            if (_initializer != value) {
                replaceChild(_initializer, value)
                _initializer = value
                invalidate()
            }
        }
    init {
        initChild(_initializer)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._initializer === old -> this.initializer = new as BirExpression
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
