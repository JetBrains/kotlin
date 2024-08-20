/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

sealed class IrConstKind(val asString: kotlin.String) {
    object Null : IrConstKind("Null")
    object Boolean : IrConstKind("Boolean")
    object Char : IrConstKind("Char")
    object Byte : IrConstKind("Byte")
    object Short : IrConstKind("Short")
    object Int : IrConstKind("Int")
    object Long : IrConstKind("Long")
    object String : IrConstKind("String")
    object Float : IrConstKind("Float")
    object Double : IrConstKind("Double")

    override fun toString() = asString
}
