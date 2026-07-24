/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * The object contains [ClassId]s of well known Kotlin types.
 */
public object KaStandardTypeClassIds {
    /** The [Unit] class ID. */
    public val UNIT: ClassId get() = StandardClassIds.Unit

    /** The [Int] class ID. */
    public val INT: ClassId get() = StandardClassIds.Int

    /** The [Long] class ID. */
    public val LONG: ClassId get() = StandardClassIds.Long

    /** The [Short] class ID. */
    public val SHORT: ClassId get() = StandardClassIds.Short

    /** The [Byte] class ID. */
    public val BYTE: ClassId get() = StandardClassIds.Byte

    /** The [Float] class ID. */
    public val FLOAT: ClassId get() = StandardClassIds.Float

    /** The [Double] class ID. */
    public val DOUBLE: ClassId get() = StandardClassIds.Double

    /** The [Char] class ID. */
    public val CHAR: ClassId get() = StandardClassIds.Char

    /** The [Boolean] class ID. */
    public val BOOLEAN: ClassId get() = StandardClassIds.Boolean

    /** The [String] class ID. */
    public val STRING: ClassId get() = StandardClassIds.String

    /** The [CharSequence] class ID. */
    public val CHAR_SEQUENCE: ClassId get() = StandardClassIds.CharSequence

    /** The [Any] class ID. */
    public val ANY: ClassId get() = StandardClassIds.Any

    /** The [Nothing] class ID. */
    public val NOTHING: ClassId get() = StandardClassIds.Nothing

    /** A set of primitive class IDs. */
    public val PRIMITIVES: Set<ClassId> = setOf(INT, LONG, SHORT, BYTE, FLOAT, DOUBLE, CHAR, BOOLEAN)
}
