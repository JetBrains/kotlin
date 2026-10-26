/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.utils

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import java.lang.reflect.Modifier

@KaExperimentalApi
internal fun KaSymbolPointer<*>.renderSymbolPointerToString(): String = prettyPrint {
    append(this@renderSymbolPointerToString::class.qualifiedName)
    append("(")
    val fields = javaClass.declaredFields.filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
    printCollection(fields) { field ->
        append(field.name)
        append(": ")
        try {
            field.isAccessible = true
            append(field.get(this@renderSymbolPointerToString).toString())
        } catch (_: Exception) {
            append("ERROR_RENDERING_FIELD")
        }
    }
    append(")")
}
