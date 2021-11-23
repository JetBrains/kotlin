/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.base

import org.jetbrains.kotlin.analysis.api.annotations.KtConstantValueRenderer
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.types.ConstantValueKind


public sealed class KtConstantValue(public val constantValueKind: ConstantValueKind<*>) {
    public abstract val value: Any?
    public abstract val sourcePsi: KtElement?

    public class KtNullConstantValue(override val sourcePsi: KtElement?) : KtConstantValue(ConstantValueKind.Null) {
        override val value: Nothing? get() = null
    }

    public class KtBooleanConstantValue(
        override val value: Boolean,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.Boolean)

    public class KtCharConstantValue(
        override val value: Char,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.Char)

    public class KtByteConstantValue(
        override val value: Byte,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.Byte)

    public class KtUnsignedByteConstantValue(
        override val value: UByte,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.UnsignedByte)

    public class KtShortConstantValue(
        override val value: Short,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.Short)

    public class KtUnsignedShortConstantValue(
        override val value: UShort,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.UnsignedShort)

    public class KtIntConstantValue(
        override val value: Int,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.Int)

    public class KtUnsignedIntConstantValue(
        override val value: UInt,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.UnsignedInt)

    public class KtLongConstantValue(
        override val value: Long,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.Long)

    public class KtUnsignedLongConstantValue(
        override val value: ULong,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.UnsignedLong)

    public class KtStringConstantValue(
        override val value: String,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.String)

    public class KtFloatConstantValue(
        override val value: Float,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.Float)

    public class KtDoubleConstantValue(
        override val value: Double,
        override val sourcePsi: KtElement?
    ) : KtConstantValue(ConstantValueKind.Double)

    public class KtErrorConstantValue(
        public val errorMessage: String,
        override val sourcePsi: KtElement?,
    ) : KtConstantValue(ConstantValueKind.Error) {
        override val value: ERROR_VALUE get() = ERROR_VALUE

        public object ERROR_VALUE
    }
}


public fun KtConstantValue.render(): String =
    KtConstantValueRenderer.render(this)