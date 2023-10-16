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
import org.jetbrains.kotlin.bir.expressions.BirSuspendableExpression
import org.jetbrains.kotlin.bir.types.BirType

class BirSuspendableExpressionImpl(
    override var sourceSpan: SourceSpan,
    override var originalBeforeInline: BirAttributeContainer?,
    override var type: BirType,
    suspensionPointId: BirExpression,
    result: BirExpression,
) : BirSuspendableExpression() {
    override var attributeOwnerId: BirAttributeContainer = this

    private var _suspensionPointId: BirExpression = suspensionPointId

    override var suspensionPointId: BirExpression
        get() = _suspensionPointId
        set(value) {
            if (_suspensionPointId != value) {
                replaceChild(_suspensionPointId, value)
                _suspensionPointId = value
            }
        }

    private var _result: BirExpression = result

    override var result: BirExpression
        get() = _result
        set(value) {
            if (_result != value) {
                replaceChild(_result, value)
                _result = value
            }
        }
    init {
        initChild(_suspensionPointId)
        initChild(_result)
    }
}
