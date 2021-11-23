/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.name.CallableId
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
 *  [KtLiteralAnnotationValue]  covers first two kinds;
 *  [KtEnumEntryAnnotationValue] corresponds to enum types;
 *  [KtAnnotationApplicationValue] represents annotation types (with annotation fq name and arguments); and
 *  [KtArrayAnnotationValue] abstracts an array of [KtAnnotationValue]s.
 */
public sealed class KtAnnotationValue(
    public open val sourcePsi: KtElement? = null
)

/**
 * This represents an error during expression evaluation or constant conversion.
 */
public class KtErrorValue(
    public val message: String
) : KtAnnotationValue()

/**
 * This represents an unsupported expression used as an annotation value.
 */
public object KtUnsupportedAnnotationValue : KtAnnotationValue()

public class KtArrayAnnotationValue(
    public val values: Collection<KtAnnotationValue>,
    override val sourcePsi: KtElement?,
) : KtAnnotationValue()

public class KtAnnotationApplicationValue(
    public val annotationValue: KtAnnotationApplication,
) : KtAnnotationValue()

public class KtEnumEntryAnnotationValue(
    public val callableId: CallableId?,
    override val sourcePsi: KtElement?,
) : KtAnnotationValue()

public class KtLiteralAnnotationValue<T>(
    public val constantValueKind: ConstantValueKind<T>,
    public val value: T,
    override val sourcePsi: KtElement?,
) : KtAnnotationValue() {
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
