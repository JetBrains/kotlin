/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.name.Name

object ConversionNames {
    val TO_BYTE = Name.identifier("toByte")
    val TO_CHAR = Name.identifier("toChar")
    val TO_DOUBLE = Name.identifier("toDouble")
    val TO_FLOAT = Name.identifier("toFloat")
    val TO_INT = Name.identifier("toInt")
    val TO_LONG = Name.identifier("toLong")
    val TO_SHORT = Name.identifier("toShort")
}