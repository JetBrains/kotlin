/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlinx.cli

import kotlin.reflect.KProperty

internal data class CLIEntityWrapper(var entity: CLIEntity<*>? = null)

/**
 * Command line entity.
 *
 * @property owner parser which owns current entity.
 */
abstract class CLIEntity<TResult> internal constructor(internal val owner: CLIEntityWrapper) {
    /**
     * Wrapper  for element - read only property.
     * Needed to close set of variable [cliElement].
     */
    lateinit var delegate: ArgumentValueDelegate<TResult>
        internal set

    /**
     * Value of entity.
     */
    var value: TResult
        get() = delegate.value
        set(value) { delegate.value = value }

    /**
     * Origin of argument value.
     */
    val valueOrigin: ArgParser.ValueOrigin
        get() = (delegate as ParsingValue<*, *>).valueOrigin

    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<TResult> {
        (delegate as ParsingValue<*, *>).provideName(prop.name)
        return delegate
    }
}

/**
 * Argument instance.
 */
abstract class Argument<TResult> internal constructor(owner: CLIEntityWrapper): CLIEntity<TResult>(owner)

/**
 * Common single argument instance.
 */
abstract class AbstractSingleArgument<T: Any, TResult> internal constructor(owner: CLIEntityWrapper): Argument<TResult>(owner) {
    /**
     * Check descriptor for this kind of argument.
     */
    internal fun checkDescriptor(descriptor: ArgDescriptor<*, *>) {
        if (descriptor.number == null || descriptor.number > 1) {
            failAssertion("Argument with single value can't be initialized with descriptor for multiple values.")
        }
    }
}

/**
 * Argument with single non-nullable value.
 */
class SingleArgument<T : Any> internal constructor(descriptor: ArgDescriptor<T, T>, owner: CLIEntityWrapper):
        AbstractSingleArgument<T, T>(owner) {
    init {
        checkDescriptor(descriptor)
        delegate = ArgumentSingleValue(descriptor)
    }
}

/**
 * Argument with single nullable value.
 */
class SingleNullableArgument<T : Any> internal constructor(descriptor: ArgDescriptor<T, T>, owner: CLIEntityWrapper):
        AbstractSingleArgument<T, T?>(owner){
    init {
        checkDescriptor(descriptor)
        delegate = ArgumentSingleNullableValue(descriptor)
    }
}

/**
 * Argument with multiple values.
 */
class MultipleArgument<T : Any> internal constructor(descriptor: ArgDescriptor<T, List<T>>, owner: CLIEntityWrapper):
        Argument<List<T>>(owner) {
    init {
        if (descriptor.number != null && descriptor.number < 2) {
            failAssertion("Argument with multiple values can't be initialized with descriptor for single one.")
        }
        delegate = ArgumentMultipleValues(descriptor)
    }
}

/**
 * Allow argument have several values.
 *
 * @param value number of arguments are expected.
 */
fun <T : Any, TResult> AbstractSingleArgument<T, TResult>.multiple(value: Int): MultipleArgument<T> {
    if (value < 2) {
        error("multiple() modifier with value less than 2 is unavailable. It's already set to 1.")
    }
    val newArgument = with((delegate as ParsingValue<T, T>).descriptor as ArgDescriptor) {
        MultipleArgument(ArgDescriptor(type, fullName, value, description, listOfNotNull(defaultValue),
                required, deprecatedWarning), owner)
    }
    owner.entity = newArgument
    return newArgument
}

/**
 * Allow argument have several values.
 */
fun <T : Any, TResult> AbstractSingleArgument<T, TResult>.vararg(): MultipleArgument<T> {
    val newArgument = with((delegate as ParsingValue<T, T>).descriptor as ArgDescriptor) {
        MultipleArgument(ArgDescriptor(type, fullName, null, description, listOfNotNull(defaultValue),
                required, deprecatedWarning), owner)
    }
    owner.entity = newArgument
    return newArgument
}

/**
 * Set default value for argument.
 *
 * @param value default value.
 */
fun <T: Any, TResult> AbstractSingleArgument<T, TResult>.default(value: T): SingleArgument<T> {
    val newArgument = with((delegate as ParsingValue<T, T>).descriptor as ArgDescriptor) {
        SingleArgument(ArgDescriptor(type, fullName, number, description, value, required, deprecatedWarning), owner)
    }
    owner.entity = newArgument
    return newArgument
}

/**
 * Set default value for argument.
 *
 * @param value default value.
 */
fun <T: Any> MultipleArgument<T>.default(value: Collection<T>): MultipleArgument<T> {
    if (value.isEmpty()) {
        error("Default value for argument can't be empty collection.")
    }
    val newArgument = with((delegate as ParsingValue<T, List<T>>).descriptor as ArgDescriptor) {
        MultipleArgument(ArgDescriptor(type, fullName, number, description, value.toList(),
                required, deprecatedWarning), owner)
    }
    owner.entity = newArgument
    return newArgument
}

/**
 * Allow argument be unprovided in command line.
 */
fun <T: Any> SingleArgument<T>.optional(): SingleNullableArgument<T> {
    val newArgument = with((delegate as ParsingValue<T, T>).descriptor as ArgDescriptor) {
        SingleNullableArgument(ArgDescriptor(type, fullName, number, description, defaultValue,
                false, deprecatedWarning), owner)
    }
    owner.entity = newArgument
    return newArgument
}

/**
 * Allow argument be unprovided in command line.
 */
fun <T: Any> MultipleArgument<T>.optional(): MultipleArgument<T> {
    val newArgument = with((delegate as ParsingValue<T, List<T>>).descriptor as ArgDescriptor) {
        MultipleArgument(ArgDescriptor(type, fullName, number, description,
                defaultValue?.toList() ?: listOf(), false, deprecatedWarning), owner)
    }
    owner.entity = newArgument
    return newArgument
}

fun failAssertion(message: String): Nothing = throw AssertionError(message)