/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlinx.cli

/**
 * Base interface for all possible types of options with multiple values.
 * Provides limitations for API that is accessible for options with several values.
 * Allows to save the way of providing several values in command line.
 *
 * @see [MultipleOption]
 */
interface MultipleOptionType {
    /**
     * Type of an option with multiple values allowed to be provided several times in command line.
     */
    class Repeated : MultipleOptionType

    /**
     * Type of an option with multiple values allowed to be provided using delimiter in one command line value.
     */
    class Delimited : MultipleOptionType

    /**
     * Type of an option with multiple values allowed to be provided several times in command line
     * both with specifying several values and with delimiter.
     */
    class RepeatedDelimited : MultipleOptionType
}

/**
 * The base class for command line options.
 *
 * You can use [ArgParser.option] function to declare an option.
 */
abstract class Option<TResult> internal constructor(delegate: ArgumentValueDelegate<TResult>,
                                                    owner: CLIEntityWrapper) : CLIEntity<TResult>(delegate, owner)

/**
 * The base class of an option with a single value.
 *
 * A required option or an option with a default value is represented with the [SingleOption] inheritor.
 * An option having nullable value is represented with the [SingleNullableOption] inheritor.
 */
abstract class AbstractSingleOption<T : Any, TResult, DefaultRequired: DefaultRequiredType> internal constructor(
    delegate: ArgumentValueDelegate<TResult>,
    owner: CLIEntityWrapper) :
    Option<TResult>(delegate, owner) {
    /**
     * Check descriptor for this kind of option.
     */
    internal fun checkDescriptor(descriptor: OptionDescriptor<*, *>) {
        if (descriptor.multiple || descriptor.delimiter != null) {
            failAssertion("Option with single value can't be initialized with descriptor for multiple values.")
        }
    }
}

/**
 * A required option or an option with a default value.
 *
 * The [value] of such option is non-null.
 */
class SingleOption<T : Any, DefaultType: DefaultRequiredType> internal constructor(descriptor: OptionDescriptor<T, T>,
                                                                                   owner: CLIEntityWrapper) :
    AbstractSingleOption<T, T, DefaultRequiredType>(ArgumentSingleValue(descriptor), owner) {
    init {
        checkDescriptor(descriptor)
    }
}

/**
 * An option with nullable [value].
 */
class SingleNullableOption<T : Any> internal constructor(descriptor: OptionDescriptor<T, T>, owner: CLIEntityWrapper) :
    AbstractSingleOption<T, T?, DefaultRequiredType.None>(ArgumentSingleNullableValue(descriptor), owner) {
    init {
        checkDescriptor(descriptor)
    }
}

/**
 * An option that allows several values to be provided in command line string.
 *
 * The [value] property of such option has type `List<T>`.
 */
class MultipleOption<T : Any, OptionType : MultipleOptionType, DefaultType: DefaultRequiredType> internal constructor(
    descriptor: OptionDescriptor<T, List<T>>,
    owner: CLIEntityWrapper
) :
    Option<List<T>>( ArgumentMultipleValues(descriptor), owner) {
    init {
        if (!descriptor.multiple && descriptor.delimiter == null) {
            failAssertion("Option with multiple values can't be initialized with descriptor for single one.")
        }
    }
}

/**
 * Allows the option to have several values specified in command line string.
 * Number of values is unlimited.
 */
fun <T : Any, TResult, DefaultType: DefaultRequiredType> AbstractSingleOption<T, TResult, DefaultType>.multiple():
        MultipleOption<T, MultipleOptionType.Repeated, DefaultType> {
    val newOption = with(delegate.cast<ParsingValue<T, T>>().descriptor as OptionDescriptor) {
        MultipleOption<T, MultipleOptionType.Repeated, DefaultType>(
            OptionDescriptor(
                optionFullFormPrefix, optionShortFromPrefix, type, fullName, shortName,
                description, listOfNotNull(defaultValue),
                required, true, delimiter, deprecatedWarning
            ), owner
        )
    }
    owner.entity = newOption
    return newOption
}

/**
 * Allows the option to have several values specified in command line string.
 * Number of values is unlimited.
 */
fun <T : Any, DefaultType: DefaultRequiredType> MultipleOption<T, MultipleOptionType.Delimited, DefaultType>.multiple():
        MultipleOption<T, MultipleOptionType.RepeatedDelimited, DefaultRequiredType> {
    val newOption = with(delegate.cast<ParsingValue<T, List<T>>>().descriptor as OptionDescriptor) {
        if (multiple) {
            error("Try to use modifier multiple() twice on option ${fullName ?: ""}")
        }
        MultipleOption<T, MultipleOptionType.RepeatedDelimited, DefaultRequiredType>(
            OptionDescriptor(
                optionFullFormPrefix, optionShortFromPrefix, type, fullName, shortName,
                description, defaultValue?.toList() ?: listOf(),
                required, true, delimiter, deprecatedWarning
            ), owner
        )
    }
    owner.entity = newOption
    return newOption
}

/**
 * Specifies the default value for the option, that will be used when no value is provided for it
 * in command line string.
 *
 * @param value the default value.
 */
fun <T : Any> SingleNullableOption<T>.default(value: T): SingleOption<T, DefaultRequiredType.Default> {
    val newOption = with(delegate.cast<ParsingValue<T, T>>().descriptor as OptionDescriptor) {
        SingleOption<T, DefaultRequiredType.Default>(
            OptionDescriptor(
                optionFullFormPrefix, optionShortFromPrefix, type, fullName, shortName,
                description, value, required, multiple, delimiter, deprecatedWarning
            ), owner
        )
    }
    owner.entity = newOption
    return newOption
}

/**
 * Specifies the default value for the option with multiple values, that will be used when no values are provided
 * for it in command line string.
 *
 * @param value the default value, must be a non-empty collection.
 * @throws IllegalArgumentException if provided default value is empty collection.
 */
fun <T : Any, OptionType : MultipleOptionType>
        MultipleOption<T, OptionType, DefaultRequiredType.None>.default(value: Collection<T>):
        MultipleOption<T, OptionType, DefaultRequiredType.Default> {
    val newOption = with(delegate.cast<ParsingValue<T, List<T>>>().descriptor as OptionDescriptor) {
        require(value.isNotEmpty()) { "Default value for option can't be empty collection." }
        MultipleOption<T, OptionType, DefaultRequiredType.Default>(
            OptionDescriptor(
                optionFullFormPrefix, optionShortFromPrefix, type, fullName,
                shortName, description, value.toList(),
                required, multiple, delimiter, deprecatedWarning
            ), owner
        )
    }
    owner.entity = newOption
    return newOption
}

/**
 * Requires the option to be always provided in command line.
 */
fun <T : Any> SingleNullableOption<T>.required(): SingleOption<T, DefaultRequiredType.Required> {
    val newOption = with(delegate.cast<ParsingValue<T, T>>().descriptor as OptionDescriptor) {
        SingleOption<T, DefaultRequiredType.Required>(
            OptionDescriptor(
                optionFullFormPrefix, optionShortFromPrefix, type, fullName,
                shortName, description, defaultValue,
                true, multiple, delimiter, deprecatedWarning
            ), owner
        )
    }
    owner.entity = newOption
    return newOption
}

/**
 * Requires the option to be always provided in command line.
 */
fun <T : Any, OptionType : MultipleOptionType>
        MultipleOption<T, OptionType, DefaultRequiredType.None>.required():
        MultipleOption<T, OptionType, DefaultRequiredType.Required> {
    val newOption = with(delegate.cast<ParsingValue<T, List<T>>>().descriptor as OptionDescriptor) {
        MultipleOption<T, OptionType, DefaultRequiredType.Required>(
            OptionDescriptor(
                optionFullFormPrefix, optionShortFromPrefix, type, fullName, shortName,
                description, defaultValue?.toList() ?: listOf(),
                true, multiple, delimiter, deprecatedWarning
            ), owner
        )
    }
    owner.entity = newOption
    return newOption
}

/**
 * Allows the option to have several values joined with [delimiter] specified in command line string.
 * Number of values is unlimited.
 *
 * The value of the argument is an empty list in case if no value was specified in command line string.
 *
 * @param delimiterValue delimiter used to separate string value to option values list.
 */
fun <T : Any, DefaultRequired: DefaultRequiredType> AbstractSingleOption<T, *, DefaultRequired>.delimiter(
    delimiterValue: String):
        MultipleOption<T, MultipleOptionType.Delimited, DefaultRequired> {
    val newOption = with(delegate.cast<ParsingValue<T, T>>().descriptor as OptionDescriptor) {
        MultipleOption<T, MultipleOptionType.Delimited, DefaultRequired>(
            OptionDescriptor(
                optionFullFormPrefix, optionShortFromPrefix, type, fullName, shortName,
                description, listOfNotNull(defaultValue),
                required, multiple, delimiterValue, deprecatedWarning
            ), owner
        )
    }
    owner.entity = newOption
    return newOption
}

/**
 * Allows the option to have several values joined with [delimiter] specified in command line string.
 * Number of values is unlimited.
 *
 * The value of the argument is an empty list in case if no value was specified in command line string.
 *
 * @param delimiterValue delimiter used to separate string value to option values list.
 */
fun <T : Any, DefaultRequired: DefaultRequiredType> MultipleOption<T, MultipleOptionType.Repeated, DefaultRequired>.delimiter(
    delimiterValue: String):
        MultipleOption<T, MultipleOptionType.RepeatedDelimited, DefaultRequired> {
    val newOption = with(delegate.cast<ParsingValue<T, List<T>>>().descriptor as OptionDescriptor) {
        MultipleOption<T, MultipleOptionType.RepeatedDelimited, DefaultRequired>(
            OptionDescriptor(
                optionFullFormPrefix, optionShortFromPrefix, type, fullName, shortName,
                description, defaultValue?.toList() ?: listOf(),
                required, multiple, delimiterValue, deprecatedWarning
            ), owner
        )
    }
    owner.entity = newOption
    return newOption
}