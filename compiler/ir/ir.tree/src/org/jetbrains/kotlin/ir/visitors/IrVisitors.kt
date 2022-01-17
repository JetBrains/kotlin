/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.visitors

import org.jetbrains.kotlin.ir.IrElement

fun IrElement.acceptVoid(visitor: IrElementVisitorVoid) {
    accept(visitor, null)
}

fun IrElement.acceptChildrenVoid(visitor: IrElementVisitorVoid) {
    acceptChildren(visitor, null)
}

fun IrElement.acceptVoid(visitor: IrAbstractVisitorVoid) {
    accept(visitor, null)
}

fun IrElement.acceptChildrenVoid(visitor: IrAbstractVisitorVoid) {
    acceptChildren(visitor, null)
}
