/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.BirElementBase
import org.jetbrains.kotlin.bir.BirStatement
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.visitors.BirElementTransformer

/**
 * A non-leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.expression]
 */
abstract class BirExpression : BirElementBase(), BirStatement, BirVarargElement,
        BirAttributeContainer {
    override var attributeOwnerId: BirAttributeContainer = this

    override var originalBeforeInline: BirAttributeContainer? = null

    abstract var type: BirType

    override fun <D> transform(transformer: BirElementTransformer<D>, data: D):
            BirExpression = accept(transformer, data) as BirExpression
}
