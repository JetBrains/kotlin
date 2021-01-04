/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

sealed class FirConstKind<T>(val asString: kotlin.String, val isUnsigned: kotlin.Boolean = false) {
    object Null : FirConstKind<Nothing?>("Null")
    object Boolean : FirConstKind<kotlin.Boolean>("Boolean")
    object Char : FirConstKind<kotlin.Char>("Char")

    object Byte : FirConstKind<kotlin.Byte>("Byte")
    object UnsignedByte : FirConstKind<kotlin.Byte>("UByte", true)
    object Short : FirConstKind<kotlin.Short>("Short")
    object UnsignedShort : FirConstKind<kotlin.Short>("UShort", true)
    object Int : FirConstKind<kotlin.Int>("Int")
    object UnsignedInt : FirConstKind<kotlin.Int>("UInt", true)
    object Long : FirConstKind<kotlin.Long>("Long")
    object UnsignedLong : FirConstKind<kotlin.Long>("ULong", true)

    object String : FirConstKind<kotlin.String>("String")

    object Float : FirConstKind<kotlin.Float>("Float")
    object Double : FirConstKind<kotlin.Double>("Double")

    object IntegerLiteral : FirConstKind<kotlin.Long>("IntegerLiteral")
    object UnsignedIntegerLiteral : FirConstKind<kotlin.Long>("UnsignedIntegerLiteral", true)

    override fun toString() = asString
}
