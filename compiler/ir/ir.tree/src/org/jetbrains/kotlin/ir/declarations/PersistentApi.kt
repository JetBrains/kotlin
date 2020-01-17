/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrBody

// TODO threadlocal
// TODO make a IrDeclarationBase field? (requires IR factory)
var stageController: StageController = object : StageController {}

interface StageController {
    val currentStage: Int get() = 0

    fun lazyLower(declaration: IrDeclaration) {}

    fun lazyLower(body: IrBody) {}

    fun <T> withStage(stage: Int, fn: () -> T): T = fn()

    val bodiesEnabled: Boolean get() = true

    fun <T> withInitialIr(block: () -> T): T = block()

    fun <T> withInitialStateOf(declaration: IrDeclaration, block: () -> T): T = block()

    fun <T> restrictTo(declaration: IrDeclaration, fn: () -> T): T = fn()

    fun <T> bodyLowering(fn: () -> T): T = fn()

    fun canModify(element: IrElement): Boolean = true

    fun <T> unrestrictDeclarationListsAccess(fn: () -> T): T = fn()

    fun canAccessDeclarationsOf(irClass: IrClass): Boolean = true
}

inline fun <T> withInitialIr(noinline fn: () -> T): T {
    return stageController.withInitialIr(fn)
}