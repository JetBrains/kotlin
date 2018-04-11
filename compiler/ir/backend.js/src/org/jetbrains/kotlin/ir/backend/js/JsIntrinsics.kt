/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns

class JsIntrinsics(irBuiltIns: IrBuiltIns) {
    // Equality
    val jsEqeq = irBuiltIns.binOpBool("jsEqeq")
    val jsNotEq = irBuiltIns.binOpBool("jsNotEq")
    val jsEqeqeq = irBuiltIns.binOpBool("jsEqeqeq")
    val jsNotEqeq = irBuiltIns.binOpBool("jsNotEqeq")

    // Unary operations
    val jsNot = irBuiltIns.unOpBool("jsNot")

    // Binary operations
    val jsPlus = irBuiltIns.binOp("jsPlus")
    val jsMinus = irBuiltIns.binOp("jsMinus")
    val jsMult = irBuiltIns.binOp("jsMult")
    val jsDiv = irBuiltIns.binOp("jsDiv")
    val jsMod = irBuiltIns.binOp("jsMod")
}

private fun IrBuiltIns.unOpBool(name: String) = defineOperator(name, bool, listOf(anyN, anyN))
private fun IrBuiltIns.binOpBool(name: String) = defineOperator(name, bool, listOf(anyN, anyN))
private fun IrBuiltIns.unOp(name: String) = defineOperator(name, anyN, listOf(anyN, anyN))
private fun IrBuiltIns.binOp(name: String) = defineOperator(name, anyN, listOf(anyN, anyN))
