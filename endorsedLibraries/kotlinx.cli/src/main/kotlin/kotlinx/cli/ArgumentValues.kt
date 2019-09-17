/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlinx.cli

import kotlin.reflect.KProperty

/**
 * Parsing value of option/argument.
 */
internal abstract class ParsingValue<T: Any, TResult: Any>(val descriptor: Descriptor<T, TResult>) {
    /**
     * Values of arguments.
     */
    protected lateinit var parsedValue: TResult

    /**
     * Value origin.
     */
    var valueOrigin = ArgParser.ValueOrigin.UNSET
        protected set

    /**
     * Check if values of argument are empty.
     */
    abstract fun isEmpty(): Boolean

    /**
     * Check if value of argument was initialized.
     */
    protected fun valueIsInitialized() = ::parsedValue.isInitialized

    /**
     * Sace value from command line.
     *
     * @param stringValue value from command line.
     */
    protected abstract fun saveValue(stringValue: String)

    /**
     * Set value of delegated property.
     */
    fun setDelegatedValue(providedValue: TResult) {
        parsedValue = providedValue
        valueOrigin = ArgParser.ValueOrigin.REDEFINED
    }

    /**
     * Add parsed value from command line.
     *
     * @param stringValue value from command line.
     */
    internal fun addValue(stringValue: String) {
        // Check of possibility to set several values to one option/argument.
        if (descriptor is OptionDescriptor<*, *> && !descriptor.multiple &&
                !isEmpty() && descriptor.delimiter == null) {
            throw ParsingException("Try to provide more than one value for ${descriptor.fullName}.")
        }
        // Show deprecated warning only first time of using option/argument.
        descriptor.deprecatedWarning?.let {
            if (isEmpty())
                println ("Warning: $it")
        }
        // Split value if needed.
        if (descriptor is OptionDescriptor<*, *> && descriptor.delimiter != null) {
            stringValue.split(descriptor.delimiter).forEach {
                saveValue(it)
            }
        } else {
            saveValue(stringValue)
        }
    }

    /**
     * Set default value to option.
     */
    fun addDefaultValue() {
        if (!descriptor.defaultValueSet && descriptor.required) {
            throw ParsingException("Please, provide value for ${descriptor.textDescription}. It should be always set.")
        }
        if (descriptor.defaultValueSet) {
            parsedValue = descriptor.defaultValue!!
            valueOrigin = ArgParser.ValueOrigin.SET_DEFAULT_VALUE
        }
    }

    /**
     * Provide name for CLI entity.
     *
     * @param name name for CLI entity.
     */
    fun provideName(name: String) {
        descriptor.fullName ?: run { descriptor.fullName = name }
    }
}

/**
 * Single argument value.
 *
 * @property descriptor descriptor of option/argument.
 */
internal abstract class AbstractArgumentSingleValue<T: Any>(descriptor: Descriptor<T, T>):
        ParsingValue<T, T>(descriptor) {

    override fun saveValue(stringValue: String) {
        if (!valueIsInitialized()) {
            parsedValue = descriptor.type.conversion(stringValue, descriptor.fullName!!)
            valueOrigin = ArgParser.ValueOrigin.SET_BY_USER
        } else {
            throw ParsingException("Try to provide more than one value $parsedValue and $stringValue for ${descriptor.fullName}.")
        }
    }

    override fun isEmpty(): Boolean = !valueIsInitialized()
}

/**
 * Single argument value.
 *
 * @property descriptor descriptor of option/argument.
 */
internal class ArgumentSingleValue<T: Any>(descriptor: Descriptor<T, T>): AbstractArgumentSingleValue<T>(descriptor),
        ArgumentValueDelegate<T> {
    override var value: T
        get() = if (!isEmpty()) parsedValue else error("Value for argument ${descriptor.fullName} isn't set. " +
                "ArgParser.parse(...) method should be called before.")
        set(value) = setDelegatedValue(value)
}

/**
 * Single nullable argument value.
 *
 * @property descriptor descriptor of option/argument.
 */
internal class ArgumentSingleNullableValue<T : Any>(descriptor: Descriptor<T, T>):
        AbstractArgumentSingleValue<T>(descriptor), ArgumentValueDelegate<T?> {
    private var setToNull = false
    override var value: T?
        get() = if (!isEmpty() && !setToNull) parsedValue else null
        set(providedValue) = providedValue?.let {
                setDelegatedValue(it)
                setToNull = false
            } ?: run {
                setToNull = true
                valueOrigin = ArgParser.ValueOrigin.REDEFINED
            }
}

/**
 * Multiple argument values.
 *
 * @property descriptor descriptor of option/argument.
 */
internal class ArgumentMultipleValues<T : Any>(descriptor: Descriptor<T, List<T>>):
        ParsingValue<T, List<T>>(descriptor), ArgumentValueDelegate<List<T>> {

    private val addedValue = mutableListOf<T>()
    init {
        parsedValue = addedValue
    }

    override var value: List<T>
        get() = parsedValue
        set(value) = setDelegatedValue(value)

    override fun saveValue(stringValue: String) {
        addedValue.add(descriptor.type.conversion(stringValue, descriptor.fullName!!))
        valueOrigin = ArgParser.ValueOrigin.SET_BY_USER
    }

    override fun isEmpty() = parsedValue.isEmpty()
}