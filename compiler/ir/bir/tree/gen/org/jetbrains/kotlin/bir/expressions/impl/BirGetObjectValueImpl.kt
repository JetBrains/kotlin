/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.expressions.BirGetObjectValue
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.types.BirType

class BirGetObjectValueImpl(
    override var sourceSpan: SourceSpan,
    override var originalBeforeInline: BirAttributeContainer?,
    override var type: BirType,
    override var symbol: BirClassSymbol,
) : BirGetObjectValue() {
    override var attributeOwnerId: BirAttributeContainer = this
}
