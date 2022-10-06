/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol

class OptimizationsContext {
   val functionUsages = mutableMapOf<IrFunctionSymbol, MutableSet<IrCall>>()
   val collapsableVariablesValues = mutableMapOf<IrSymbol, IrExpression>()

   fun reset() {
      functionUsages.clear()
      collapsableVariablesValues.clear()
   }
}