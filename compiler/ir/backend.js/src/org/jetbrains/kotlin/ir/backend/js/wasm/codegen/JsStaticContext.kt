/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.wasm.codegen

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrClassModel
import org.jetbrains.kotlin.ir.backend.js.utils.IrNamer
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.js.backend.ast.JsGlobalBlock

class WasmStaticContext(private val irNamer: IrNamer) : IrNamer by irNamer {
    val classModels = mutableMapOf<IrClassSymbol, JsIrClassModel>()
    val initializerBlock = JsGlobalBlock()
}