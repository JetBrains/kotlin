/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.contextParameters

import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsSourceGenerator
import java.io.PrintWriter

class GenerateContextFunctions(out: PrintWriter) : BuiltInsSourceGenerator(out) {
    override fun getMultifileClassName(): String = "ContextParametersKt"

    override fun generateBody() {
        generateSingleFunction(listOf("with"), listOf("T"), "R")
        for (i in 2..6) {
            val parameterNames = ('a' .. 'z').take(i)
            generateSingleFunction(
                parameterNames.map { it.toString() },
                parameterNames.map { it.uppercase() },
                "R"
            )
        }
    }

    fun generateSingleFunction(parameterNames: List<String>, parameterTypes: List<String>, resultType: String) {
        val arguments = parameterNames.joinToString()
        val parameters = parameterNames.zip(parameterTypes) { name, type -> "$name: $type" }.joinToString()
        val types = parameterTypes.joinToString()

        val values = if (parameterTypes.size == 1) "value" else "values"
        val receivers = if (parameterTypes.size == 1) "receiver" else "receivers"

        out.println(
            """
/**
 * Runs the specified [block] with the given $values in context scope.
 *
 * As opposed to [with], [context] only makes the $values available for
 * context parameter resolution, but not as implicit $receivers.
 *
 * @sample samples.misc.ContextParameters.useContext
 */
@kotlin.internal.InlineOnly
@SinceKotlin("2.2")
public inline fun <$types, $resultType> context($parameters, block: context($types) () -> $resultType): $resultType {
    kotlin.contracts.contract {
        callsInPlace(block, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return block($arguments)
}
"""
        )
    }

}
