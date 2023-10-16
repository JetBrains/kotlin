/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirGetClass
import org.jetbrains.kotlin.bir.types.BirType

class BirGetClassImpl(
    override var sourceSpan: SourceSpan,
    override var originalBeforeInline: BirAttributeContainer?,
    override var type: BirType,
    argument: BirExpression,
) : BirGetClass() {
    override var attributeOwnerId: BirAttributeContainer = this

    private var _argument: BirExpression = argument

    override var argument: BirExpression
        get() = _argument
        set(value) {
            if (_argument != value) {
                replaceChild(_argument, value)
                _argument = value
            }
        }
    init {
        initChild(_argument)
    }
}
