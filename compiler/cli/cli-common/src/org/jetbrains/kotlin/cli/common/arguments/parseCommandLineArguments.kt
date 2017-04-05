/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.common.arguments

import java.lang.reflect.Field

annotation class Argument(
        val value: String,
        val shortName: String = "",
        val valueDescription: String = "",
        val description: String
)

val Argument.isAdvanced: Boolean
    get() = value.startsWith("-X") && value.length > 2

fun <A : CommonCompilerArguments> parseCommandLineArguments(args: Array<String>, result: A): List<String> {
    data class ArgumentField(val field: Field, val argumentNames: List<String>)

    val fields = result::class.java.fields.mapNotNull { field ->
        val argument = field.getAnnotation(Argument::class.java)
        if (argument != null)
            ArgumentField(field, listOfNotNull(argument.value, argument.shortName.takeUnless(String::isEmpty)))
        else null
    }

    val freeArgs = mutableListOf<String>()
    var i = 0
    while (i < args.size) {
        val arg = args[i++]
        val field = fields.firstOrNull { (_, names) -> arg in names }?.field
        if (field == null) {
            freeArgs.add(arg)
            continue
        }

        val value: Any =
                if (field.type == Boolean::class.java) true
                else {
                    if (i == args.size) {
                        throw IllegalArgumentException("No value passed for argument $arg")
                    }
                    args[i++]
                }

        updateField(field, result, value)
    }

    return freeArgs
}

private fun <A : CommonCompilerArguments> updateField(field: Field, result: A, value: Any) {
    when (field.type) {
        Boolean::class.java, String::class.java -> field.set(result, value)
        Array<String>::class.java -> {
            val newElements = (value as String).split(",").toTypedArray()
            @Suppress("UNCHECKED_CAST")
            val oldValue = field.get(result) as Array<String>?
            field.set(result, if (oldValue != null) arrayOf(*oldValue, *newElements) else newElements)
        }
        else -> throw IllegalStateException("Unsupported argument type: ${field.type}")
    }
}
