/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION", "DeprecatedCallableAddReplaceWith")

package org.jetbrains.kotlin.analysis.api.impl.base

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.psi.KtElement

@KaImplementationDetail
object KaConstantValueFactory {
    fun createConstantValue(value: Any?, expression: KtElement? = null): KaConstantValue? = when (value) {
        null -> KaNullConstantValueImpl(expression)
        is Boolean -> KaBooleanConstantValueImpl(value, expression)
        is Char -> KaCharConstantValueImpl(value, expression)
        is Byte -> KaByteConstantValueImpl(value, expression)
        is UByte -> KaUnsignedByteConstantValueImpl(value, expression)
        is Short -> KaShortConstantValueImpl(value, expression)
        is UShort -> KaUnsignedShortConstantValueImpl(value, expression)
        is Int -> KaIntConstantValueImpl(value, expression)
        is UInt -> KaUnsignedIntConstantValueImpl(value, expression)
        is Long -> KaLongConstantValueImpl(value, expression)
        is ULong -> KaUnsignedLongConstantValueImpl(value, expression)
        is String -> KaStringConstantValueImpl(value, expression)
        is Float -> KaFloatConstantValueImpl(value, expression)
        is Double -> KaDoubleConstantValueImpl(value, expression)
        else -> null
    }
}

@KaImplementationDetail
class KaNullConstantValueImpl(override val sourcePsi: KtElement?) : KaConstantValue.NullValue {
    override val value: Nothing?
        get() = null

    override fun render(): String = "null"
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaBooleanConstantValueImpl(
    override val value: Boolean,
    override val sourcePsi: KtElement?
) : KaConstantValue.BooleanValue {
    override fun render(): String = value.toString()
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaCharConstantValueImpl(
    override val value: Char,
    override val sourcePsi: KtElement?
) : KaConstantValue.CharValue {
    override fun render(): String = "`$value`"
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaByteConstantValueImpl(
    override val value: Byte,
    override val sourcePsi: KtElement?
) : KaConstantValue.ByteValue {
    override fun render(): String = value.toString()
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaUnsignedByteConstantValueImpl(
    override val value: UByte,
    override val sourcePsi: KtElement?
) : KaConstantValue.UByteValue {
    override fun render(): String = "${value}u"
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaShortConstantValueImpl(
    override val value: Short,
    override val sourcePsi: KtElement?
) : KaConstantValue.ShortValue {
    override fun render(): String = value.toString()
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaUnsignedShortConstantValueImpl(
    override val value: UShort,
    override val sourcePsi: KtElement?
) : KaConstantValue.UShortValue {
    override fun render(): String = "${value}u"
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaIntConstantValueImpl(
    override val value: Int,
    override val sourcePsi: KtElement?
) : KaConstantValue.IntValue {
    override fun render(): String = value.toString()
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaUnsignedIntConstantValueImpl(
    override val value: UInt,
    override val sourcePsi: KtElement?
) : KaConstantValue.UIntValue {
    override fun render(): String = "${value}u"
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaLongConstantValueImpl(
    override val value: Long,
    override val sourcePsi: KtElement?
) : KaConstantValue.LongValue {
    override fun render(): String = value.toString()
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaUnsignedLongConstantValueImpl(
    override val value: ULong,
    override val sourcePsi: KtElement?
) : KaConstantValue.ULongValue {
    override fun render(): String = "${value}uL"
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaStringConstantValueImpl(
    override val value: String,
    override val sourcePsi: KtElement?
) : KaConstantValue.StringValue {
    override fun render(): String = "\"${value}\""
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaFloatConstantValueImpl(
    override val value: Float,
    override val sourcePsi: KtElement?
) : KaConstantValue.FloatValue {
    override fun render(): String = "${value}f"
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaDoubleConstantValueImpl(
    override val value: Double,
    override val sourcePsi: KtElement?
) : KaConstantValue.DoubleValue {
    override fun render(): String = value.toString()
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaErrorConstantValueImpl(
    override val errorMessage: String,
    override val sourcePsi: KtElement?,
) : KaConstantValue.ErrorValue {
    override val value: Nothing
        get() = error("Cannot get value for KaErrorConstantValue")

    override fun render(): String = "error(\"$errorMessage\")"
    override fun toString(): String = render()
}
