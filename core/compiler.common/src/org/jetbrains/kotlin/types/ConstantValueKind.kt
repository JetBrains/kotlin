/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

sealed class ConstantValueKind(val asString: kotlin.String) {
    object Null : ConstantValueKind("Null")
    object Boolean : ConstantValueKind("Boolean")
    object Char : ConstantValueKind("Char")

    object Byte : ConstantValueKind("Byte") {
        override fun toUnsigned(): ConstantValueKind = UnsignedByte
    }

    object UnsignedByte : ConstantValueKind("UByte") {
        override fun toSigned(): ConstantValueKind = Byte
    }

    object Short : ConstantValueKind("Short") {
        override fun toUnsigned(): ConstantValueKind = UnsignedShort
    }

    object UnsignedShort : ConstantValueKind("UShort") {
        override fun toSigned(): ConstantValueKind = Short
    }

    object Int : ConstantValueKind("Int") {
        override fun toUnsigned(): ConstantValueKind = UnsignedInt
    }

    object UnsignedInt : ConstantValueKind("UInt") {
        override fun toSigned(): ConstantValueKind = Int
    }

    object Long : ConstantValueKind("Long") {
        override fun toUnsigned(): ConstantValueKind = UnsignedLong
    }

    object UnsignedLong : ConstantValueKind("ULong") {
        override fun toSigned(): ConstantValueKind = Long
    }

    object String : ConstantValueKind("String")

    object Float : ConstantValueKind("Float")
    object Double : ConstantValueKind("Double")

    object Error : ConstantValueKind("Error")

    object IntegerLiteral : ConstantValueKind("IntegerLiteral") {
        override fun toUnsigned(): ConstantValueKind = UnsignedIntegerLiteral
    }

    object UnsignedIntegerLiteral : ConstantValueKind("UnsignedIntegerLiteral") {
        override fun toSigned(): ConstantValueKind = IntegerLiteral
    }

    val isUnsigned: kotlin.Boolean get() = asString[0] == 'U'

    open fun toSigned(): ConstantValueKind = this
    open fun toUnsigned(): ConstantValueKind = this

    override fun toString() = asString
}
