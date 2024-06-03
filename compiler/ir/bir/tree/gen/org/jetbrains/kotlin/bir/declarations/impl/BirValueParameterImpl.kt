/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "CanBePrimaryConstructorProperty")

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirValueParameter
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody
import org.jetbrains.kotlin.bir.symbols.BirValueParameterSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirValueParameterImpl(
    sourceSpan: SourceSpan,
    signature: IdSignature?,
    annotations: List<BirConstructorCall>,
    origin: IrDeclarationOrigin,
    name: Name,
    type: BirType,
    isAssignable: Boolean,
    index: Int,
    varargElementType: BirType?,
    isCrossinline: Boolean,
    isNoinline: Boolean,
    isHidden: Boolean,
    defaultValue: BirExpressionBody?,
) : BirImplElementBase(), BirValueParameter, BirValueParameterSymbol {
    constructor(
        sourceSpan: SourceSpan,
        origin: IrDeclarationOrigin,
        name: Name,
        type: BirType,
        isAssignable: Boolean,
        index: Int,
        varargElementType: BirType?,
        isCrossinline: Boolean,
        isNoinline: Boolean,
        isHidden: Boolean,
    ) : this(
        sourceSpan = sourceSpan,
        signature = null,
        annotations = emptyList(),
        origin = origin,
        name = name,
        type = type,
        isAssignable = isAssignable,
        index = index,
        varargElementType = varargElementType,
        isCrossinline = isCrossinline,
        isNoinline = isNoinline,
        isHidden = isHidden,
        defaultValue = null,
    )

    override val owner: BirValueParameterImpl
        get() = this

    override val isBound: Boolean
        get() = true

    override var sourceSpan: SourceSpan = sourceSpan

    override val signature: IdSignature? = signature

    override var annotations: List<BirConstructorCall> = annotations

    override var origin: IrDeclarationOrigin = origin

    override var name: Name = name

    override var type: BirType = type

    override val isAssignable: Boolean = isAssignable

    override val symbol: BirValueParameterSymbol
        get() = this

    override var index: Int = index

    override var varargElementType: BirType? = varargElementType

    override var isCrossinline: Boolean = isCrossinline

    override var isNoinline: Boolean = isNoinline

    override var isHidden: Boolean = isHidden

    private var _defaultValue: BirExpressionBody? = defaultValue
    override var defaultValue: BirExpressionBody?
        get() {
            return _defaultValue
        }
        set(value) {
            if (_defaultValue !== value) {
                childReplaced(_defaultValue, value)
                _defaultValue = value
            }
        }


    init {
        initChild(_defaultValue)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _defaultValue?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._defaultValue === old -> {
                this._defaultValue = new as BirExpressionBody?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
