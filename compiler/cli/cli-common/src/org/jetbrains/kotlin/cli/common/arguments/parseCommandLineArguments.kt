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

import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.utils.SmartList
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
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

private const val ADVANCED_ARGUMENT_PREFIX = "-X"
private const val FREE_ARGS_DELIMITER = "--"

data class ArgumentParseErrors(
    val unknownArgs: MutableList<String> = SmartList(),

    val unknownExtraFlags: MutableList<String> = SmartList(),

    // Names of extra (-X...) arguments which have been passed in an obsolete form ("-Xaaa bbb", instead of "-Xaaa=bbb")
    val extraArgumentsPassedInObsoleteForm: MutableList<String> = SmartList(),

    // Non-boolean arguments which have been passed multiple times, possibly with different values.
    // The key in the map is the name of the argument, the value is the last passed value.
    val duplicateArguments: MutableMap<String, String> = mutableMapOf(),

    // Arguments where [Argument.deprecatedName] was used; the key is the deprecated name, the value is the new name ([Argument.value])
    val deprecatedArguments: MutableMap<String, String> = mutableMapOf(),

    var argumentWithoutValue: String? = null,

    var booleanArgumentWithValue: String? = null,

    val argfileErrors: MutableList<String> = SmartList(),

    // Reports from internal arguments parsers
    val internalArgumentsParsingProblems: MutableList<String> = SmartList()
)

// Parses arguments into the passed [result] object. Errors related to the parsing will be collected into [CommonToolArguments.errors].
fun <A : CommonToolArguments> parseCommandLineArguments(args: List<String>, result: A, overrideArguments: Boolean = false) {
    val errors = result.errors ?: ArgumentParseErrors().also { result.errors = it }
    val preprocessed = preprocessCommandLineArguments(args, errors)
    parsePreprocessedCommandLineArguments(preprocessed, result, errors, overrideArguments)
}

fun <A : CommonToolArguments> parseCommandLineArgumentsFromEnvironment(arguments: A) {
    val settingsFromEnvironment = CompilerSystemProperties.LANGUAGE_VERSION_SETTINGS.value?.takeIf { it.isNotEmpty() }
        ?.split(Regex("""\s""")) ?: return
    parseCommandLineArguments(settingsFromEnvironment, arguments, overrideArguments = true)
}

private fun <A : CommonToolArguments> parsePreprocessedCommandLineArguments(
    args: List<String>,
    result: A,
    errors: ArgumentParseErrors,
    overrideArguments: Boolean
) {
    data class ArgumentField(val property: KMutableProperty1<A, Any?>, val argument: Argument)

    @Suppress("UNCHECKED_CAST")
    val properties = result::class.memberProperties.mapNotNull { property ->
        if (property !is KMutableProperty1<*, *>) return@mapNotNull null
        val argument = property.annotations.firstOrNull { it is Argument } as Argument? ?: return@mapNotNull null
        ArgumentField(property as KMutableProperty1<A, Any?>, argument)
    }

    val visitedArgs = mutableSetOf<String>()
    var freeArgsStarted = false

    fun ArgumentField.matches(arg: String): Boolean {
        if (argument.shortName.takeUnless(String::isEmpty) == arg) {
            return true
        }

        val deprecatedName = argument.deprecatedName
        if (deprecatedName.isNotEmpty() && (deprecatedName == arg || arg.startsWith("$deprecatedName="))) {
            errors.deprecatedArguments[deprecatedName] = argument.value
            return true
        }

        if (argument.value == arg) {
            if (argument.isAdvanced && property.returnType.classifier != Boolean::class) {
                errors.extraArgumentsPassedInObsoleteForm.add(arg)
            }
            return true
        }

        return arg.startsWith(argument.value + "=")
    }

    val freeArgs = ArrayList<String>()
    val internalArguments = ArrayList<InternalArgument>()

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

        if (arg.startsWith(InternalArgumentParser.INTERNAL_ARGUMENT_PREFIX)) {
            val matchingParsers = InternalArgumentParser.PARSERS.filter { it.canParse(arg) }
            assert(matchingParsers.size <= 1) { "Internal error: internal argument $arg can be ambiguously parsed by parsers ${matchingParsers.joinToString()}" }

            val parser = matchingParsers.firstOrNull()

            if (parser == null) {
                errors.unknownExtraFlags += arg
            } else {
                val newInternalArgument = parser.parseInternalArgument(arg, errors) ?: continue
                // Manual language feature setting overrides the previous value of the same feature setting, if it exists.
                internalArguments.removeIf {
                    (it as? ManualLanguageFeatureSetting)?.languageFeature ==
                            (newInternalArgument as? ManualLanguageFeatureSetting)?.languageFeature
                }
                internalArguments.add(newInternalArgument)
            }

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
            argumentField.property.returnType.classifier == Boolean::class -> {
                if (arg.startsWith(argument.value + "=")) {
                    // Can't use toBooleanStrict yet because this part of the compiler is used in Gradle and needs API version 1.4.
                    when (arg.substring(argument.value.length + 1)) {
                        "true" -> true
                        "false" -> false
                        else -> true.also { errors.booleanArgumentWithValue = arg }
                    }
                } else true
            }
            arg.startsWith(argument.value + "=") -> {
                arg.substring(argument.value.length + 1)
            }
            arg.startsWith(argument.deprecatedName + "=") -> {
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
            && !visitedArgs.add(argument.value) && value is String && property.get(result) != value
        ) {
            errors.duplicateArguments[argument.value] = value
        }

        updateField(property, result, value, argument.delimiter, overrideArguments)
    }

    result.freeArgs += freeArgs
    result.updateInternalArguments(internalArguments, overrideArguments)
}

private fun <A : CommonToolArguments> A.updateInternalArguments(
    newInternalArguments: ArrayList<InternalArgument>,
    overrideArguments: Boolean
) {
    val filteredExistingArguments = if (overrideArguments) {
        internalArguments.filter { existingArgument ->
            existingArgument !is ManualLanguageFeatureSetting ||
                    newInternalArguments.none {
                        it is ManualLanguageFeatureSetting && it.languageFeature == existingArgument.languageFeature
                    }
        }
    } else internalArguments

    internalArguments = filteredExistingArguments + newInternalArguments
}

private fun <A : CommonToolArguments> updateField(
    property: KMutableProperty1<A, Any?>,
    result: A,
    value: Any,
    delimiter: String,
    overrideArguments: Boolean
) {
    when (property.returnType.classifier) {
        Boolean::class, String::class -> property.set(result, value)
        Array<String>::class -> {
            val newElements = if (delimiter.isEmpty()) {
                arrayOf(value as String)
            } else {
                (value as String).split(delimiter).toTypedArray()
            }
            @Suppress("UNCHECKED_CAST")
            val oldValue = property.get(result) as Array<String>?
            property.set(result, if (oldValue != null && !overrideArguments) arrayOf(*oldValue, *newElements) else newElements)
        }
        else -> throw IllegalStateException("Unsupported argument type: ${property.returnType}")
    }
}

/**
 * @return error message if arguments are parsed incorrectly, null otherwise
 */
fun validateArguments(errors: ArgumentParseErrors?): String? {
    if (errors == null) return null
    if (errors.argumentWithoutValue != null) {
        return "No value passed for argument ${errors.argumentWithoutValue}"
    }
    errors.booleanArgumentWithValue?.let { arg ->
        return "No value expected for boolean argument ${arg.substringBefore('=')}. Please remove the value: $arg"
    }
    if (errors.unknownArgs.isNotEmpty()) {
        return "Invalid argument: ${errors.unknownArgs.first()}"
    }
    return null
}
