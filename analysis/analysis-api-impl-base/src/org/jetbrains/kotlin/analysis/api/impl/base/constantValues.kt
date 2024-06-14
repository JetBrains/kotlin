/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION", "DeprecatedCallableAddReplaceWith")

package org.jetbrains.kotlin.analysis.api.impl.base

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.types.ConstantValueKind

object KaConstantValueFactory {
    @OptIn(KaImplementationDetail::class)
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
class KaNullConstantValueImpl(override val sourcePsi: KtElement?) : KaConstantValue.NullValue, KaConstantValue.KaNullConstantValue {
    @Deprecated("Check the class type instead.")
    override val constantValueKind: ConstantValueKind
        get() = ConstantValueKind.Null

    override val value: Nothing?
        get() = null

    override fun render(): String = "null"
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaBooleanConstantValueImpl(
    override val value: Boolean,
    override val sourcePsi: KtElement?
) : KaConstantValue.BooleanValue, KaConstantValue.KaBooleanConstantValue {
    @Deprecated("Check the class type instead.")
    override val constantValueKind: ConstantValueKind
        get() = ConstantValueKind.Boolean

    override fun render(): String = value.toString()
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaCharConstantValueImpl(
    override val value: Char,
    override val sourcePsi: KtElement?
) : KaConstantValue.CharValue, KaConstantValue.KaCharConstantValue {
    @Deprecated("Check the class type instead.")
    override val constantValueKind: ConstantValueKind
        get() = ConstantValueKind.Char

    override fun render(): String = "`$value`"
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaByteConstantValueImpl(
    override val value: Byte,
    override val sourcePsi: KtElement?
) : KaConstantValue.ByteValue, KaConstantValue.KaByteConstantValue {
    @Deprecated("Check the class type instead.")
    override val constantValueKind: ConstantValueKind
        get() = ConstantValueKind.Byte

    override fun render(): String = value.toString()
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaUnsignedByteConstantValueImpl(
    override val value: UByte,
    override val sourcePsi: KtElement?
) : KaConstantValue.UByteValue, KaConstantValue.KaUnsignedByteConstantValue {
    @Deprecated("Check the class type instead.")
    override val constantValueKind: ConstantValueKind
        get() = ConstantValueKind.UnsignedByte

    override fun render(): String = "${value}u"
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaShortConstantValueImpl(
    override val value: Short,
    override val sourcePsi: KtElement?
) : KaConstantValue.ShortValue, KaConstantValue.KaShortConstantValue {
    @Deprecated("Check the class type instead.")
    override val constantValueKind: ConstantValueKind
        get() = ConstantValueKind.Short

    override fun render(): String = value.toString()
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaUnsignedShortConstantValueImpl(
    override val value: UShort,
    override val sourcePsi: KtElement?
) : KaConstantValue.UShortValue, KaConstantValue.KaUnsignedShortConstantValue {
    @Deprecated("Check the class type instead.")
    override val constantValueKind: ConstantValueKind
        get() = ConstantValueKind.UnsignedShort

    override fun render(): String = "${value}u"
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaIntConstantValueImpl(
    override val value: Int,
    override val sourcePsi: KtElement?
) : KaConstantValue.IntValue, KaConstantValue.KaIntConstantValue {
    @Deprecated("Check the class type instead.")
    override val constantValueKind: ConstantValueKind
        get() = ConstantValueKind.Int

    override fun render(): String = value.toString()
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaUnsignedIntConstantValueImpl(
    override val value: UInt,
    override val sourcePsi: KtElement?
) : KaConstantValue.UIntValue, KaConstantValue.KaUnsignedIntConstantValue {
    @Deprecated("Check the class type instead.")
    override val constantValueKind: ConstantValueKind
        get() = ConstantValueKind.UnsignedInt

    override fun render(): String = "${value}u"
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaLongConstantValueImpl(
    override val value: Long,
    override val sourcePsi: KtElement?
) : KaConstantValue.LongValue, KaConstantValue.KaLongConstantValue {
    @Deprecated("Check the class type instead.")
    override val constantValueKind: ConstantValueKind
        get() = ConstantValueKind.Long

    override fun render(): String = value.toString()
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaUnsignedLongConstantValueImpl(
    override val value: ULong,
    override val sourcePsi: KtElement?
) : KaConstantValue.ULongValue, KaConstantValue.KaUnsignedLongConstantValue {
    @Deprecated("Check the class type instead.")
    override val constantValueKind: ConstantValueKind
        get() = ConstantValueKind.UnsignedLong

    override fun render(): String = "${value}uL"
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaStringConstantValueImpl(
    override val value: String,
    override val sourcePsi: KtElement?
) : KaConstantValue.StringValue, KaConstantValue.KaStringConstantValue {
    @Deprecated("Check the class type instead.")
    override val constantValueKind: ConstantValueKind
        get() = ConstantValueKind.String

    override fun render(): String = "\"${value}\""
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaFloatConstantValueImpl(
    override val value: Float,
    override val sourcePsi: KtElement?
) : KaConstantValue.FloatValue, KaConstantValue.KaFloatConstantValue {
    @Deprecated("Check the class type instead.")
    override val constantValueKind: ConstantValueKind
        get() = ConstantValueKind.Float

    override fun render(): String = "${value}f"
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaDoubleConstantValueImpl(
    override val value: Double,
    override val sourcePsi: KtElement?
) : KaConstantValue.DoubleValue, KaConstantValue.KaDoubleConstantValue {
    @Deprecated("Check the class type instead.")
    override val constantValueKind: ConstantValueKind
        get() = ConstantValueKind.Double

    override fun render(): String = value.toString()
    override fun toString(): String = render()
}

@KaImplementationDetail
class KaErrorConstantValueImpl(
    override val errorMessage: String,
    override val sourcePsi: KtElement?,
) : KaConstantValue.ErrorValue, KaConstantValue.KaErrorConstantValue {
    @Deprecated("Check the class type instead.")
    override val constantValueKind: ConstantValueKind
        get() = ConstantValueKind.Error

    override val value: Nothing
        get() = error("Cannot get value for KaErrorConstantValue")

    override fun render(): String = "error(\"$errorMessage\")"
    override fun toString(): String = render()
}