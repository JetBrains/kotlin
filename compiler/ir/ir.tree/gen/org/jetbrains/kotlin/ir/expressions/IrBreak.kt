/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * A leaf IR tree element.
 * @sample org.jetbrains.kotlin.ir.generator.IrTree.break
 */
abstract class IrBreak : IrBreakContinue() {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitBreak(this, data)
}
