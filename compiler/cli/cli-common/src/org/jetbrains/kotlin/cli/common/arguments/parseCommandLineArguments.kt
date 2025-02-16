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
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.utils.SmartList
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.cast

@Target(AnnotationTarget.FIELD)
annotation class Argument(
    val value: String,
    val shortName: String = "",
    val deprecatedName: String = "",
    @property:RawDelimiter
    val delimiter: String = Delimiters.default,
    val valueDescription: String = "",
    val description: String
) {
    @RequiresOptIn(
        message = "The raw delimiter value needs to be resolved. See 'resolvedDelimiter'. Using the raw value requires opt-in",
        level = RequiresOptIn.Level.ERROR
    )
    annotation class RawDelimiter

    object Delimiters {
        const val default = ","
        const val none = ""
        const val pathSeparator = "<path_separator>"
    }
}

val Argument.isAdvanced: Boolean
    get() = isSpecial(ADVANCED_ARGUMENT_PREFIX)

val Argument.isInternal: Boolean
    get() = isSpecial(INTERNAL_ARGUMENT_PREFIX)

private fun Argument.isSpecial(prefix: String): Boolean {
    return value.startsWith(prefix) && value.length > prefix.length
}

@OptIn(Argument.RawDelimiter::class)
val Argument.resolvedDelimiter: String?
    get() = when (delimiter) {
        Argument.Delimiters.none -> null
        Argument.Delimiters.pathSeparator -> File.pathSeparator
        else -> delimiter
    }

private const val ADVANCED_ARGUMENT_PREFIX = "-X"
internal const val INTERNAL_ARGUMENT_PREFIX = "-XX"
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
    val internalArgumentsParsingProblems: MutableList<Pair<CompilerMessageSeverity, String>> = SmartList()
)

inline fun <reified T : CommonToolArguments> parseCommandLineArguments(args: List<String>): T {
    return parseCommandLineArguments(T::class, args)
}

fun <T : CommonToolArguments> parseCommandLineArguments(clazz: KClass<T>, args: List<String>): T {
    val constructor = clazz.java.constructors.find { it.parameters.isEmpty() }
        ?: error("Missing empty constructor on '${clazz.java.name}")
    val arguments = clazz.cast(constructor.newInstance())
    parseCommandLineArguments(args, arguments)
    return arguments
}


// Parses arguments into the passed [result] object. Errors related to the parsing will be collected into [CommonToolArguments.errors].
fun <A : CommonToolArguments> parseCommandLineArguments(args: List<String>, result: A, overrideArguments: Boolean = false) {
    val errors = lazy { result.errors ?: ArgumentParseErrors().also { result.errors = it } }
    val preprocessed = preprocessCommandLineArguments(args, errors)
    parsePreprocessedCommandLineArguments(preprocessed, result, errors, overrideArguments)
}

fun <A : CommonToolArguments> parseCommandLineArgumentsFromEnvironment(arguments: A) {
    val settingsFromEnvironment = CompilerSystemProperties.LANGUAGE_VERSION_SETTINGS.value?.takeIf { it.isNotEmpty() }
        ?.split(Regex("""\s"""))
        ?.filterNot { it.isBlank() }
        ?: return
    parseCommandLineArguments(settingsFromEnvironment, arguments, overrideArguments = true)
}

private data class ArgumentField(val getter: Method, val setter: Method, val argument: Argument)

private val argumentsCache = ConcurrentHashMap<Class<*>, Map<String, ArgumentField>>()

private fun getArguments(klass: Class<*>): Map<String, ArgumentField> = argumentsCache.getOrPut(klass) {
    if (klass == Any::class.java) emptyMap()
    else buildMap {
        putAll(getArguments(klass.superclass))
        for (field in klass.declaredFields) {
            field.getAnnotation(Argument::class.java)?.let { argument ->
                val getter = klass.getMethod(JvmAbi.getterName(field.name))
                val setter = klass.getMethod(JvmAbi.setterName(field.name), field.type)
                val argumentField = ArgumentField(getter, setter, argument)
                for (key in listOf(argument.value, argument.shortName, argument.deprecatedName)) {
                    if (key.isNotEmpty()) put(key, argumentField)
                }
            }
        }
    }
}

private fun <A : CommonToolArguments> parsePreprocessedCommandLineArguments(
    args: List<String>,
    result: A,
    errors: Lazy<ArgumentParseErrors>,
    overrideArguments: Boolean
) {
    val properties = getArguments(result::class.java)

    val visitedArgs = mutableSetOf<String>()
    var freeArgsStarted = false

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

        val key = arg.substringBefore('=')
        val argumentField = properties[key]
        if (argumentField == null) {
            when {
                // Unknown -XX argument
                arg.startsWith(INTERNAL_ARGUMENT_PREFIX) -> {
                    val matchingParsers = InternalArgumentParser.PARSERS.filter { it.canParse(arg) }
                    assert(matchingParsers.size <= 1) { "Internal error: internal argument $arg can be ambiguously parsed by parsers ${matchingParsers.joinToString()}" }

                    val parser = matchingParsers.firstOrNull()

                    if (parser == null) {
                        errors.value.unknownExtraFlags += arg
                    } else {
                        val newInternalArgument = parser.parseInternalArgument(arg, errors.value) ?: continue
                        // Manual language feature setting overrides the previous value of the same feature setting, if it exists.
                        internalArguments.removeIf {
                            (it as? ManualLanguageFeatureSetting)?.languageFeature ==
                                    (newInternalArgument as? ManualLanguageFeatureSetting)?.languageFeature
                        }
                        internalArguments.add(newInternalArgument)
                    }
                }
                // Unknown -X argument
                arg.startsWith(ADVANCED_ARGUMENT_PREFIX) -> errors.value.unknownExtraFlags.add(arg)
                arg.startsWith("-") -> errors.value.unknownArgs.add(arg)
                else -> freeArgs.add(arg)
            }
            continue
        }

        val (getter, setter, argument) = argumentField

        // Tests for -shortName=value, which isn't currently allowed.
        if (key != arg && key == argument.shortName) {
            errors.value.unknownArgs.add(arg)
            continue
        }

        val deprecatedName = argument.deprecatedName
        if (deprecatedName == key) {
            errors.value.deprecatedArguments[deprecatedName] = argument.value
        }

        if (argument.value == arg) {
            if (argument.isAdvanced && getter.returnType.kotlin != Boolean::class) {
                errors.value.extraArgumentsPassedInObsoleteForm.add(arg)
            }
        }

        val value: Any = when {
            getter.returnType.kotlin == Boolean::class -> {
                if (arg.startsWith(argument.value + "=")) {
                    // Can't use toBooleanStrict yet because this part of the compiler is used in Gradle and needs API version 1.4.
                    when (arg.substring(argument.value.length + 1)) {
                        "true" -> true
                        "false" -> false
                        else -> true.also { errors.value.booleanArgumentWithValue = arg }
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
                errors.value.argumentWithoutValue = arg
                break@loop
            }
            else -> {
                args[i++]
            }
        }

        if (!getter.returnType.isArray && !visitedArgs.add(argument.value) && value is String && getter(result) != value
        ) {
            errors.value.duplicateArguments[argument.value] = value
        }

        updateField(getter, setter, result, value, argument.resolvedDelimiter, overrideArguments)
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
    getter: Method,
    setter: Method,
    result: A,
    value: Any,
    delimiter: String?,
    overrideArguments: Boolean
) {
    when (getter.returnType.kotlin) {
        Boolean::class, String::class -> setter(result, value)
        Array<String>::class -> {
            val newElements = if (delimiter.isNullOrEmpty()) {
                arrayOf(value as String)
            } else {
                (value as String).split(delimiter).toTypedArray()
            }

            @Suppress("UNCHECKED_CAST")
            val oldValue = getter(result) as Array<String>?
            setter(result, if (oldValue != null && !overrideArguments) arrayOf(*oldValue, *newElements) else newElements)
        }
        else -> throw IllegalStateException("Unsupported argument type: ${getter.returnType}")
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

/**
 * Instructs the annotated argument to enable the specified [LanguageFeature] when set to `true`.
 */
@Target(AnnotationTarget.FIELD)
@Repeatable
annotation class Enables(val feature: LanguageFeature)

/**
 * Instructs the annotated argument to disable the specified [LanguageFeature] when set to `true`.
 */
@Target(AnnotationTarget.FIELD)
@Repeatable
annotation class Disables(val feature: LanguageFeature)
