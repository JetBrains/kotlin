/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.util.BirImplementationDetail

abstract class BirInlinedFunctionBlock() : BirBlock() {
    abstract var inlineCall: BirFunctionAccessExpression

    abstract var inlinedElement: BirElement

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        statements.acceptChildren(visitor, data)
    }

    @BirImplementationDetail
    override fun getElementClassInternal(): BirElementClass<*> = BirInlinedFunctionBlock

    companion object : BirElementClass<BirInlinedFunctionBlock>(BirInlinedFunctionBlock::class.java, 59, true)
}
