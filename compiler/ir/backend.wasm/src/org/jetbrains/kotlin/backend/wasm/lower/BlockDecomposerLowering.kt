/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.ir.backend.js.lower.AbstractBlockDecomposerLowering
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.expressions.IrExpression

class WasmBlockDecomposerLowering(val context: WasmBackendContext) : AbstractBlockDecomposerLowering(context) {
    override fun unreachableExpression(): IrExpression = TODO()
}