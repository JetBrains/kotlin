/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrBody

// TODO threadlocal
// TODO make a IrDeclarationBase field? (requires IR factory)
var stageController: StageController = StageController()

// TODO make a class
open class StageController(open val currentStage: Int = 0) {
    open fun lazyLower(declaration: IrDeclaration) {}

    open fun lazyLower(body: IrBody) {}

    open fun <T> withStage(stage: Int, fn: () -> T): T = fn()

    open val bodiesEnabled: Boolean get() = true

    open fun <T> withInitialIr(block: () -> T): T = block()

    open fun <T> restrictTo(declaration: IrDeclaration, fn: () -> T): T = fn()

    open fun <T> bodyLowering(fn: () -> T): T = fn()

    open fun canModify(element: IrElement): Boolean = true

    open fun <T> unrestrictDeclarationListsAccess(fn: () -> T): T = fn()

    open fun canAccessDeclarationsOf(irClass: IrClass): Boolean = true
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T> withInitialIr(noinline fn: () -> T): T {
    return stageController.withInitialIr(fn)
}
