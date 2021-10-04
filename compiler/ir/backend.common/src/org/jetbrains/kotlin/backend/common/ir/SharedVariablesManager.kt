/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol

interface SharedVariablesManager {
    fun declareSharedVariable(originalDeclaration: IrVariable): IrVariable

    fun defineSharedValue(originalDeclaration: IrVariable, sharedVariableDeclaration: IrVariable): IrStatement

    fun getSharedValue(sharedVariableSymbol: IrValueSymbol, originalGet: IrGetValue): IrExpression

    fun setSharedValue(sharedVariableSymbol: IrValueSymbol, originalSet: IrSetValue): IrExpression
}