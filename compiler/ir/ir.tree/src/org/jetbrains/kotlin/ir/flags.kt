/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.descriptors.Modality

/* Flag values for IrDeclaration.flags */
object IrFlags {
    const val MODALITY_MASK = 0x00000003
    const val MODALITY_FINAL = 0x0
    const val MODALITY_SEALED = 0x1
    const val MODALITY_OPEN = 0x2
    const val MODALITY_ABSTRACT = 0x3

    const val IS_INLINE = 0x00000004
    const val IS_EXTERNAL = 0x00000008
    const val IS_TAILREC = 0x00000010
    const val IS_SUSPEND = 0x00000020
    const val IS_OPERATOR = 0x00000040
    const val IS_INFIX = 0x00000080
    const val IS_EXPECT = 0x00000100

    const val IS_PRIMARY = 0x00000200

    const val IS_VAR = 0x00000400
    const val IS_CONST = 0x00000800
    const val IS_LATEINIT = 0x00001000
    const val IS_DELEGATED = 0x00002000
    const val IS_FAKE_OVERRIDE = 0x00004000

    const val IS_COMPANION = 0x00008000
    const val IS_INNER = 0x00010000
    const val IS_DATA = 0x00020000
    const val IS_FUN = 0x00040000

    const val IS_FINAL = 0x00080000
    const val IS_STATIC = 0x00100000

    const val IS_CROSSINLINE = 0x00200000
    const val IS_NOINLINE = 0x00400000
    const val IS_HIDDEN = 0x00800000
    const val IS_ASSIGNABLE = 0x01000000
}

fun Modality.toFlags() = when (this) {
    Modality.FINAL -> IrFlags.MODALITY_FINAL
    Modality.SEALED -> IrFlags.MODALITY_SEALED
    Modality.OPEN -> IrFlags.MODALITY_OPEN
    Modality.ABSTRACT -> IrFlags.MODALITY_ABSTRACT
}

fun Int.toModality() = when (this and IrFlags.MODALITY_MASK) {
    IrFlags.MODALITY_FINAL -> Modality.FINAL
    IrFlags.MODALITY_SEALED -> Modality.SEALED
    IrFlags.MODALITY_OPEN -> Modality.OPEN
    IrFlags.MODALITY_ABSTRACT -> Modality.ABSTRACT
    else -> error("Impossible")
}

fun Int.setModality(modality: Modality): Int = (this and IrFlags.MODALITY_MASK.inv()) or modality.toFlags()

fun Boolean.toFlag(flag: Int) = if (this) flag else 0

fun Int.getFlag(flag: Int) = (this and flag) == flag
