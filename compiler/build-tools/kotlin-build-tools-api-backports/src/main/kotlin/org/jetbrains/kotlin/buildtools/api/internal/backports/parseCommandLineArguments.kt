/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.internal.backports

import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.Disables
import org.jetbrains.kotlin.cli.common.arguments.Enables
import org.jetbrains.kotlin.cli.common.arguments.ManualLanguageFeatureSetting
import org.jetbrains.kotlin.cli.common.arguments.RemovedCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * An annotation used to define metadata for a field that represents an argument in a command-line interface.
 * This annotation allows specification of argument-related properties, such as its name, description, and delimiter.
 *
 * @property value The primary name of the argument. This field is mandatory and represents the identifier
 * for the argument in input parsing.
 *
 * @property shortName An optional shorthand name for the argument, typically prefixed with a single dash.
 * If not provided, no shorthand identifier will be associated with the argument.
 *
 * @property deprecatedName An optional, previously used name for the argument. Useful for maintaining backward
 * compatibility when migrating to a new name.
 *
 * @property delimiter The delimiter used to parse values that represent collections or lists. This property
 * must use predefined constants from the [Delimiters] object. Use of the raw value for this property requires
 * opt-in via the [RawDelimiter] annotation.
 *
 * @property valueDescription An optional description of the expected format or type of the argument value. Helps
 * provide guidance to users about how the argument should be used.
 *
 * @property description A human-readable explanation of the purpose of this argument. This provides details
 * about what the argument is and when it should be used.
 *
 * @property deprecatedVersion Specifies the version in which this argument was marked as deprecated. If empty,
 * the argument has not been marked as deprecated.
 *
 * @property removedVersion Specifies the version in which this argument was removed. If empty, the argument
 * has not been scheduled or marked for removal.
 */
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

val Argument.isAlreadyRemoved: Boolean
    get() = removedVersion.takeIf { it.isNotEmpty() }.let {
        it != null && parseKotlinVersion(removedVersion) <= KotlinVersion.CURRENT
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
    val internalArgumentsParsingProblems: MutableList<Pair<CompilerMessageSeverity, String>> = SmartList(),
)

// Parses arguments into the passed [result] object. Errors related to the parsing will be returned from this function.
fun <A : CommonToolArguments> parseCommandLineArguments(
    args: List<String>,
    result: A,
    overrideArguments: Boolean = false,
): ArgumentParseErrors {
    val errors = ArgumentParseErrors()
    val preprocessed = preprocessCommandLineArguments(args, errors)
    parsePreprocessedCommandLineArguments(preprocessed, result, errors, overrideArguments)
    return errors
}

fun <A : CommonToolArguments> parseCommandLineArgumentsFromEnvironment(arguments: A) {
    val settingsFromEnvironment = CompilerSystemProperties.LANGUAGE_VERSION_SETTINGS.value?.takeIf { it.isNotEmpty() }
        ?.split(Regex("""\s"""))
        ?.filterNot { it.isBlank() }
        ?: return
    parseCommandLineArguments(settingsFromEnvironment, arguments, overrideArguments = true)
}

private val argumentsCache = ConcurrentHashMap<Class<*>, ArgumentsInfo>()
private val removedArguments: ArgumentsInfo by lazy(LazyThreadSafetyMode.PUBLICATION) {
    extractArgumentsInfo(@Suppress("DEPRECATION_ERROR") RemovedCompilerArguments::class.java)
}

enum class ArgumentLifecycleStatus {
    REGULAR,
    WILL_BE_DEPRECATED,
    WILL_BE_REMOVED,
    DEPRECATED,
    DEPRECATED_AND_WILL_BE_REMOVED,
    REMOVED,
}

data class ArgumentField(
    val getter: Method,
    val setter: Method,
    val argument: Argument,
    val enablesAnnotations: List<Enables>,
    val disablesAnnotations: List<Disables>,
    val deprecatedAnnotation: Deprecated?,
) {
    val changesLanguageFeatures: Boolean
        get() = enablesAnnotations.isNotEmpty() || disablesAnnotations.isNotEmpty()

    val status: ArgumentLifecycleStatus
        get() {
            val removedVersion = argument.removedVersion
            val deprecatedVersion = argument.deprecatedVersion

            if (removedVersion.isNotEmpty()) {
                return when {
                    parseKotlinVersion(removedVersion) <= KotlinVersion.CURRENT -> {
                        ArgumentLifecycleStatus.REMOVED
                    }
                    deprecatedVersion.isEmpty() || deprecatedVersion == removedVersion -> {
                        ArgumentLifecycleStatus.WILL_BE_REMOVED
                    }
                    else -> {
                        ArgumentLifecycleStatus.DEPRECATED_AND_WILL_BE_REMOVED
                    }
                }
            }

            if (deprecatedVersion.isNotEmpty()) {
                return when {
                    parseKotlinVersion(deprecatedVersion) <= KotlinVersion.CURRENT -> {
                        ArgumentLifecycleStatus.DEPRECATED
                    }
                    else -> {
                        ArgumentLifecycleStatus.WILL_BE_DEPRECATED
                    }
                }
            }

            return ArgumentLifecycleStatus.REGULAR
        }
}

data class ArgumentsInfo(
    val cliArgNameToArguments: Map<String, ArgumentField>,
    val defaultArgsConstructor: Constructor<*>?,
) {
    private val defaultArgs: CommonToolArguments by lazy(LazyThreadSafetyMode.PUBLICATION) {
        defaultArgsConstructor?.newInstance() as? CommonToolArguments ?: error("Missing empty constructor")
    }

    fun getDefaultValue(argumentField: ArgumentField): Any? = argumentField.getter.invoke(defaultArgs)
}

fun getArgumentsInfo(klass: Class<*>): ArgumentsInfo {
    require(CommonToolArguments::class.java.isAssignableFrom(klass))
    return argumentsCache.getOrPut(klass) {
        extractArgumentsInfo(klass)
    }
}

private fun extractArgumentsInfo(klass: Class<*>): ArgumentsInfo = ArgumentsInfo(
    cliArgNameToArguments = buildMap {
        for (field in klass.declaredFields) {
            val argument = field.getAnnotation(Argument::class.java) ?: continue
            val enablesAnnotations = try {
                field.getAnnotationsByType(Enables::class.java).toList()
            } catch (_: Throwable) {
                emptyList()
            }
            val disablesAnnotations = try {
                field.getAnnotationsByType(Disables::class.java).toList()
            } catch (_: Throwable) {
                emptyList()
            }
            val getter = klass.getMethod(JvmAbi.getterName(field.name))
            val setter = klass.getMethod(JvmAbi.setterName(field.name), field.type)
            val deprecatedAnnotation =
                getter.getAnnotation(Deprecated::class.java) // Check the getter because `@Deprecated` doesn't have `FIELD` target
            val argumentField =
                ArgumentField(getter, setter, argument, enablesAnnotations, disablesAnnotations, deprecatedAnnotation)
            for (key in listOf(argument.value, argument.shortName, argument.deprecatedName)) {
                if (key.isNotEmpty()) put(key, argumentField)
            }
        }
        val superclass = klass.superclass
        if (CommonToolArguments::class.java.isAssignableFrom(superclass)) {
            putAll(extractArgumentsInfo(superclass).cliArgNameToArguments)
        }
    },
    defaultArgsConstructor = klass.constructors.find { it.parameters.isEmpty() },
)

private fun <A : CommonToolArguments> parsePreprocessedCommandLineArguments(
    args: List<String>,
    result: A,
    errors: ArgumentParseErrors,
    overrideArguments: Boolean,
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
        var argumentField = properties[key]
        var removedArg = false

        if (argumentField == null) {
            try {
                argumentField = removedArguments.cliArgNameToArguments[key]
                // We still should parse a value of the removed argument to get rid of potential CLI parse error.
                removedArg = true
            } catch (_: Throwable) {
                // before 2.4.20 there was no removedArguments
            }
        }

        if (argumentField == null) {
            when {
                // Unknown -X argument
                arg.startsWith(ADVANCED_ARGUMENT_PREFIX) -> errors.unknownExtraFlags.add(arg)
                arg.startsWith("-") -> errors.unknownArgs.add(arg)
                else -> freeArgs.add(arg)
            }
            continue
        }

        val argument = argumentField.argument
        val getterReturnType = argumentField.getter.returnType.kotlin

        // Tests for -shortName=value, which isn't currently allowed.
        if (key != arg && key == argument.shortName) {
            errors.unknownArgs.add(arg)
            continue
        }

        val deprecatedName = argument.deprecatedName
        if (deprecatedName == key) {
            errors.deprecatedArguments[deprecatedName] = argument.value
        }

        if (argument.value == arg) {
            if (argument.isAdvanced && getterReturnType != Boolean::class) {
                errors.extraArgumentsPassedInObsoleteForm.add(arg)
            }
        }

        val existingValues by lazy(LazyThreadSafetyMode.NONE) { explicitArgs.getOrPut(argumentField) { mutableListOf() } }

        val newValue: Any = if (getterReturnType == Boolean::class) {
            parseBooleanValue(arg, argumentField, delimiter, errors).also { existingValues.add(it) }
        } else {
            val argument1 = argumentField.argument
            val stringValue: String = when {
                arg.startsWith(argument1.value + delimiter) -> {
                    val legalValues = buildSet {
                        argumentField.enablesAnnotations.forEach { add(it.ifValueIs) }
                        argumentField.disablesAnnotations.forEach { add(it.ifValueIs) }
                    }
                    arg.substring(argument1.value.length + 1).also {
                        if (legalValues.isNotEmpty() && !legalValues.contains(it)) {
                            errors.stringLangFeatureArgumentsWithIncorrectValue.add(arg to legalValues)
                        }
                    }
                }
                arg.startsWith(argument1.deprecatedName + delimiter) -> {
                    arg.substring(argument1.deprecatedName.length + 1)
                }
                i == args.size -> {
                    errors.argumentsWithoutValue.add(arg)
                    break@loop
                }
                else -> {
                    args[i++]
                }
            }

            when (getterReturnType) {
                String::class -> stringValue.also { existingValues.add(it) }
                Array<String>::class -> convertArrayOfStrings(argument1, stringValue, overrideArguments, existingValues)
                else -> error("Unexpected argument type: $getterReturnType")
            }
        }

        if (!removedArg) {
            // We can't set the value if the argument is removed because object types are incompatible.
            // Moreover, the object for removed args doesn't even exist.
            argumentField.setter(result, newValue)
        }
    }

    result.freeArgs += freeArgs

    if (result is CommonCompilerArguments) {
        val internalArguments = ArrayList<ManualLanguageFeatureSetting>()
        try {
            for (arg in result.manuallyConfiguredFeatures.orEmpty()) {
                val featureSetting = LanguageSettingsParser.parseLanguageFeature(arg, "-XXLanguage:$arg", errors) ?: continue
                internalArguments.removeIf {
                    it.languageFeature == featureSetting.languageFeature
                }
                internalArguments.add(featureSetting)
            }
        } catch (_: Throwable) {
            // 2.2.21 doesn't have result.manuallyConfiguredFeatures
        }
        result.updateInternalArguments(internalArguments, overrideArguments)
    }
}

private fun parseBooleanValue(
    arg: String,
    argumentField: ArgumentField,
    delimiter: Char,
    errors: ArgumentParseErrors,
): Boolean {
    val argumentValue = argumentField.argument.value
    return if (arg.startsWith(argumentValue + delimiter)) {
        val changesLangFeatures = argumentField.changesLanguageFeatures
        when (arg.substring(argumentValue.length + 1)) {
            "true" -> true
            "false" -> false
            else -> true.also {
                if (!changesLangFeatures) {
                    errors.booleanArgumentsWithIncorrectValue.add(arg)
                }
            }
        }.also {
            if (changesLangFeatures) {
                errors.booleanLangFeatureArgumentsWithValue.add(arg)
            }
        }
    } else {
        true
    }
}

private fun convertArrayOfStrings(
    argument: Argument,
    stringValue: String,
    overrideArguments: Boolean,
    existingValues: MutableList<Any>,
): Array<String> {
    val resolvedDelimiter = argument.resolvedDelimiter

    val newElements: List<String> = if (resolvedDelimiter.isNullOrEmpty()) {
        listOf(stringValue)
    } else {
        stringValue.split(resolvedDelimiter)
    }

    val oldValue: MutableList<String>? = if (!overrideArguments) {
        existingValues.firstIsInstanceOrNull<MutableList<String>>()
    } else {
        null
    }

    val resultElements: MutableList<String> = oldValue?.also { it.addAll(newElements) }
        ?: newElements.toMutableList().also { existingValues.add(it) }

    return resultElements.toTypedArray()
}

private fun <A : CommonToolArguments> A.updateInternalArguments(
    newInternalArguments: ArrayList<ManualLanguageFeatureSetting>,
    overrideArguments: Boolean,
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
            val [arg, allowedValues] = argWithAllowedValued
            val [argName, argValue] = arg.split('=')
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

private fun parseKotlinVersion(kotlinReleaseVersion: String): KotlinVersion {
    val components = kotlinReleaseVersion.split('.')
    return KotlinVersion(
        major = components[0].toInt(),
        minor = components[1].toInt(),
        patch = components[2].toInt(),
    )
}
