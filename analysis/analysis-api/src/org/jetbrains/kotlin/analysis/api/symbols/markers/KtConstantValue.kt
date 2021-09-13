/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.markers

import org.jetbrains.kotlin.types.ConstantValueKind

public sealed class KtConstantValue
public object KtUnsupportedConstantValue : KtConstantValue()

public data class KtSimpleConstantValue<T>(val constantValueKind: ConstantValueKind<T>, val value: T) : KtConstantValue() {
    public fun toConst(): Any? {
        return (value as? Long)?.let {
            when (constantValueKind) {
                ConstantValueKind.Byte -> it.toByte()
                ConstantValueKind.Short -> it.toShort()
                ConstantValueKind.Int -> it.toInt()
                ConstantValueKind.Float -> it.toFloat()
                ConstantValueKind.Double -> it.toDouble()

                ConstantValueKind.UnsignedByte -> it.toUByte()
                ConstantValueKind.UnsignedShort -> it.toUShort()
                ConstantValueKind.UnsignedInt -> it.toUInt()
                ConstantValueKind.UnsignedLong -> it.toULong()
                else -> it
            }
        } ?: value
    }
}
