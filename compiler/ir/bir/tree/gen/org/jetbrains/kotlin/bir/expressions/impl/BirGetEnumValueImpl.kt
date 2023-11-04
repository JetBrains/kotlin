/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.expressions.BirGetEnumValue
import org.jetbrains.kotlin.bir.symbols.BirEnumEntrySymbol
import org.jetbrains.kotlin.bir.types.BirType

class BirGetEnumValueImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    symbol: BirEnumEntrySymbol,
) : BirGetEnumValue() {
    private var _sourceSpan: SourceSpan = sourceSpan

    override var sourceSpan: SourceSpan
        get() {
            recordPropertyRead(4)
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate(4)
            }
        }

    private var _attributeOwnerId: BirAttributeContainer = this

    override var attributeOwnerId: BirAttributeContainer
        get() {
            recordPropertyRead(1)
            return _attributeOwnerId
        }
        set(value) {
            if (_attributeOwnerId != value) {
                _attributeOwnerId = value
                invalidate(1)
            }
        }

    private var _type: BirType = type

    override var type: BirType
        get() {
            recordPropertyRead(2)
            return _type
        }
        set(value) {
            if (_type != value) {
                _type = value
                invalidate(2)
            }
        }

    private var _symbol: BirEnumEntrySymbol = symbol

    override var symbol: BirEnumEntrySymbol
        get() {
            recordPropertyRead(3)
            return _symbol
        }
        set(value) {
            if (_symbol != value) {
                _symbol = value
                invalidate(3)
            }
        }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
