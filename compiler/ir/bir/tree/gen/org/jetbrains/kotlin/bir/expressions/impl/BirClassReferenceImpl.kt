/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "CanBePrimaryConstructorProperty")

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.expressions.BirClassReference
import org.jetbrains.kotlin.bir.symbols.BirClassifierSymbol
import org.jetbrains.kotlin.bir.types.BirType

class BirClassReferenceImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    symbol: BirClassifierSymbol,
    classType: BirType,
) : BirClassReference() {
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

    private var _symbol: BirClassifierSymbol = symbol
    override var symbol: BirClassifierSymbol
        get() {
            recordPropertyRead()
            return _symbol
        }
        set(value) {
            if (_symbol !== value) {
                _symbol = value
                invalidate()
            }
        }

    private var _classType: BirType = classType
    override var classType: BirType
        get() {
            recordPropertyRead()
            return _classType
        }
        set(value) {
            if (_classType != value) {
                _classType = value
                invalidate()
            }
        }

}
