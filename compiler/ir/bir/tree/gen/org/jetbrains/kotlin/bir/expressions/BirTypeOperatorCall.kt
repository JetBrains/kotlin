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
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.BirImplementationDetail
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator

abstract class BirTypeOperatorCall() : BirExpression() {
    abstract var operator: IrTypeOperator

    abstract var argument: BirExpression

    abstract var typeOperand: BirType

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        argument.accept(data, visitor)
    }

    @BirImplementationDetail
    override fun getElementClassInternal(): BirElementClass<*> = BirTypeOperatorCall

    companion object : BirElementClass<BirTypeOperatorCall>(BirTypeOperatorCall::class.java, 95, true)
}
