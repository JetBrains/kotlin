/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.visitors

import org.jetbrains.kotlin.CompilerVersionOfApiDeprecation
import org.jetbrains.kotlin.DeprecatedCompilerApi
import org.jetbrains.kotlin.ir.IrElement

@DeprecatedCompilerApi(CompilerVersionOfApiDeprecation._2_1_20)
fun IrElement.acceptVoid(@Suppress("DEPRECATED_COMPILER_API") visitor: IrElementVisitorVoid) {
    accept(visitor, null)
}

@DeprecatedCompilerApi(CompilerVersionOfApiDeprecation._2_1_20)
fun IrElement.acceptChildrenVoid(@Suppress("DEPRECATED_COMPILER_API") visitor: IrElementVisitorVoid) {
    acceptChildren(visitor, null)
}

fun IrElement.acceptVoid(visitor: IrVisitorVoid) {
    accept(visitor, null)
}

fun IrElement.acceptChildrenVoid(visitor: IrVisitorVoid) {
    acceptChildren(visitor, null)
}
