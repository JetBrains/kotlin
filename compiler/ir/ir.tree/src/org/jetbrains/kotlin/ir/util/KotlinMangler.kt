/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

interface KotlinMangler {
    val String.hashMangle: Long
    val IrDeclaration.hashedMangle: Long
    fun IrDeclaration.isExported(): Boolean
    val IrFunction.functionName: String
    val IrType.isInlined: Boolean
    val Long.isSpecial: Boolean

    companion object {
        private val FUNCTION_PREFIX = "<BUILT-IN-FUNCTION>"
        fun functionClassSymbolName(name: Name) = "ktype:$FUNCTION_PREFIX$name"
        fun functionInvokeSymbolName(name: Name) = "kfun:$FUNCTION_PREFIX$name.invoke"
    }
}
