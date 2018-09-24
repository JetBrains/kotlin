/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.js.backend.ast.JsName

interface NameGenerator {
    fun getNameForSymbol(symbol: IrSymbol, context: JsGenerationContext): JsName
    fun getNameForType(type: IrType, context: JsGenerationContext): JsName
    fun getNameForLoop(loop: IrLoop, context: JsGenerationContext): JsName?
}
