/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "CanBePrimaryConstructorProperty")

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.expressions.BirConstantObject
import org.jetbrains.kotlin.bir.expressions.BirConstantValue
import org.jetbrains.kotlin.bir.symbols.BirConstructorSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.utils.SmartList

class BirConstantObjectImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    constructor: BirConstructorSymbol,
    typeArguments: List<BirType>,
) : BirConstantObject() {
    constructor(
        sourceSpan: SourceSpan,
        type: BirType,
        constructor: BirConstructorSymbol,
    ) : this(
        sourceSpan = sourceSpan,
        type = type,
        constructor = constructor,
        typeArguments = SmartList(),
    )

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

    private var _attributeOwnerId: BirAttributeContainer = this
    override var attributeOwnerId: BirAttributeContainer
        get() {
            recordPropertyRead()
            return _attributeOwnerId
        }
        set(value) {
            if (_attributeOwnerId !== value) {
                _attributeOwnerId = value
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

    private var _constructor: BirConstructorSymbol = constructor
    override var constructor: BirConstructorSymbol
        get() {
            recordPropertyRead()
            return _constructor
        }
        set(value) {
            if (_constructor !== value) {
                _constructor = value
                invalidate()
            }
        }

    private var _typeArguments: List<BirType> = typeArguments
    override var typeArguments: List<BirType>
        get() {
            recordPropertyRead()
            return _typeArguments
        }
        set(value) {
            if (_typeArguments != value) {
                _typeArguments = value
                invalidate()
            }
        }

    override val valueArguments: BirImplChildElementList<BirConstantValue> = BirImplChildElementList(this, 1, false)


    init {
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        valueArguments.acceptChildrenLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> {
        return when (id) {
            1 -> this.valueArguments
            else -> throwChildrenListWithIdNotFound(id)
        }
    }
}
