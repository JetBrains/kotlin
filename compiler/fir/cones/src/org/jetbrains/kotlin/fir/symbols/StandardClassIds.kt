/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object StandardClassIds {

    private val BASE_KOTLIN_PACKAGE = FqName("kotlin")
    private fun String.baseId() = ClassId(BASE_KOTLIN_PACKAGE, Name.identifier(this))

    val Nothing = "Nothing".baseId()
    val Unit = "Unit".baseId()
    val Any = "Any".baseId()
    val Enum = "Enum".baseId()
    val Annotation = "Annotation".baseId()
    val Array = "Array".baseId()

    val Boolean = "Boolean".baseId()
    val Char = "Char".baseId()
    val Byte = "Byte".baseId()
    val Short = "Short".baseId()

    val Int = "Int".baseId()
    val Long = "Long".baseId()

    val String = "String".baseId()

    val Float = "Float".baseId()
    val Double = "Double".baseId()

    fun byName(name: String) = name.baseId()
}