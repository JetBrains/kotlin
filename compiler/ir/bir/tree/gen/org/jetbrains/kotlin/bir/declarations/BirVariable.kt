/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.symbols.BirVariableSymbol

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.variable]
 */
abstract class BirVariable(elementClass: BirElementClass<*>) : BirImplElementBase(elementClass), BirElement, BirDeclaration, BirValueDeclaration, BirVariableSymbol {
    abstract var isVar: Boolean
    abstract var isConst: Boolean
    abstract var isLateinit: Boolean
    abstract var initializer: BirExpression?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        annotations.acceptChildren(visitor, data)
        initializer?.accept(data, visitor)
    }

    companion object : BirElementClass<BirVariable>(BirVariable::class.java, 65, true)
}
