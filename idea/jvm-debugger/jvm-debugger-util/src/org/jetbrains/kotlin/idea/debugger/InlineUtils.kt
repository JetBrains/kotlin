/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.jdi.LocalVariableProxyImpl
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.inline.INLINE_FUN_VAR_SUFFIX
import org.jetbrains.kotlin.load.java.JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT
import org.jetbrains.kotlin.load.java.JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION

val INLINED_THIS_REGEX = getLocalVariableNameRegexInlineAware(AsmUtil.INLINE_DECLARATION_SITE_THIS)

fun getInlineDepth(variables: List<LocalVariableProxyImpl>): Int {
    val rawInlineFunDepth = variables.count { it.name().startsWith(LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION) }

    for (variable in variables.sortedByDescending { it.variable }) {
        val name = variable.name()
        val depth = getInlineDepth(name)
        if (depth > 0) {
            return depth
        } else if (name.startsWith(LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT)) {
            return 0
        }
    }

    return rawInlineFunDepth
}

fun getInlineDepth(variableName: String): Int {
    var endIndex = variableName.length
    var depth = 0

    val suffixLen = INLINE_FUN_VAR_SUFFIX.length
    while (endIndex >= suffixLen) {
        if (variableName.substring(endIndex - suffixLen, endIndex) != INLINE_FUN_VAR_SUFFIX) {
            break
        }

        depth++
        endIndex -= suffixLen
    }

    return depth
}

fun dropInlineSuffix(name: String): String {
    val depth = getInlineDepth(name)
    if (depth == 0) {
        return name
    }

    return name.dropLast(depth * INLINE_FUN_VAR_SUFFIX.length)
}

private fun getLocalVariableNameRegexInlineAware(name: String): Regex {
    val escapedName = Regex.escape(name)
    val escapedSuffix = Regex.escape(INLINE_FUN_VAR_SUFFIX)
    return Regex("^$escapedName(?:$escapedSuffix)*$")
}