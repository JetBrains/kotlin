/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.base

import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.types.ConstantValueKind


/**
 * A Kotlin constant value. This value amy be used as `const val` initializer or annotation argument.
 * Also, may represent evaluated constant value. So, `1 + 2` will be represented as `KtIntConstantValue(3)`
 *
 * For more info about constant values please see [official Kotlin documentation](https://kotlinlang.org/docs/properties.html#compile-time-constants])
 */
public sealed class KtConstantValue(public val constantValueKind: ConstantValueKind<*>) {
    /**
     * The constant value. The type of this value is always the type specified in its name, i.e, it is `Boolean` for [KtBooleanConstantValue]
     *
     * It is null only for [KtNullConstantValue]
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

    public class KtNullConstantValue(override val sourcePsi: KtElement?) : KtConstantValue(ConstantValueKind.Null) {
        override val value: Nothing? get() = null
        override fun renderAsKotlinConstant(): String = "null"
    }

    public class KtBooleanConstantValue(
        override val value: Boolean,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.Boolean) {
        override fun renderAsKotlinConstant(): String = value.toString()
    }

    public class KtCharConstantValue(
        override val value: Char,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.Char) {
        override fun renderAsKotlinConstant(): String = "`$value`"
    }

    public class KtByteConstantValue(
        override val value: Byte,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.Byte) {
        override fun renderAsKotlinConstant(): String = value.toString()
    }

    public class KtUnsignedByteConstantValue(
        override val value: UByte,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.UnsignedByte) {
        override fun renderAsKotlinConstant(): String = "${value}u"
    }

    public class KtShortConstantValue(
        override val value: Short,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.Short) {
        override fun renderAsKotlinConstant(): String = value.toString()
    }

    public class KtUnsignedShortConstantValue(
        override val value: UShort,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.UnsignedShort) {
        override fun renderAsKotlinConstant(): String = "${value}u"
    }

    public class KtIntConstantValue(
        override val value: Int,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.Int) {
        override fun renderAsKotlinConstant(): String = value.toString()
    }

    public class KtUnsignedIntConstantValue(
        override val value: UInt,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.UnsignedInt) {
        override fun renderAsKotlinConstant(): String = "${value}u"
    }

    public class KtLongConstantValue(
        override val value: Long,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.Long) {
        override fun renderAsKotlinConstant(): String = value.toString()
    }

    public class KtUnsignedLongConstantValue(
        override val value: ULong,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.UnsignedLong) {
        override fun renderAsKotlinConstant(): String = "${value}uL"
    }

    public class KtStringConstantValue(
        override val value: String,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.String) {
        override fun renderAsKotlinConstant(): String = "\"${value}\""
    }

    public class KtFloatConstantValue(
        override val value: Float,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.Float) {
        override fun renderAsKotlinConstant(): String = "${value}f"
    }

    public class KtDoubleConstantValue(
        override val value: Double,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.Double) {
        override fun renderAsKotlinConstant(): String = value.toString()
    }

    /**
     * Value which is not cosntant or there was an error (e.g, division by 0) bug during value evaluation
     */
    public class KtErrorConstantValue(
        public val errorMessage: String,
        override val sourcePsi: KtElement?,
    ) : KtConstantValue(ConstantValueKind.Error) {
        override val value: Nothing
            get() = error("Cannot get value for KtErrorConstantValue")

        override fun renderAsKotlinConstant(): String {
            return "error(\"$errorMessage\")"
        }
    }
}
