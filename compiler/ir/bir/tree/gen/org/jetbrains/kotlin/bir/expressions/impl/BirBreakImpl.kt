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
import org.jetbrains.kotlin.bir.expressions.BirBreak
import org.jetbrains.kotlin.bir.expressions.BirLoop
import org.jetbrains.kotlin.bir.types.BirType

class BirBreakImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    loop: BirLoop,
    label: String?,
) : BirBreak() {
    constructor(
        sourceSpan: SourceSpan,
        type: BirType,
        loop: BirLoop,
    ) : this(
        sourceSpan = sourceSpan,
        type = type,
        loop = loop,
        label = null,
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

    private var _loop: BirLoop = loop
    override var loop: BirLoop
        get() {
            recordPropertyRead()
            return _loop
        }
        set(value) {
            if (_loop !== value) {
                _loop = value
                invalidate()
            }
        }

    private var _label: String? = label
    override var label: String?
        get() {
            recordPropertyRead()
            return _label
        }
        set(value) {
            if (_label != value) {
                _label = value
                invalidate()
            }
        }

}
