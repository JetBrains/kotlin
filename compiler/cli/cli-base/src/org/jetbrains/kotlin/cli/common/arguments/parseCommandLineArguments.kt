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
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.java
import kotlin.reflect.KClass
import kotlin.reflect.cast

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
@Target(AnnotationTarget.FIELD)
annotation class Argument(
    val value: String,
    val shortName: String = "",
    val deprecatedName: String = "",
    @property:RawDelimiter
    val delimiter: String = Delimiters.default,
    val valueDescription: String = "",
    val description: String,
    val deprecatedVersion: String = "",
    val removedVersion: String = "",
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

val Argument.isAlreadyRemoved: Boolean
    get() = removedVersion.takeIf { it.isNotEmpty() }.let {
        it != null && parseKotlinVersion(removedVersion) <= KotlinVersion.CURRENT
    }

private const val ADVANCED_ARGUMENT_PREFIX = "-X"
internal const val INTERNAL_ARGUMENT_PREFIX = "-XX"
private const val FREE_ARGS_DELIMITER = "--"

inline fun <reified T : CommonToolArguments> parseCommandLineArguments(args: List<String>): T {
    return parseCommandLineArguments(T::class, args)
}

fun <T : CommonToolArguments> parseCommandLineArguments(clazz: KClass<T>, args: List<String>): T {
    val constructor = getArgumentsInfo(clazz.java).defaultArgsConstructor
    val arguments = clazz.cast(constructor?.newInstance() ?: error("Missing empty constructor on '${clazz.java.name}"))
    parseCommandLineArguments(args, arguments)
    return arguments
}


// Parses arguments into the passed [result] object. Errors related to the parsing will be collected into [CommonToolArguments.errors].
fun <A : CommonToolArguments> parseCommandLineArguments(args: List<String>, result: A, overrideArguments: Boolean = false) {
    val preprocessed = preprocessCommandLineArguments(args, result.diagnostics)
    parsePreprocessedCommandLineArguments(preprocessed, result, overrideArguments)
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
            val enablesAnnotations = field.getAnnotationsByType(Enables::class.java).toList()
            val disablesAnnotations = field.getAnnotationsByType(Disables::class.java).toList()
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
    overrideArguments: Boolean
) {
    val diagnostics = result.diagnostics

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
        val delimiterIndex = arg.indexOfFirst { it == ':' || it == '=' }

        val key: String
        val value: String? // `null` means no delimiter; empty means a delimiter with empty string afterwords
        if (delimiterIndex != -1) {
            key = arg.substring(0, delimiterIndex)
            value = arg.substring(delimiterIndex + 1)
        } else {
            key = arg
            value = null
        }

        var argumentField = properties[key]
        var removedArg = false

        if (argumentField == null) {
            argumentField = removedArguments.cliArgNameToArguments[key]
            // We still should parse a value of the removed argument to get rid of potential CLI parse error.
            removedArg = true
        }

        if (argumentField == null) {
            when {
                // Unknown -X argument
                key.startsWith(ADVANCED_ARGUMENT_PREFIX) -> diagnostics += ArgumentParseDiagnostic.UnknownExtraFlag(arg)
                key.startsWith('-') -> diagnostics += ArgumentParseDiagnostic.InvalidArgument(arg)
                else -> freeArgs.add(arg)
            }
            continue
        }

        val argument = argumentField.argument
        val getterReturnType = argumentField.getter.returnType.kotlin

        // Tests for -shortName=value, which isn't currently allowed.
        if (key != arg && key == argument.shortName) {
            diagnostics += ArgumentParseDiagnostic.InvalidArgument(arg)
            continue
        }

        val deprecatedName = argument.deprecatedName
        if (deprecatedName == key) {
            diagnostics += ArgumentParseDiagnostic.ArgumentWithDeprecatedName(deprecatedName, argument.value)
        }

        if (argument.value == arg) {
            if (argument.isAdvanced && getterReturnType != Boolean::class) {
                diagnostics += ArgumentParseDiagnostic.ExtraArgumentInObsoleteForm(arg)
            }
        }

        val existingValues by lazy(LazyThreadSafetyMode.NONE) { explicitArgs.getOrPut(argumentField) { mutableListOf() } }

        val newValue: Any = if (getterReturnType == Boolean::class) {
            parseBooleanValue(key, value, argumentField, diagnostics).also { existingValues.add(it) }
        } else {
            val stringValue = value ?: run {
                if (i == args.size) {
                    diagnostics += ArgumentParseDiagnostic.ArgumentWithoutValue(arg)
                    break@loop
                }
                args[i++]
            }

            if (argumentField.changesLanguageFeatures) {
                val allowedValues = buildSet {
                    argumentField.enablesAnnotations.forEach { add(it.ifValueIs) }
                    argumentField.disablesAnnotations.forEach { add(it.ifValueIs) }
                }
                if (!allowedValues.contains(stringValue)) {
                    diagnostics += ArgumentParseDiagnostic.StringLanguageFeatureArgumentWithIncorrectValue(key, stringValue, allowedValues)
                }
            }

            when (getterReturnType) {
                String::class -> stringValue.also { existingValues.add(it) }
                Array<String>::class -> convertArrayOfStrings(argumentField.argument, stringValue, overrideArguments, existingValues)
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
    result.explicitArguments = explicitArgs

    if (result is CommonCompilerArguments) {
        val internalArguments = ArrayList<ManualLanguageFeatureSetting>()
        for (arg in result.manuallyConfiguredFeatures) {
            val featureSetting = LanguageSettingsParser.parseLanguageFeature(arg, "-XXLanguage:$arg", diagnostics) ?: continue
            internalArguments.removeIf {
                it.languageFeature == featureSetting.languageFeature
            }
            internalArguments.add(featureSetting)
        }
        result.updateInternalArguments(internalArguments, overrideArguments)
    }
}

private fun parseBooleanValue(
    key: String,
    value: String?,
    argumentField: ArgumentField,
    diagnostics: MutableList<ArgumentParseDiagnostic>,
): Boolean {
    return if (value != null) {
        val changesLangFeatures = argumentField.changesLanguageFeatures
        when (value) {
            "true" -> true
            "false" -> false
            else -> true.also {
                if (!changesLangFeatures) {
                    diagnostics += ArgumentParseDiagnostic.BooleanArgumentWithIncorrectValue(key)
                }
            }
        }.also {
            if (changesLangFeatures) {
                diagnostics += ArgumentParseDiagnostic.BooleanLanguageFeatureArgumentWithValue(key)
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
    overrideArguments: Boolean
) {
    val filteredExistingArguments = if (overrideArguments) {
        internalArguments.filter { existingArgument ->
            newInternalArguments.none { it.languageFeature == existingArgument.languageFeature }
        }
    } else internalArguments

    internalArguments = filteredExistingArguments + newInternalArguments
}
