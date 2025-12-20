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
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.java
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * @property isObsolete Set to `true`if you want the compiler to treat this option as unknown and show the appropriate diagnostics,
 * but you still want it around for some reason.
 */
@Target(AnnotationTarget.FIELD)
annotation class Argument(
    val value: String,
    val shortName: String = "",
    val deprecatedName: String = "",
    @property:RawDelimiter
    val delimiter: String = Delimiters.default,
    val valueDescription: String = "",
    val description: String,
    val isObsolete: Boolean = false,
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
        const val space = " "
        const val semicolon = ";"
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

    // Arguments where [Argument.deprecatedName] was used; the key is the deprecated name, the value is the new name ([Argument.value])
    val deprecatedArguments: MutableMap<String, String> = mutableMapOf(),

    var argumentsWithoutValue: MutableList<String> = SmartList(),

    var booleanArgumentsWithIncorrectValue: MutableList<String> = SmartList(),

    var booleanLangFeatureArgumentsWithValue: MutableList<String> = SmartList(),

    val stringLangFeatureArgumentsWithIncorrectValue: MutableList<Pair<String, Set<String>>> = SmartList(),

    val argfileErrors: MutableList<String> = SmartList(),

    // Reports from internal arguments parsers
    val internalArgumentsParsingProblems: MutableList<Pair<CompilerMessageSeverity, String>> = SmartList()
)

inline fun <reified T : CommonToolArguments> parseCommandLineArguments(args: List<String>): T {
    return parseCommandLineArguments(T::class, args)
}

fun <T : CommonToolArguments> parseCommandLineArguments(clazz: KClass<T>, args: List<String>): T {
    val constructor = getArgumentsInfo(clazz.java).defaultArgsConstructor
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

private val argumentsCache = ConcurrentHashMap<Class<*>, ArgumentsInfo>()

data class ArgumentField(
    val getter: Method,
    val setter: Method,
    val argument: Argument,
    val enablesAnnotations: List<Enables>,
    val disablesAnnotations: List<Disables>,
) {
    val changesLanguageFeatures: Boolean
        get() = enablesAnnotations.isNotEmpty() || disablesAnnotations.isNotEmpty()
}

data class ArgumentsInfo(
    val cliArgNameToArguments: Map<String, ArgumentField>,
    val defaultArgsConstructor: Constructor<*>,
) {
    private val defaultArgs: CommonToolArguments by lazy(LazyThreadSafetyMode.PUBLICATION) {
        defaultArgsConstructor.newInstance() as CommonToolArguments
    }

    fun getDefaultValue(argumentField: ArgumentField): Any? = argumentField.getter.invoke(defaultArgs)
}

fun getArgumentsInfo(klass: Class<*>): ArgumentsInfo {
    return argumentsCache.getOrPut(klass) {
        ArgumentsInfo(
            cliArgNameToArguments = buildMap {
                val superclass = klass.superclass
                if (CommonToolArguments::class.java.isAssignableFrom(superclass)) {
                    putAll(getArgumentsInfo(superclass).cliArgNameToArguments)
                }
                for (field in klass.declaredFields) {
                    val argument = field.getAnnotation(Argument::class.java) ?: continue
                    val enablesAnnotations = field.getAnnotationsByType(Enables::class.java).toList()
                    val disablesAnnotations = field.getAnnotationsByType(Disables::class.java).toList()
                    val getter = klass.getMethod(JvmAbi.getterName(field.name))
                    val setter = klass.getMethod(JvmAbi.setterName(field.name), field.type)
                    val argumentField = ArgumentField(getter, setter, argument, enablesAnnotations, disablesAnnotations)
                    for (key in listOf(argument.value, argument.shortName, argument.deprecatedName)) {
                        if (key.isNotEmpty()) put(key, argumentField)
                    }
                }
            },
            defaultArgsConstructor = klass.constructors.find { it.parameters.isEmpty() } ?: error("Missing empty constructor on '${klass.name}"),
        )
    }
}

private fun <A : CommonToolArguments> parsePreprocessedCommandLineArguments(
    args: List<String>,
    result: A,
    errors: Lazy<ArgumentParseErrors>,
    overrideArguments: Boolean
) {
    val properties = getArgumentsInfo(result::class.java).cliArgNameToArguments

    var freeArgsStarted = false

    val freeArgs = ArrayList<String>()

    val explicitArgs = mutableMapOf<ArgumentField, MutableList<Any>>()

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

        // TODO(KT-80348): should be replaced with just '=' when `-XXLanguage` would be removed
        val delimiter = when {
            arg.startsWith("-XXLanguage") -> ':'
            else -> '='
        }
        val key = arg.substringBefore(delimiter)
        val argumentField = properties[key]
        if (argumentField == null) {
            when {
                // Unknown -X argument
                arg.startsWith(ADVANCED_ARGUMENT_PREFIX) -> errors.value.unknownExtraFlags.add(arg)
                arg.startsWith("-") -> errors.value.unknownArgs.add(arg)
                else -> freeArgs.add(arg)
            }
            continue
        }

        val (getter, setter, argument, enablesAnnotations, disablesAnnotations) = argumentField

        // Tests for -shortName=value, which isn't currently allowed.
        if (key != arg && key == argument.shortName) {
            errors.value.unknownArgs.add(arg)
            continue
        }

        if (argument.isObsolete) {
            // Add to unknown to show the diagnostic, but keep parsing.
            errors.value.unknownArgs.add(arg)
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
                val changesLangFeatures = argumentField.changesLanguageFeatures
                if (arg.startsWith(argument.value + delimiter)) {
                    when (arg.substring(argument.value.length + 1)) {
                        "true" -> true
                        "false" -> false
                        else -> true.also {
                            if (!changesLangFeatures) {
                                errors.value.booleanArgumentsWithIncorrectValue.add(arg)
                            }
                        }
                    }.also {
                        if (changesLangFeatures) {
                            errors.value.booleanLangFeatureArgumentsWithValue.add(arg)
                        }
                    }
                } else true
            }
            arg.startsWith(argument.value + delimiter) -> {
                val legalValues = buildSet {
                    enablesAnnotations.forEach { add(it.ifValueIs) }
                    disablesAnnotations.forEach { add(it.ifValueIs) }
                }
                arg.substring(argument.value.length + 1).also {
                    if (legalValues.isNotEmpty() && !legalValues.contains(it)) {
                        errors.value.stringLangFeatureArgumentsWithIncorrectValue.add(arg to legalValues)
                    }
                }
            }
            arg.startsWith(argument.deprecatedName + delimiter) -> {
                arg.substring(argument.deprecatedName.length + 1)
            }
            i == args.size -> {
                errors.value.argumentsWithoutValue.add(arg)
                break@loop
            }
            else -> {
                args[i++]
            }
        }

        val existingValues = explicitArgs.getOrPut(argumentField) { mutableListOf() }

        val newValue = when (getter.returnType.kotlin) {
            Boolean::class, String::class -> value.also { existingValues.add(it) }
            Array<String>::class -> {
                val resolvedDelimiter = argument.resolvedDelimiter
                val valueString = value as String

                val newElements: List<String> = if (resolvedDelimiter.isNullOrEmpty()) {
                    listOf(valueString)
                } else {
                    valueString.split(resolvedDelimiter)
                }

                val oldValue: MutableList<String>? = if (!overrideArguments) {
                    existingValues.firstIsInstanceOrNull<MutableList<String>>()
                } else {
                    null
                }

                val resultElements: MutableList<String> = oldValue?.also { it.addAll(newElements) }
                    ?: newElements.toMutableList().also { existingValues.add(it) }

                resultElements.toTypedArray()
            }
            else -> throw IllegalStateException("Unexpected argument type: ${getter.returnType}")
        }
        setter(result, newValue)
    }

    result.freeArgs += freeArgs
    result.explicitArguments = explicitArgs

    if (result is CommonCompilerArguments) {
        val internalArguments = ArrayList<ManualLanguageFeatureSetting>()
        for (arg in result.manuallyConfiguredFeatures.orEmpty()) {
            val featureSetting = LanguageSettingsParser.parseLanguageFeature(arg, "-XXLanguage:$arg", errors.value) ?: continue
            internalArguments.removeIf {
                it.languageFeature == featureSetting.languageFeature
            }
            internalArguments.add(featureSetting)
        }
        result.updateInternalArguments(internalArguments, overrideArguments)
    }
}

private fun <A : CommonToolArguments> A.updateInternalArguments(
    newInternalArguments: ArrayList<ManualLanguageFeatureSetting>,
    overrideArguments: Boolean
) {
    val filteredExistingArguments = if (overrideArguments) {
        internalArguments.filter { existingArgument ->
            newInternalArguments.none { it.languageFeature == existingArgument.languageFeature }
        }
    } else internalArguments

    internalArguments = filteredExistingArguments + newInternalArguments
}

/**
 * @return comprehensive error message (all child error messages separated by line break) if arguments are parsed incorrectly.
 * Avoid changing the signature because it's used externally.
 */
fun validateArguments(errors: ArgumentParseErrors?): String? {
    return validateArgumentsAllErrors(errors).takeIf { it.isNotEmpty() }?.joinToString("\n")
}

/**
 * @return all error messages encountered during arguments parsing.
 */
fun validateArgumentsAllErrors(errors: ArgumentParseErrors?): List<String> {
    if (errors == null) return emptyList()
    return buildList {
        errors.argumentsWithoutValue.forEach {
            add("No value passed for argument $it")
        }
        errors.booleanArgumentsWithIncorrectValue.forEach { arg ->
            add("Incorrect value for boolean argument '${arg.substringBefore('=')}'. Only 'true' and 'false' are allowed.")
        }
        errors.booleanLangFeatureArgumentsWithValue.forEach { arg ->
            add(
                "No value is expected for argument '${arg.substringBefore('=')}'."
            )
        }
        errors.stringLangFeatureArgumentsWithIncorrectValue.forEach { argWithAllowedValued ->
            val (arg, allowedValues) = argWithAllowedValued
            val (argName, argValue) = arg.split('=')
            val allowedValuesString = allowedValues.joinToString(", ") { "'$it'" }
            add(
                "Incorrect value for argument '$argName'. " +
                        "Actual value: '$argValue', but allowed values: $allowedValuesString."
            )
        }
        errors.unknownArgs.forEach {
            add("Invalid argument: $it")
        }
    }
}

