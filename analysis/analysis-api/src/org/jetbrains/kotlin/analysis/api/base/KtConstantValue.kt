/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.base

import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.types.ConstantValueKind


/**
 * A Kotlin constant value. This value amy be used as `const val` initializer or annotation argument.
 * Also, may represent evaluated constant value. So, `1 + 2` will be represented as `KaIntConstantValue(3)`
 *
 * For more info about constant values please see [official Kotlin documentation](https://kotlinlang.org/docs/properties.html#compile-time-constants])
 */
public sealed class KaConstantValue(public val constantValueKind: ConstantValueKind) {
    /**
     * The constant value. The type of this value is always the type specified in its name, i.e, it is `Boolean` for [KaBooleanConstantValue]
     *
     * It is null only for [KaNullConstantValue]
     */
    public abstract val value: Any?

    /**
     * Source element from which the value was created. May be null for constants from non-source files.
     */
    public abstract val sourcePsi: KtElement?

    /**
     * Constant value represented as Kotlin code. E.g: `1`, `2f, `3u` `null`, `"str"`
     */
    public abstract fun renderAsKotlinConstant(): String

    public class KaNullConstantValue(override val sourcePsi: KtElement?) : KaConstantValue(ConstantValueKind.Null) {
        override val value: Nothing? get() = null
        override fun renderAsKotlinConstant(): String = "null"
    }

    public class KaBooleanConstantValue(
        override val value: Boolean,
        override val sourcePsi: KtElement?
    ) : KaConstantValue(ConstantValueKind.Boolean) {
        override fun renderAsKotlinConstant(): String = value.toString()
    }

    public class KaCharConstantValue(
        override val value: Char,
        override val sourcePsi: KtElement?
    ) : KaConstantValue(ConstantValueKind.Char) {
        override fun renderAsKotlinConstant(): String = "`$value`"
    }

    public class KaByteConstantValue(
        override val value: Byte,
        override val sourcePsi: KtElement?
    ) : KaConstantValue(ConstantValueKind.Byte) {
        override fun renderAsKotlinConstant(): String = value.toString()
    }

    public class KaUnsignedByteConstantValue(
        override val value: UByte,
        override val sourcePsi: KtElement?
    ) : KaConstantValue(ConstantValueKind.UnsignedByte) {
        override fun renderAsKotlinConstant(): String = "${value}u"
    }

    public class KaShortConstantValue(
        override val value: Short,
        override val sourcePsi: KtElement?
    ) : KaConstantValue(ConstantValueKind.Short) {
        override fun renderAsKotlinConstant(): String = value.toString()
    }

    public class KaUnsignedShortConstantValue(
        override val value: UShort,
        override val sourcePsi: KtElement?
    ) : KaConstantValue(ConstantValueKind.UnsignedShort) {
        override fun renderAsKotlinConstant(): String = "${value}u"
    }

    public class KaIntConstantValue(
        override val value: Int,
        override val sourcePsi: KtElement?
    ) : KaConstantValue(ConstantValueKind.Int) {
        override fun renderAsKotlinConstant(): String = value.toString()
    }

    public class KaUnsignedIntConstantValue(
        override val value: UInt,
        override val sourcePsi: KtElement?
    ) : KaConstantValue(ConstantValueKind.UnsignedInt) {
        override fun renderAsKotlinConstant(): String = "${value}u"
    }

    public class KaLongConstantValue(
        override val value: Long,
        override val sourcePsi: KtElement?
    ) : KaConstantValue(ConstantValueKind.Long) {
        override fun renderAsKotlinConstant(): String = value.toString()
    }

    public class KaUnsignedLongConstantValue(
        override val value: ULong,
        override val sourcePsi: KtElement?
    ) : KaConstantValue(ConstantValueKind.UnsignedLong) {
        override fun renderAsKotlinConstant(): String = "${value}uL"
    }

    public class KaStringConstantValue(
        override val value: String,
        override val sourcePsi: KtElement?
    ) : KaConstantValue(ConstantValueKind.String) {
        override fun renderAsKotlinConstant(): String = "\"${value}\""
    }

    public class KaFloatConstantValue(
        override val value: Float,
        override val sourcePsi: KtElement?
    ) : KaConstantValue(ConstantValueKind.Float) {
        override fun renderAsKotlinConstant(): String = "${value}f"
    }

    public class KaDoubleConstantValue(
        override val value: Double,
        override val sourcePsi: KtElement?
    ) : KaConstantValue(ConstantValueKind.Double) {
        override fun renderAsKotlinConstant(): String = value.toString()
    }

    /**
     * Value which is not cosntant or there was an error (e.g, division by 0) bug during value evaluation
     */
    public class KaErrorConstantValue(
        public val errorMessage: String,
        override val sourcePsi: KtElement?,
    ) : KaConstantValue(ConstantValueKind.Error) {
        override val value: Nothing
            get() = error("Cannot get value for KaErrorConstantValue")

        override fun renderAsKotlinConstant(): String {
            return "error(\"$errorMessage\")"
        }
    }
}

public typealias KtConstantValue = KaConstantValue