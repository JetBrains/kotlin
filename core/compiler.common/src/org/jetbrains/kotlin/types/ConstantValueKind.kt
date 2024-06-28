/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

sealed class ConstantValueKind(val asString: kotlin.String) {
    object Null : ConstantValueKind("Null")
    object Boolean : ConstantValueKind("Boolean")
    object Char : ConstantValueKind("Char")

    object Byte : ConstantValueKind("Byte")
    object UnsignedByte : ConstantValueKind("UByte")
    object Short : ConstantValueKind("Short")
    object UnsignedShort : ConstantValueKind("UShort")
    object Int : ConstantValueKind("Int")
    object UnsignedInt : ConstantValueKind("UInt")
    object Long : ConstantValueKind("Long")
    object UnsignedLong : ConstantValueKind("ULong")

    object String : ConstantValueKind("String")

    object Float : ConstantValueKind("Float")
    object Double : ConstantValueKind("Double")

    object Error : ConstantValueKind("Error")

    object IntegerLiteral : ConstantValueKind("IntegerLiteral")
    object UnsignedIntegerLiteral : ConstantValueKind("UnsignedIntegerLiteral")

    override fun toString() = asString
}
