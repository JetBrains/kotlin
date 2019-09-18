/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlinx.cli

import kotlin.reflect.KProperty

/**
 * Base interface for all possible types of options with multiple values.
 */
interface MultipleOptionType

/**
 * Type of option with multiple values that can be provided several times in command line.
 */
class RepeatedOption: MultipleOptionType

/**
 * Type of option with multiple values that are provided using delimiter.
 */
class DelimitedOption: MultipleOptionType

/**
 * Type of option with multiple values that can be both provided several times in command line and using delimiter.
 */
class RepeatedDelimitedOption: MultipleOptionType

/**
 * Option instance.
 */
abstract class Option<TResult> internal constructor(owner: CLIEntityWrapper): CLIEntity<TResult>(owner)

/**
 * Common single option instance.
 */
abstract class AbstractSingleOption<T: Any, TResult> internal constructor(owner: CLIEntityWrapper): Option<TResult>(owner) {
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
 * Option wit single non-nullable value.
 */
class SingleOption<T : Any> internal constructor(descriptor: OptionDescriptor<T, T>, owner: CLIEntityWrapper):
        AbstractSingleOption<T, T>(owner) {
    init {
        checkDescriptor(descriptor)
        delegate = ArgumentSingleValue(descriptor)
    }
}

/**
 * Option with single nullable value.
 */
class SingleNullableOption<T : Any> internal constructor(descriptor: OptionDescriptor<T, T>, owner: CLIEntityWrapper):
        AbstractSingleOption<T, T?>(owner) {
    init {
        checkDescriptor(descriptor)
        delegate = ArgumentSingleNullableValue(descriptor)
    }
}

/**
 * Option with multiple values.
 */
class MultipleOption<T : Any, OptionType: MultipleOptionType> internal constructor(descriptor: OptionDescriptor<T, List<T>>, owner: CLIEntityWrapper):
        Option<List<T>>(owner) {
    init {
        if (!descriptor.multiple && descriptor.delimiter == null) {
            failAssertion("Option with multiple values can't be initialized with descriptor for single one.")
        }
        delegate = ArgumentMultipleValues(descriptor)
    }
}

/**
 * Allow option have several values.
 */
fun <T : Any, TResult> AbstractSingleOption<T, TResult>.multiple(): MultipleOption<T, RepeatedOption> {
    val newOption = with((delegate as ParsingValue<T, T>).descriptor as OptionDescriptor) {
        MultipleOption<T, RepeatedOption>(OptionDescriptor(optionFullFormPrefix, optionShortFromPrefix, type, fullName, shortName,
                description, listOfNotNull(defaultValue),
                required, true, delimiter, deprecatedWarning), owner)
    }
    owner.entity = newOption
    return newOption
}

/**
 * Allow option have several values.
 */
fun <T : Any> MultipleOption<T, DelimitedOption>.multiple(): MultipleOption<T, RepeatedDelimitedOption> {
    val newOption = with((delegate as ParsingValue<T, List<T>>).descriptor as OptionDescriptor) {
        if (multiple) {
            error("Try to use modifier multiple() twice on option ${fullName ?: ""}")
        }
        MultipleOption<T, RepeatedDelimitedOption>(OptionDescriptor(optionFullFormPrefix, optionShortFromPrefix, type, fullName, shortName,
                description, defaultValue?.toList() ?: listOf(),
                required, true, delimiter, deprecatedWarning), owner)
    }
    owner.entity = newOption
    return newOption
}

/**
 * Set default option value.
 *
 * @param value default value.
 */
fun <T: Any, TResult> AbstractSingleOption<T, TResult>.default(value: T): SingleOption<T> {
    val newOption = with((delegate as ParsingValue<T, T>).descriptor as OptionDescriptor) {
        SingleOption(OptionDescriptor(optionFullFormPrefix, optionShortFromPrefix, type, fullName, shortName,
                description, value, required, multiple, delimiter, deprecatedWarning), owner)
    }
    owner.entity = newOption
    return newOption
}

/**
 * Set default option value.
 *
 * @param value default value.
 */
fun <T: Any, OptionType: MultipleOptionType>
        MultipleOption<T, OptionType>.default(value: Collection<T>): MultipleOption<T, OptionType> {
    val newOption = with((delegate as ParsingValue<T, List<T>>).descriptor as OptionDescriptor) {
        if (value.isEmpty()) {
            error("Default value for option can't be empty collection.")
        }
        MultipleOption<T, OptionType>(OptionDescriptor(optionFullFormPrefix, optionShortFromPrefix, type, fullName,
                shortName, description, value.toList(),
                required, multiple, delimiter, deprecatedWarning), owner)
    }
    owner.entity = newOption
    return newOption
}

/**
 * Require option to be always provided in command line.
 */
fun <T: Any> SingleNullableOption<T>.required(): SingleOption<T> {
    val newOption = with((delegate as ParsingValue<T, T>).descriptor as OptionDescriptor) {
        SingleOption(OptionDescriptor(optionFullFormPrefix, optionShortFromPrefix, type, fullName,
                shortName, description, defaultValue,
                true, multiple, delimiter, deprecatedWarning), owner)
    }
    owner.entity = newOption
    return newOption
}

/**
 * Require option to be always provided in command line.
 */
fun <T: Any, OptionType: MultipleOptionType>
        MultipleOption<T, OptionType>.required(): MultipleOption<T, OptionType> {
    val newOption = with((delegate as ParsingValue<T, List<T>>).descriptor as OptionDescriptor) {
        MultipleOption<T, OptionType>(OptionDescriptor(optionFullFormPrefix, optionShortFromPrefix, type, fullName, shortName,
                description, defaultValue?.toList() ?: listOf(),
                true, multiple, delimiter, deprecatedWarning), owner)
    }
    owner.entity = newOption
    return newOption
}

/**
 * Allow provide several options using [delimiter].
 *
 * @param delimiterValue delimiter used to separate string value to option values.
 */
fun <T : Any, TResult> AbstractSingleOption<T, TResult>.delimiter(delimiterValue: String): MultipleOption<T, DelimitedOption> {
    val newOption = with((delegate as ParsingValue<T, T>).descriptor as OptionDescriptor) {
        MultipleOption<T, DelimitedOption>(OptionDescriptor(optionFullFormPrefix, optionShortFromPrefix, type, fullName, shortName,
                description, listOfNotNull(defaultValue),
                required, multiple, delimiterValue, deprecatedWarning), owner)
    }
    owner.entity = newOption
    return newOption
}

/**
 * Allow provide several options using [delimiter].
 *
 * @param delimiterValue delimiter used to separate string value to option values.
 */
fun <T : Any> MultipleOption<T, RepeatedOption>.delimiter(delimiterValue: String): MultipleOption<T, RepeatedDelimitedOption> {
    val newOption = with((delegate as ParsingValue<T, List<T>>).descriptor as OptionDescriptor) {
        MultipleOption<T, RepeatedDelimitedOption>(OptionDescriptor(optionFullFormPrefix, optionShortFromPrefix, type, fullName, shortName,
                description, defaultValue?.toList() ?: listOf(),
                required, multiple, delimiterValue, deprecatedWarning), owner)
    }
    owner.entity = newOption
    return newOption
}