/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlinx.cli

import kotlin.reflect.KProperty

internal expect fun exitProcess(status: Int): Nothing

/**
 * Queue of arguments descriptors.
 * Arguments can have several values, so one descriptor can be returned several times.
 */
internal class ArgumentsQueue(argumentsDescriptors: List<ArgDescriptor<*, *>>) {
    /**
     * Map of arguments descriptors and their current usage number.
     */
    private val argumentsUsageNumber = linkedMapOf(*argumentsDescriptors.map { it to 0 }.toTypedArray())

    /**
     * Get next descriptor from queue.
     */
    fun pop(): String? {
        if (argumentsUsageNumber.isEmpty())
            return null

        val (currentDescriptor, usageNumber) = argumentsUsageNumber.iterator().next()
        currentDescriptor.number?.let {
            // Parse all arguments for current argument description.
            if (usageNumber + 1 >= currentDescriptor.number) {
                // All needed arguments were provided.
                argumentsUsageNumber.remove(currentDescriptor)
            } else {
                argumentsUsageNumber[currentDescriptor] = usageNumber + 1
            }
        }
        return currentDescriptor.fullName
    }
}

/**
 * Interface of argument value.
 */
interface ArgumentValueDelegate<T> {
    var value: T
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}

/**
 * Abstract base class for subcommands.
 */
@SinceKotlin("1.3")
@ExperimentalCli
abstract class Subcommand(val name: String): ArgParser(name) {
    /**
     * Execute action if subcommand was provided.
     */
    abstract fun execute()
}

/**
 * Argument parsing result.
 * Contains name of subcommand which was called.
 *
 * @property commandName name of command which was called.
 */
class ArgParserResult(val commandName: String)

/**
 * Arguments parser.
 *
 * @property programName name of current program.
 * @property useDefaultHelpShortName add or not -h flag for help message.
 * @property prefixStyle style of expected options prefix.
 * @property skipExtraArguments just skip extra arguments in command line string without producing error message.
 */
open class ArgParser(val programName: String, var useDefaultHelpShortName: Boolean = true,
                     var prefixStyle: OPTION_PREFIX_STYLE = OPTION_PREFIX_STYLE.LINUX,
                     var skipExtraArguments: Boolean = false) {

    /**
     * Map of options: key - full name of option, value - pair of descriptor and parsed values.
     */
    private val options = mutableMapOf<String, ParsingValue<*, *>>()
    /**
     * Map of arguments: key - full name of argument, value - pair of descriptor and parsed values.
     */
    private val arguments = mutableMapOf<String, ParsingValue<*, *>>()

    /**
     * Map with declared options.
     */
    private val declaredOptions = mutableListOf<CLIEntityWrapper>()

    /**
     * Map with declared arguments.
     */
    private val declaredArguments = mutableListOf<CLIEntityWrapper>()

    /**
     * Map of subcommands.
     */
    @UseExperimental(ExperimentalCli::class)
    protected val subcommands = mutableMapOf<String, Subcommand>()

    /**
     * Mapping for short options names for quick search.
     */
    private val shortNames = mutableMapOf<String, ParsingValue<*, *>>()

    /**
     * Used prefix form for full option form.
     */
    private val optionFullFormPrefix = if (prefixStyle == OPTION_PREFIX_STYLE.LINUX) "--" else "-"

    /**
     * Used prefix form for short option form.
     */
    private val optionShortFromPrefix = "-"

    /**
     * Name with all commands that should be executed.
     */
    protected val fullCommandName = mutableListOf(programName)

    /**
     * Origin of option/argument value.
     *
     * Possible values:
     * SET_BY_USER - value of option was provided in command line string;
     * SET_DEFAULT_VALUE - value of option wasn't provided in command line, but set using default value;
     * UNSET - value of option is unset
     * REDEFINED - value of option was redefined in source code after parsing.
     */
    enum class ValueOrigin { SET_BY_USER, SET_DEFAULT_VALUE, UNSET, REDEFINED }

    /**
     * Options prefix style.
     *
     * Possible values:
     * LINUX - Linux style, for full forms of options "--", for short form - "-"
     * JVM - JVM style, both for full and short forms of options "-"
     */
    enum class OPTION_PREFIX_STYLE { LINUX, JVM }

    /**
     * Add option with single possible value and get delegator to its value.
     *
     * @param type argument type, one of [ArgType].
     * @param fullName argument full name.
     * @param shortName option short name.
     * @param description text description of Argument.
     * @param deprecatedWarning text message with information in case if option is deprecated.
     */
    fun <T : Any>option(type: ArgType<T>,
                        fullName: String? = null,
                        shortName: String ? = null,
                        description: String? = null,
                        deprecatedWarning: String? = null): SingleNullableOption<T> {
        val option = SingleNullableOption(OptionDescriptor(optionFullFormPrefix, optionShortFromPrefix, type,
                fullName, shortName, description, deprecatedWarning = deprecatedWarning), CLIEntityWrapper())
        option.owner.entity = option
        declaredOptions.add(option.owner)
        return option
    }

    /**
     * Check usage of required property for arguments.
     * Make sense only for several last arguments.
     */
    private fun inspectRequiredAndDefaultUsage() {
        var previousArgument: ParsingValue<*, *>? = null
        arguments.forEach { (_, currentArgument) ->
            previousArgument?.let {
                // Previous argument has default value.
                it.descriptor.defaultValue?.let {
                    if (currentArgument.descriptor.defaultValue == null && currentArgument.descriptor.required) {
                        printError("Default value of argument ${previousArgument.descriptor.fullName} will be unused,  " +
                                "because next argument ${currentArgument.descriptor.fullName} is always required and has no default value.")
                    }
                }
                // Previous argument is optional.
                if (!it.descriptor.required) {
                    if (currentArgument.descriptor.defaultValue == null && currentArgument.descriptor.required) {
                        printError("Argument ${previousArgument.descriptor.fullName} will be always required, " +
                                "because next argument ${currentArgument.descriptor.fullName} is always required.")
                    }
                }
            }
        }
    }

    /**
     * Add argument with single nullable value and get delegator to its value.
     *
     * @param type argument type, one of [ArgType].
     * @param fullName argument full name.
     * @param description text description of argument.
     * @param deprecatedWarning text message with information in case if argument is deprecated.
     */
    fun <T : Any>argument(type: ArgType<T>,
                          fullName: String? = null,
                          description: String? = null,
                          deprecatedWarning: String? = null) : SingleArgument<T> {
        val argument = SingleArgument(ArgDescriptor(type, fullName, 1,
                description, deprecatedWarning = deprecatedWarning), CLIEntityWrapper())
        argument.owner.entity = argument
        declaredArguments.add(argument.owner)
        return argument
    }

    /**
     * Add subcommands.
     *
     * @param subcommandsList subcommands to add.
     */
    @SinceKotlin("1.3")
    @ExperimentalCli
    fun subcommands(vararg subcommandsList: Subcommand) {
        subcommandsList.forEach {
            if (it.name in subcommands) {
                printError("Subcommand with name ${it.name} was already defined.")
            }

            // Set same settings as main parser.
            it.prefixStyle = prefixStyle
            it.useDefaultHelpShortName = useDefaultHelpShortName
            fullCommandName.forEachIndexed { index, namePart ->
                it.fullCommandName.add(index, namePart)
            }
            subcommands[it.name] = it
        }
    }

    /**
     * Output error. Also adds help usage information for easy understanding of problem.
     *
     * @param message error message.
     */
    fun printError(message: String): Nothing {
        error("$message\n${makeUsage()}")
    }

    /**
     * Save value as argument value.
     *
     * @param arg string with argument value.
     * @param argumentsQueue queue with active argument descriptors.
     */
    private fun saveAsArg(arg: String, argumentsQueue: ArgumentsQueue): Boolean {
        // Find next uninitialized arguments.
        val name = argumentsQueue.pop()
        name?.let {
            val argumentValue = arguments[name]!!
            argumentValue.descriptor.deprecatedWarning?.let { printWarning(it) }
            argumentValue.addValue(arg)
            return true
        }
        return false
    }

    /**
     * Save value as option value.
     */
    private fun <T : Any, U: Any> saveAsOption(parsingValue: ParsingValue<T, U>, value: String) {
        parsingValue.addValue(value)
    }

    /**
     * Try to recognize command line element as full form of option.
     *
     * @param candidate string with candidate in options.
     */
    private fun recognizeOptionFullForm(candidate: String) =
        if (candidate.startsWith(optionFullFormPrefix))
            options[candidate.substring(optionFullFormPrefix.length)]
        else null

    /**
     * Try to recognize command line element as short form of option.
     *
     * @param candidate string with candidate in options.
     */
    private fun recognizeOptionShortForm(candidate: String) =
            if (candidate.startsWith(optionShortFromPrefix))
                shortNames[candidate.substring(optionShortFromPrefix.length)]
            else null

    /**
     * Parse arguments.
     *
     * @param args array with command line arguments.
     *
     * @return true if all arguments were parsed successfully, otherwise return false and print help message.
     */
    fun parse(args: Array<String>) = parse(args.asList())

    protected fun parse(args: List<String>): ArgParserResult {
        // Add help option.
        val helpDescriptor = if (useDefaultHelpShortName) OptionDescriptor<Boolean, Boolean>(optionFullFormPrefix,
                optionShortFromPrefix, ArgType.Boolean,
                "help", "h", "Usage info")
            else OptionDescriptor(optionFullFormPrefix, optionShortFromPrefix,
                ArgType.Boolean, "help", description = "Usage info")
        val helpOption = SingleNullableOption(helpDescriptor, CLIEntityWrapper())
        helpOption.owner.entity = helpOption
        declaredOptions.add(helpOption.owner)

        // Add default list with arguments if there can be extra free arguments.
        if (skipExtraArguments) {
            argument(ArgType.String, "").vararg()
        }

        // Map declared options and arguments to maps.
        declaredOptions.forEachIndexed { index, option ->
            val value = option.entity?.delegate as ParsingValue<*, *>
            value.descriptor.fullName?.let {
                // Add option.
                if (options.containsKey(it)) {
                    error("Option with full name $it was already added.")
                }
                with(value.descriptor as OptionDescriptor) {
                    if (shortName != null && shortNames.containsKey(shortName)) {
                        error("Option with short name ${shortName} was already added.")
                    }
                    shortName?.let {
                        shortNames[it] = value
                    }
                }
                options[it] = value

            } ?: error("Option was added, but unnamed. Added option under №${index + 1}")
        }

        declaredArguments.forEachIndexed { index, argument ->
            val value = argument.entity?.delegate as ParsingValue<*, *>
            value.descriptor.fullName?.let {
                // Add option.
                if (arguments.containsKey(it)) {
                    error("Argument with full name $it was already added.")
                }
                arguments[it] = value
            } ?: error("Argument was added, but unnamed. Added argument under №${index + 1}")
        }
        // Make inspections for arguments.
        inspectRequiredAndDefaultUsage()

        val argumentsQueue = ArgumentsQueue(arguments.map { it.value.descriptor as ArgDescriptor<*, *> })

        var index = 0
        try {
            while (index < args.size) {
                val arg = args[index]
                // Check for subcommands.
                @UseExperimental(ExperimentalCli::class)
                subcommands.forEach { (name, subcommand) ->
                    if (arg == name) {
                        // Use parser for this subcommand.
                        subcommand.parse(args.slice(index + 1..args.size - 1))
                        subcommand.execute()

                        return ArgParserResult(name)
                    }
                }
                // Parse arguments from command line.
                if (arg.startsWith('-')) {
                    // Candidate in being option.
                    // Option is found.
                    val argValue = recognizeOptionShortForm(arg) ?: recognizeOptionFullForm(arg)
                    argValue?.descriptor?.let {
                        if (argValue.descriptor.type.hasParameter) {
                            if (index < args.size - 1) {
                                saveAsOption(argValue, args[index + 1])
                                index++
                            } else {
                                // An error, option with value without value.
                                printError("No value for ${argValue.descriptor.textDescription}")
                            }
                        } else {
                            // Boolean flags.
                            if (argValue.descriptor.fullName == "help") {
                                println(makeUsage())
                                exitProcess(0)
                            }
                            saveAsOption(argValue, "true")
                        }
                    } ?: run {
                        // Try save as argument.
                        if (!saveAsArg(arg, argumentsQueue)) {
                            printError("Unknown option $arg")
                        }
                    }
                } else {
                    // Argument is found.
                    if (!saveAsArg(arg, argumentsQueue)) {
                        printError("Too many arguments! Couldn't process argument $arg!")
                    }
                }
                index++
            }
            // Postprocess results of parsing.
            options.values.union(arguments.values).forEach { value ->
                // Not inited, append default value if needed.
                if (value.isEmpty()) {
                    value.addDefaultValue()
                }
            }
        } catch (exception: ParsingException) {
            printError(exception.message!!)
        }

        return ArgParserResult(programName)
    }

    /**
     * Create message with usage description.
     */
    internal fun makeUsage(): String {
        val result = StringBuilder()
        result.append("Usage: ${fullCommandName.joinToString(" ")} options_list\n")
        if (arguments.isNotEmpty()) {
            result.append("Arguments: \n")
            arguments.forEach {
                result.append(it.value.descriptor.helpMessage)
            }
        }
        if (options.isNotEmpty()) {
            result.append("Options: \n")
            options.forEach {
                result.append(it.value.descriptor.helpMessage)
            }
        }
        return result.toString()
    }
}

/**
 * Output warning.
 *
 * @param message warning message.
 */
internal fun printWarning(message: String) {
    println("WARNING $message")
}