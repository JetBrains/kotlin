/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.irAttribute

var IrFunction.topLevelFunctionForNestedExternal: IrSimpleFunction? by irAttribute()

var IrClass.getInstanceFunctionForExternalObject: IrSimpleFunction? by irAttribute()

var IrClass.instanceCheckForExternalClass: IrSimpleFunction? by irAttribute()

var IrClass.getJsClassForExternalClass: IrSimpleFunction? by irAttribute()
