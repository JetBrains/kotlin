/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.accept
import org.jetbrains.kotlin.bir.symbols.BirReturnTargetSymbol
import org.jetbrains.kotlin.bir.util.BirImplementationDetail

abstract class BirReturn() : BirExpression() {
    abstract var value: BirExpression

    abstract var returnTargetSymbol: BirReturnTargetSymbol

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        value.accept(data, visitor)
    }

    @BirImplementationDetail
    override fun getElementClassInternal(): BirElementClass<*> = BirReturn

    companion object : BirElementClass<BirReturn>(BirReturn::class.java, 77, true)
}
