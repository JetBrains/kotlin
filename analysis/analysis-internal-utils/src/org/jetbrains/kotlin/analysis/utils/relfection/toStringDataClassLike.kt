/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.utils.relfection

import java.lang.reflect.InvocationTargetException
import kotlin.reflect.full.declaredMemberProperties
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import kotlin.reflect.jvm.isAccessible

public fun Any.renderAsDataClassToString(): String = prettyPrint {
    append(this@renderAsDataClassToString::class.qualifiedName)
    append("(")
    printCollection(this@renderAsDataClassToString::class.declaredMemberProperties) { property ->
        append(property.name)
        append(": ")
        val getter = property.getter
        try {
            getter.isAccessible = true
            append(getter.call(this@renderAsDataClassToString).toString())
        } catch (e: InvocationTargetException) {
            append("ERROR_RENDERING_FIELD")
        } catch (_: Exception) {
            println("")
        }
    }
    append(")")
}