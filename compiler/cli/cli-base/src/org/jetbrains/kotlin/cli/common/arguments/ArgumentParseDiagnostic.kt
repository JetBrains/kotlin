/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.CliDiagnostics.CLI_ARGFILE_ERROR
import org.jetbrains.kotlin.cli.CliDiagnostics.CLI_ARGUMENT_VALUE_PASSED_MULTIPLE_TIMES
import org.jetbrains.kotlin.cli.CliDiagnostics.CLI_ARGUMENT_WITHOUT_VALUE
import org.jetbrains.kotlin.cli.CliDiagnostics.CLI_ARGUMENT_WITH_DEPRECATED_NAME
import org.jetbrains.kotlin.cli.CliDiagnostics.CLI_BOOLEAN_ARGUMENT_WITH_INCORRECT_VALUE
import org.jetbrains.kotlin.cli.CliDiagnostics.CLI_BOOLEAN_LANGUAGE_FEATURE_ARGUMENT_WITH_VALUE
import org.jetbrains.kotlin.cli.CliDiagnostics.CLI_EXTRA_ARGUMENT_IN_OBSOLETE_FORM
import org.jetbrains.kotlin.cli.CliDiagnostics.CLI_INTERNAL_ARGUMENT_ERROR
import org.jetbrains.kotlin.cli.CliDiagnostics.CLI_INTERNAL_ARGUMENT_WARNING
import org.jetbrains.kotlin.cli.CliDiagnostics.CLI_INVALID_ARGUMENT
import org.jetbrains.kotlin.cli.CliDiagnostics.CLI_STRING_LANGUAGE_FEATURE_ARGUMENT_WITH_INCORRECT_VALUE
import org.jetbrains.kotlin.cli.CliDiagnostics.CLI_UNKNOWN_EXTRA_ARGUMENT
import org.jetbrains.kotlin.cli.CliDiagnostics.CLI_UNSAFE_INTERNAL_ARGUMENT
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory
import kotlin.collections.component1
import kotlin.collections.component2

/**
 * Determines whether a list of [ArgumentParseDiagnostic] instances contains at least one fatal error.
 *
 * A fatal error means that no further compilation steps should be performed: the arguments are broken badly enough
 * to make the whole compilation meaningless.
 */
val List<ArgumentParseDiagnostic>.hasFatalError: Boolean
    get() = any { it.isFatal }

fun CommonToolArguments.getFatalDiagnosticsMessage(): String {
    return StringReporterWrapper().reportDiagnostics(this, fatal = true)
}

fun MessageCollector.reportCliArgumentFatalDiagnostics(diagnostics: List<ArgumentParseDiagnostic>) {
    MessageReporterWrapper(this).reportParseDiagnostics(diagnostics, fatal = true)
}

fun MessageCollector.reportCliArgumentFatalDiagnostics(arguments: CommonToolArguments) {
    reportCliArgumentFatalDiagnostics(arguments.diagnostics)
}

fun MessageCollector.reportCliArgumentNonFatalDiagnostics(arguments: CommonToolArguments) {
    MessageReporterWrapper(this).reportDiagnostics(arguments, fatal = false)
}

fun CompilerConfiguration.reportCliArgumentNonFatalDiagnostics(arguments: CommonToolArguments) {
    CompilerConfigurationWrapper(this).reportDiagnostics(arguments, fatal = false)
}

fun MutableCollection<String>.appendFatalErrors(arguments: CommonToolArguments) {
    StringCollectionReporterWrapper(this).reportDiagnostics(arguments, fatal = true)
}

sealed class ArgumentParseDiagnostic(val factory: KtSourcelessDiagnosticFactory, val isFatal: Boolean = false) {
    class ArgumentWithoutValue(val argument: String) :
        ArgumentParseDiagnostic(CLI_ARGUMENT_WITHOUT_VALUE, isFatal = true)

    class BooleanArgumentWithIncorrectValue(val argument: String) :
        ArgumentParseDiagnostic(CLI_BOOLEAN_ARGUMENT_WITH_INCORRECT_VALUE, isFatal = true)

    class BooleanLanguageFeatureArgumentWithValue(val argument: String) :
        ArgumentParseDiagnostic(CLI_BOOLEAN_LANGUAGE_FEATURE_ARGUMENT_WITH_VALUE, isFatal = true)

    class StringLanguageFeatureArgumentWithIncorrectValue(val argument: String, val value: String, val allowedValues: Collection<String>) :
        ArgumentParseDiagnostic(CLI_STRING_LANGUAGE_FEATURE_ARGUMENT_WITH_INCORRECT_VALUE, isFatal = true)

    class InvalidArgument(val argument: String) :
        ArgumentParseDiagnostic(CLI_INVALID_ARGUMENT, isFatal = true)

    class UnknownExtraFlag(val argument: String) :
        ArgumentParseDiagnostic(CLI_UNKNOWN_EXTRA_ARGUMENT)

    class ExtraArgumentInObsoleteForm(val argument: String) :
        ArgumentParseDiagnostic(CLI_EXTRA_ARGUMENT_IN_OBSOLETE_FORM)

    class ArgumentWithDeprecatedName(val deprecatedName: String, val newName: String) :
        ArgumentParseDiagnostic(CLI_ARGUMENT_WITH_DEPRECATED_NAME)

    class ArgfileError(val message: String) :
        ArgumentParseDiagnostic(CLI_ARGFILE_ERROR)

    class InternalArgumentError(val message: String) :
        ArgumentParseDiagnostic(CLI_INTERNAL_ARGUMENT_ERROR)

    class InternalArgumentWarning(val message: String) :
        ArgumentParseDiagnostic(CLI_INTERNAL_ARGUMENT_WARNING)
}

private sealed class CliDiagnosticReporterWrapper<T> {
    /**
     * The [fatal] flag selects which diagnostics to report: `true` reports only the fatal ones, `false` only the non-fatal ones.
     *
     * The flag could be dropped entirely (reporting all diagnostics whenever a fatal error is encountered),
     * but the old behavior is preserved for backward compatibility.
     */
    fun reportDiagnostics(arguments: CommonToolArguments, fatal: Boolean) : T {
        reportParseDiagnostics(arguments.diagnostics, fatal)

        if (!fatal) {
            checkArgumentValuePassedMultipleTimes(arguments)
            checkUnsafeInternalArguments(arguments)
        }

        return prepareResult()
    }

    fun reportParseDiagnostics(diagnostics: List<ArgumentParseDiagnostic>, fatal: Boolean) {
        for (diagnostic in diagnostics) {
            if (fatal xor diagnostic.isFatal) {
                continue
            }

            val message = when (diagnostic) {
                is ArgumentWithoutValue -> "No value passed for argument '${diagnostic.argument}'."
                is BooleanArgumentWithIncorrectValue -> "Incorrect value for boolean argument '${diagnostic.argument}'. Only 'true' and 'false' are allowed."
                is BooleanLanguageFeatureArgumentWithValue -> "No value is expected for argument '${diagnostic.argument}'."
                is StringLanguageFeatureArgumentWithIncorrectValue -> {
                    val allowedValuesString = diagnostic.allowedValues.joinToString(", ") { "'$it'" }
                    "Incorrect value '${diagnostic.value}' for argument '${diagnostic.argument}'. " +
                            "Allowed values: $allowedValuesString."
                }
                is InvalidArgument -> "Invalid argument: '${diagnostic.argument}'."
                is UnknownExtraFlag -> "Flag is not supported by this version of the compiler: '${diagnostic.argument}'."
                is ExtraArgumentInObsoleteForm -> "Advanced option value is passed in an obsolete form. To specify the value, use the '=' character: '${diagnostic.argument}=...'."
                is ArgumentWithDeprecatedName -> "Argument '${diagnostic.deprecatedName}' is deprecated. Use '${diagnostic.newName}' instead."
                is ArgfileError -> diagnostic.message
                is InternalArgumentError -> diagnostic.message
                is InternalArgumentWarning -> diagnostic.message
            }

            report(diagnostic.factory, message)
        }
    }

    private fun checkArgumentValuePassedMultipleTimes(arguments: CommonToolArguments) {
        for ([key, values] in arguments.explicitArguments) {
            if (values.size <= 1 || values.distinct().size == 1) continue

            val argName = key.argument.value
            val valuesString = values.joinToString("', '")
            val message = "Argument '$argName' is passed multiple times: '$valuesString'. The last value will be used."
            report(CLI_ARGUMENT_VALUE_PASSED_MULTIPLE_TIMES, message)
        }
    }

    private fun checkUnsafeInternalArguments(arguments: CommonToolArguments) {
        val unsafeArguments = arguments.internalArguments.filterNot {
            // -XXLanguage that turns on a BUG_FIX is considered safe
            it.languageFeature.actuallyEnabledInProgressiveMode && it.state == ENABLED
        }

        if (unsafeArguments.isNotEmpty()) {
            val unsafeArgumentsString = unsafeArguments.joinToString(prefix = "\n", postfix = "\n\n", separator = "\n") {
                it.stringRepresentation
            }

            report(
                CLI_UNSAFE_INTERNAL_ARGUMENT,
                "ATTENTION!\n" +
                        "This build uses unsafe internal compiler arguments:\n" +
                        unsafeArgumentsString +
                        "This mode is not recommended for production use,\n" +
                        "as no stability/compatibility guarantees are given on\n" +
                        "compiler or generated code. Use it at your own risk!\n"
            )
        }
    }

    protected abstract fun report(factory: KtSourcelessDiagnosticFactory, message: String)

    protected abstract fun prepareResult(): T
}

private class MessageReporterWrapper(val messageCollector: MessageCollector) : CliDiagnosticReporterWrapper<MessageCollector>() {
    override fun report(factory: KtSourcelessDiagnosticFactory, message: String) {
        messageCollector.report(factory.severity.toCompilerMessageSeverity(), message)
    }

    override fun prepareResult(): MessageCollector = messageCollector
}

private class CompilerConfigurationWrapper(val compilerConfiguration: CompilerConfiguration) : CliDiagnosticReporterWrapper<CompilerConfiguration>() {
    override fun report(factory: KtSourcelessDiagnosticFactory, message: String) {
        compilerConfiguration.report(factory, message)
    }

    override fun prepareResult(): CompilerConfiguration = compilerConfiguration
}

private class StringReporterWrapper : CliDiagnosticReporterWrapper<String>() {
    private val result = StringBuilder()

    override fun report(factory: KtSourcelessDiagnosticFactory, message: String) {
        result.appendLine(message)
    }

    override fun prepareResult(): String = result.toString().trimEnd('\n')
}

private class StringCollectionReporterWrapper(val collection: MutableCollection<String>) : CliDiagnosticReporterWrapper<MutableCollection<String>>() {
    override fun report(factory: KtSourcelessDiagnosticFactory, message: String) {
        collection.add(message)
    }

    override fun prepareResult(): MutableCollection<String> = collection
}
