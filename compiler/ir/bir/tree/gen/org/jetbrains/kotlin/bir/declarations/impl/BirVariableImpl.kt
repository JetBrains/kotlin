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
import org.jetbrains.kotlin.bir.declarations.BirVariable
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.symbols.BirVariableSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirVariableImpl(
    sourceSpan: SourceSpan,
    signature: IdSignature?,
    annotations: List<BirConstructorCall>,
    origin: IrDeclarationOrigin,
    name: Name,
    type: BirType,
    isVar: Boolean,
    isConst: Boolean,
    isLateinit: Boolean,
    initializer: BirExpression?,
) : BirVariable(), BirVariableSymbol {
    constructor(
        sourceSpan: SourceSpan,
        origin: IrDeclarationOrigin,
        name: Name,
        type: BirType,
        isVar: Boolean,
        isConst: Boolean,
        isLateinit: Boolean,
    ) : this(
        sourceSpan = sourceSpan,
        signature = null,
        annotations = emptyList(),
        origin = origin,
        name = name,
        type = type,
        isVar = isVar,
        isConst = isConst,
        isLateinit = isLateinit,
        initializer = null,
    )

    override val owner: BirVariableImpl
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

    override val symbol: BirVariableSymbol
        get() = this

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

    private var _isConst: Boolean = isConst
    override var isConst: Boolean
        get() {
            recordPropertyRead()
            return _isConst
        }
        set(value) {
            if (_isConst != value) {
                _isConst = value
                invalidate()
            }
        }

    private var _isLateinit: Boolean = isLateinit
    override var isLateinit: Boolean
        get() {
            recordPropertyRead()
            return _isLateinit
        }
        set(value) {
            if (_isLateinit != value) {
                _isLateinit = value
                invalidate()
            }
        }

    private var _initializer: BirExpression? = initializer
    override var initializer: BirExpression?
        get() {
            recordPropertyRead()
            return _initializer
        }
        set(value) {
            if (_initializer !== value) {
                childReplaced(_initializer, value)
                _initializer = value
                invalidate()
            }
        }


    init {
        initChild(_initializer)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _initializer?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._initializer === old -> {
                this._initializer = new as BirExpression?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
