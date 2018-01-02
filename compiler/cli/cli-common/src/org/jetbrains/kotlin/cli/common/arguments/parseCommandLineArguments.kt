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

import org.jetbrains.kotlin.utils.SmartList
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

@Target(AnnotationTarget.PROPERTY)
annotation class Argument(
        val value: String,
        val shortName: String = "",
        val deprecatedName: String = "",
        val delimiter: String = ",",
        val valueDescription: String = "",
        val description: String
)

val Argument.isAdvanced: Boolean
    get() = value.startsWith(ADVANCED_ARGUMENT_PREFIX) && value.length > ADVANCED_ARGUMENT_PREFIX.length

private val ADVANCED_ARGUMENT_PREFIX = "-X"
private val FREE_ARGS_DELIMITER = "--"

data class ArgumentParseErrors(
    val unknownArgs: MutableList<String> = SmartList<String>(),

    val unknownExtraFlags: MutableList<String> = SmartList<String>(),

    // Names of extra (-X...) arguments which have been passed in an obsolete form ("-Xaaa bbb", instead of "-Xaaa=bbb")
    val extraArgumentsPassedInObsoleteForm: MutableList<String> = SmartList<String>(),

    // Non-boolean arguments which have been passed multiple times, possibly with different values.
    // The key in the map is the name of the argument, the value is the last passed value.
    val duplicateArguments: MutableMap<String, String> = mutableMapOf(),

    // Arguments where [Argument.deprecatedName] was used; the key is the deprecated name, the value is the new name ([Argument.value])
    val deprecatedArguments: MutableMap<String, String> = mutableMapOf(),

    var argumentWithoutValue: String? = null
)

// Parses arguments into the passed [result] object. Errors related to the parsing will be collected into [CommonToolArguments.errors].
fun <A : CommonToolArguments> parseCommandLineArguments(args: List<String>, result: A) {
    data class ArgumentField(val property: KMutableProperty1<A, Any?>, val argument: Argument)

    @Suppress("UNCHECKED_CAST")
    val properties = result::class.memberProperties.mapNotNull { property ->
        if (property !is KMutableProperty1<*, *>) return@mapNotNull null
        val argument = property.findAnnotation<Argument>() ?: return@mapNotNull null
        ArgumentField(property as KMutableProperty1<A, Any?>, argument)
    }

    val errors = result.errors
    val visitedArgs = mutableSetOf<String>()
    var freeArgsStarted = false

    fun ArgumentField.matches(arg: String): Boolean {
        if (argument.shortName.takeUnless(String::isEmpty) == arg) {
            return true
        }

        val deprecatedName = argument.deprecatedName.takeUnless(String::isEmpty)
        if (deprecatedName == arg) {
            errors.deprecatedArguments[deprecatedName] = argument.value
            return true
        }

        if (argument.isAdvanced) {
            if (argument.value == arg) {
                if (property.returnType.classifier != Boolean::class) {
                    errors.extraArgumentsPassedInObsoleteForm.add(arg)
                }
                return true
            }

            if (deprecatedName != null && arg.startsWith(deprecatedName + "=")) {
                errors.deprecatedArguments[deprecatedName] = argument.value
                return true
            }

            return arg.startsWith(argument.value + "=")
        }

        return argument.value == arg
    }

    val freeArgs = ArrayList<String>()

    var i = 0
    loop@ while (i < args.size) {
        val arg = args[i++]

        if (freeArgsStarted) {
            freeArgs.add(arg)
            continue
        }
        if (arg == FREE_ARGS_DELIMITER) {
            freeArgsStarted = true
            continue
        }

        val argumentField = properties.firstOrNull { it.matches(arg) }
        if (argumentField == null) {
            when {
                arg.startsWith(ADVANCED_ARGUMENT_PREFIX) -> errors.unknownExtraFlags.add(arg)
                arg.startsWith("-") -> errors.unknownArgs.add(arg)
                else -> freeArgs.add(arg)
            }
            continue
        }

        val (property, argument) = argumentField
        val value: Any = when {
            argumentField.property.returnType.classifier == Boolean::class -> true
            argument.isAdvanced && arg.startsWith(argument.value + "=") -> {
                arg.substring(argument.value.length + 1)
            }
            argument.isAdvanced && arg.startsWith(argument.deprecatedName + "=") -> {
                arg.substring(argument.deprecatedName.length + 1)
            }
            i == args.size -> {
                errors.argumentWithoutValue = arg
                break@loop
            }
            else -> {
                args[i++]
            }
        }

        if ((argumentField.property.returnType.classifier as? KClass<*>)?.java?.isArray == false
            && !visitedArgs.add(argument.value) && value is String && property.get(result) != value) {
            errors.duplicateArguments.put(argument.value, value)
        }

        updateField(property, result, value, argument.delimiter)
    }

    result.freeArgs += freeArgs
}

private fun <A : CommonToolArguments> updateField(property: KMutableProperty1<A, Any?>, result: A, value: Any, delimiter: String) {
    when (property.returnType.classifier) {
        Boolean::class, String::class -> property.set(result, value)
        Array<String>::class -> {
            val newElements = (value as String).split(delimiter).toTypedArray()
            @Suppress("UNCHECKED_CAST")
            val oldValue = property.get(result) as Array<String>?
            property.set(result, if (oldValue != null) arrayOf(*oldValue, *newElements) else newElements)
        }
        else -> throw IllegalStateException("Unsupported argument type: ${property.returnType}")
    }
}

/**
 * @return error message if arguments are parsed incorrectly, null otherwise
 */
fun validateArguments(errors: ArgumentParseErrors): String? {
    if (errors.argumentWithoutValue != null) {
        return "No value passed for argument ${errors.argumentWithoutValue}"
    }
    if (errors.unknownArgs.isNotEmpty()) {
        return "Invalid argument: ${errors.unknownArgs.first()}"
    }
    return null
}
