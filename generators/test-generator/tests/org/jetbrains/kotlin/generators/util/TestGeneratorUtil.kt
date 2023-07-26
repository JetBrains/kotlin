/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.generators.util

import org.intellij.lang.annotations.Language
import java.io.File
import java.lang.StringBuilder

object TestGeneratorUtil {
    @Language("RegExp") const val KT_OR_KTS = """^(.+)\.(kt|kts)$"""
    @Language("RegExp") const val KT = """^(.+)\.(kt)$"""
    @Language("RegExp") const val KTS = """^(.+)\.(kts)$"""
    @Language("RegExp") const val KT_OR_KTS_WITHOUT_DOTS_IN_NAME = """^([^.]+)\.(kt|kts)$"""

    @Language("RegExp") const val KT_WITHOUT_DOTS_IN_NAME = """^([^.]+)\.kt$"""
    @Language("RegExp") const val KT_WITHOUT_FIR_PREFIX = """^(.+)(?<!\.fir)\.kt$"""

    @JvmStatic
    fun escapeForJavaIdentifier(fileName: String): String {
        // A file name may contain characters (like ".") that can't be a part of method name
        val result = StringBuilder()
        for (c in fileName) {
            if (Character.isJavaIdentifierPart(c)) {
                result.append(c)
            } else {
                result.append("_")
            }
        }
        return result.toString()
    }

    @JvmStatic
    fun fileNameToJavaIdentifier(file: File): String {
        return escapeForJavaIdentifier(file.name).replaceFirstChar(Char::uppercaseChar)
    }

    fun getMainClassName(): String? =
        Throwable().stackTrace.lastOrNull()?.className
}

private val defaultPackages = listOf(
    "java.lang",
    "kotlin",
    "kotlin.annotations",
    "kotlin.collections"
)

fun Class<*>.isDefaultImportedClass(): Boolean {
    val outerName = canonicalName.removeSuffix(".$simpleName")
    return outerName in defaultPackages
}
