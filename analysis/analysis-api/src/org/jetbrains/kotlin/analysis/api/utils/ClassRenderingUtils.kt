/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.utils

import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

internal fun Any.renderAsDataClassToString(): String = prettyPrint {
    append(this@renderAsDataClassToString::class.qualifiedName)
    append("(")
    printCollection(this@renderAsDataClassToString::class.declaredMemberProperties) { property ->
        append(property.name)
        append(": ")
        val getter = property.getter
        try {
            getter.isAccessible = true
            append(getter.call(this@renderAsDataClassToString).toString())
        } catch (_: InvocationTargetException) {
            append("ERROR_RENDERING_FIELD")
        } catch (_: Exception) {
            println("")
        }
    }
    append(")")
}
