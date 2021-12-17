/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.descriptors

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol

@Deprecated("rhizomedb & noria compatibility", level = DeprecationLevel.ERROR)
class IrBuiltIns(val irBuiltIns: IrBuiltIns) {
    val anyNType = irBuiltIns.anyNType
    val stringType = irBuiltIns.stringType
    val kClassClass = irBuiltIns.kClassClass

    fun getKPropertyClass(mutable: Boolean, n: Int): IrClassSymbol = irBuiltIns.getKPropertyClass(mutable, n)

    val unitType = irBuiltIns.unitType
    val anyClass = irBuiltIns.anyClass
    val longType = irBuiltIns.longType
    val booleanType = irBuiltIns.booleanType
    val intType = irBuiltIns.intType
    val anyType = irBuiltIns.anyType
    val arrayClass = irBuiltIns.arrayClass
}