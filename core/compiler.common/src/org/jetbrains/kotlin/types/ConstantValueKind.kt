/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

sealed class ConstantValueKind<T>(val asString: kotlin.String) {
    object Null : ConstantValueKind<Nothing?>("Null")
    object Boolean : ConstantValueKind<kotlin.Boolean>("Boolean")
    object Char : ConstantValueKind<kotlin.Char>("Char")

    object Byte : ConstantValueKind<kotlin.Byte>("Byte")
    object UnsignedByte : ConstantValueKind<kotlin.Byte>("UByte")
    object Short : ConstantValueKind<kotlin.Short>("Short")
    object UnsignedShort : ConstantValueKind<kotlin.Short>("UShort")
    object Int : ConstantValueKind<kotlin.Int>("Int")
    object UnsignedInt : ConstantValueKind<kotlin.Int>("UInt")
    object Long : ConstantValueKind<kotlin.Long>("Long")
    object UnsignedLong : ConstantValueKind<kotlin.Long>("ULong")

    object String : ConstantValueKind<kotlin.String>("String")

    object Float : ConstantValueKind<kotlin.Float>("Float")
    object Double : ConstantValueKind<kotlin.Double>("Double")

    object Error : ConstantValueKind<Nothing>("Error")

    object IntegerLiteral : ConstantValueKind<kotlin.Long>("IntegerLiteral")
    object UnsignedIntegerLiteral : ConstantValueKind<kotlin.Long>("UnsignedIntegerLiteral")

    override fun toString() = asString
}
