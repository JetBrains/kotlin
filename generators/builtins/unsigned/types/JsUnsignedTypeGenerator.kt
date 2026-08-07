/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.unsigned

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import org.jetbrains.kotlin.generators.builtins.UnsignedType
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.ExpectActualModifier
import unsigned.types.BaseUnsignedTypeGenerator
import java.io.PrintWriter

class JsUnsignedTypeGenerator(type: UnsignedType, out: PrintWriter) : BaseUnsignedTypeGenerator(type, out, ExpectActualModifier.Actual) {
    override fun binaryOperatorsBody(operator: String, otherType: UnsignedType, returnType: UnsignedType): String =
        if (type == otherType && type == returnType && type == UnsignedType.UINT) {
            when (operator) {
                "div" -> "jsUintDivide(this, other)"
                "rem" -> "jsUintRemainder(this, other)"
                else -> super.binaryOperatorsBody(operator, otherType, returnType)
            }
        } else {
            super.binaryOperatorsBody(operator, otherType, returnType)
        }

    override fun compareToBody(otherType: UnsignedType): String {
        val theSameType = type == otherType
        return when (UnsignedType.UINT) {
            type if theSameType -> "jsUintCompare(data, other.data)"
            maxByDomainCapacity(type, UnsignedType.UINT) if theSameType ->
                "jsUintCompare(data.toInt(), other.data.toInt())"
            else -> super.compareToBody(otherType)
        }
    }

    override fun floatingConversionBody(otherType: PrimitiveType): String =
        if (type == UnsignedType.UINT && otherType == PrimitiveType.DOUBLE) {
            "jsUintToDouble(data)"
        } else super.floatingConversionBody(otherType)

    override fun fromFloatingPointBody(otherType: PrimitiveType): String =
        if (type == UnsignedType.UINT && otherType == PrimitiveType.DOUBLE) {
            "jsDoubleToUInt(this)"
        } else super.fromFloatingPointBody(otherType)

    override fun toStringHashCodeBody(): String = when (type) {
        UnsignedType.UINT -> "jsUintToString(data)"
        else -> super.toStringHashCodeBody()
    }

    override fun toStringWithBase(): String = when (type) {
        UnsignedType.UINT -> "jsUintToString(data, checkRadix(radix))"
        else -> super.toStringWithBase()
    }
}
