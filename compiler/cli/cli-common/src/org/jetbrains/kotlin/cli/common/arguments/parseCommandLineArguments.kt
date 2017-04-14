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
        val delimiter: String = ",",
        val valueDescription: String = "",
        val description: String
)

val Argument.isAdvanced: Boolean
    get() = value.startsWith(ADVANCED_ARGUMENT_PREFIX) && value.length > ADVANCED_ARGUMENT_PREFIX.length

private val ADVANCED_ARGUMENT_PREFIX = "-X"

// Parses arguments in the passed [result] object, or throws an [IllegalArgumentException] with the message to be displayed to the user
fun <A : CommonCompilerArguments> parseCommandLineArguments(args: Array<String>, result: A) {
    data class ArgumentField(val field: Field, val argument: Argument)

    val fields = result::class.java.fields.mapNotNull { field ->
        val argument = field.getAnnotation(Argument::class.java)
        if (argument != null) ArgumentField(field, argument) else null
    }

    val visitedArgs = mutableSetOf<String>()

    var i = 0
    while (i < args.size) {
        val arg = args[i++]
        val argumentField = fields.firstOrNull { (_, argument) ->
            argument.value == arg ||
            argument.shortName.takeUnless(String::isEmpty) == arg ||
            (argument.isAdvanced && arg.startsWith(argument.value + "="))
        }

        if (argumentField == null) {
            if (arg.startsWith(ADVANCED_ARGUMENT_PREFIX)) {
                result.unknownExtraFlags.add(arg)
            }
            else {
                result.freeArgs.add(arg)
            }
            continue
        }

        val (field, argument) = argumentField
        val value: Any = when {
            field.type == Boolean::class.java -> true
            argument.isAdvanced && arg.startsWith(argument.value + "=") -> {
                arg.substring(argument.value.length + 1)
            }
            else -> {
                if (i == args.size) {
                    throw IllegalArgumentException("No value passed for argument $arg")
                }
                if (argument.isAdvanced) {
                    result.extraArgumentsPassedInObsoleteForm.add(arg)
                }
                args[i++]
            }
        }

        if (!field.type.isArray && !visitedArgs.add(argument.value) && value is String && field.get(result) != value) {
            result.duplicateArguments.put(argument.value, value)
        }

        updateField(field, result, value, argument.delimiter)
    }
}

private fun <A : CommonCompilerArguments> updateField(field: Field, result: A, value: Any, delimiter: String) {
    when (field.type) {
        Boolean::class.java, String::class.java -> field.set(result, value)
        Array<String>::class.java -> {
            val newElements = (value as String).split(delimiter).toTypedArray()
            @Suppress("UNCHECKED_CAST")
            val oldValue = field.get(result) as Array<String>?
            field.set(result, if (oldValue != null) arrayOf(*oldValue, *newElements) else newElements)
        }
        else -> throw IllegalStateException("Unsupported argument type: ${field.type}")
    }
}
