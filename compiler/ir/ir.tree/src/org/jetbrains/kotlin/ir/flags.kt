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

    const val MODALITY_BITS = 2
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
