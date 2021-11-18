/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.markers

import org.jetbrains.kotlin.analysis.api.annotations.KtNamedConstantValue
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.types.ConstantValueKind

/**
 * Annotation values are expected to be compile-time constants. According to the
 * [spec](https://kotlinlang.org/spec/annotations.html#annotation-values),
 * allowed kinds are:
 *   * integer types,
 *   * string type,
 *   * enum types,
 *   * other annotation types, and
 *   * array of aforementioned types
 *
 *  [KtLiteralConstantValue] covers first two kinds;
 *  [KtEnumEntryConstantValue] corresponds to enum types;
 *  [KtAnnotationConstantValue] represents annotation types (with annotation fq name and arguments); and
 *  [KtArrayConstantValue] abstracts an array of [KtConstantValue]s.
 */
public sealed class KtConstantValue(
    public open val sourcePsi: KtElement? = null
)

/**
 * This represents an error during expression evaluation or constant conversion.
 */
public class KtErrorValue(
    public val message: String
) : KtConstantValue()

/**
 * This represents an unsupported expression used as an annotation value.
 */
public object KtUnsupportedConstantValue : KtConstantValue()

public class KtArrayConstantValue(
    public val values: Collection<KtConstantValue>,
    override val sourcePsi: KtElement?,
) : KtConstantValue()

public class KtAnnotationConstantValue(
    public val classId: ClassId?,
    public val arguments: List<KtNamedConstantValue>,
    override val sourcePsi: KtCallElement?,
) : KtConstantValue()

public class KtEnumEntryConstantValue(
    public val callableId: CallableId?,
    override val sourcePsi: KtElement?,
) : KtConstantValue()

public class KtLiteralConstantValue<T>(
    public val constantValueKind: ConstantValueKind<T>,
    public val value: T,
    override val sourcePsi: KtElement?,
) : KtConstantValue() {
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
