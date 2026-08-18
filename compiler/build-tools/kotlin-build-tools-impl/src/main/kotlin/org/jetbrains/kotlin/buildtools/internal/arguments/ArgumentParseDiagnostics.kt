/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import org.jetbrains.kotlin.cli.common.arguments.ArgumentField
import org.jetbrains.kotlin.cli.common.arguments.ArgumentsInfo
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.getArgumentsInfo
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments

/**
 * Remembers everything needed to reproduce the warnings that the CLI compiler reports while parsing its arguments
 * (see [org.jetbrains.kotlin.cli.common.reportArgumentParseProblems]).
 */
internal class ArgumentParseDiagnostics private constructor(
    private var argumentsClass: Class<out CommonToolArguments>?,
    private val argumentStringBatches: MutableList<List<String>>,
    private val valuesFromTypedApi: MutableMap<String, MutableList<IndexedValue<Any>>>,
) {
    constructor() : this(null, mutableListOf(), LinkedHashMap())

    fun copy(): ArgumentParseDiagnostics = ArgumentParseDiagnostics(
        argumentsClass,
        argumentStringBatches.toMutableList(),
        valuesFromTypedApi.mapValuesTo(LinkedHashMap()) { entry -> entry.value.toMutableList() },
    )

    fun isEmpty(): Boolean = argumentStringBatches.isEmpty()

    /**
     * Records one `applyArgumentStrings` call.
     *
     * [parsedArguments] is the result of parsing [newArgumentStrings], and [currentArguments] produces the arguments as
     * they are configured *before* the call is applied. It is a lambda because it is only needed when an argument is
     * being overwritten.
     */
    fun record(
        parsedArguments: CommonToolArguments,
        newArgumentStrings: List<String>,
        currentArguments: () -> CommonToolArguments,
    ) {
        try {
            recordOverwrittenTypedApiValues(parsedArguments, currentArguments)
        } catch (_: Throwable) {
            // best-effort for potential problems detection
        }
        argumentsClass = parsedArguments.javaClass
        argumentStringBatches += newArgumentStrings
    }

    private fun recordOverwrittenTypedApiValues(
        parsedArguments: CommonToolArguments,
        currentArguments: () -> CommonToolArguments,
    ) {
        if (parsedArguments.explicitArguments.isEmpty()) return
        val argumentsClass = parsedArguments.javaClass
        val argumentsInfo = getArgumentsInfo(argumentsClass)
        val impliedByPreviousBatches = parseCommandLineArguments(argumentsClass.kotlin, argumentStringBatches.flatten())
        val current by lazy(LazyThreadSafetyMode.NONE) { currentArguments() }
        for (field in parsedArguments.explicitArguments.keys) {
            val cliArgumentName = field.argument.value
            // a removed argument has no property on the arguments class, so it can't have been set through the typed API
            if (argumentsInfo.cliArgNameToArguments[cliArgumentName] !== field) continue
            val currentValue = field.getter.invoke(current) ?: continue
            if (valuesEqual(argumentsInfo.getDefaultValue(field), currentValue)) continue
            if (valuesEqual(field.getter.invoke(impliedByPreviousBatches), currentValue)) continue
            valuesFromTypedApi.getOrPut(cliArgumentName) { mutableListOf() }
                .add(IndexedValue(argumentStringBatches.size, currentValue.normalizeForReporting()))
        }
    }

    /**
     * Builds a throwaway arguments instance carrying the [CommonToolArguments.errors] and the multi-valued
     * [CommonToolArguments.explicitArguments] that the Build Tools API argument model cannot represent.
     *
     * [finalArguments] are the arguments as they will be handed to the compiler. They are what makes a value configured
     * through the typed argument API *after* the last `applyArgumentStrings` observable: such a value is in neither
     * [argumentStringBatches] nor [valuesFromTypedApi], but it is the value the compiler ends up using, so it can
     * differ from what the argument strings alone imply.
     */
    fun buildReportableArguments(finalArguments: CommonToolArguments): CommonToolArguments? {
        val argumentsClass = argumentsClass ?: return null
        if (finalArguments.javaClass != argumentsClass) return null
        val arguments = parseCommandLineArguments(argumentsClass.kotlin, argumentStringBatches.flatten())
        val argumentsInfo = getArgumentsInfo(argumentsClass)
        val valuesPerBatch = argumentStringBatches.map { batch ->
            parseCommandLineArguments(argumentsClass.kotlin, batch).explicitArguments
        }
        arguments.explicitArguments = arguments.explicitArguments.mapValues { entry ->
            explicitValuesInConfigurationOrder(entry.key, valuesPerBatch, argumentsInfo, arguments, finalArguments)
        }
        // internal (-XX) arguments do survive the Build Tools API argument model, so the compiler can report them itself
        arguments.internalArguments = emptyList()
        return arguments
    }

    private fun explicitValuesInConfigurationOrder(
        field: ArgumentField,
        valuesPerBatch: List<Map<ArgumentField, List<Any>>>,
        argumentsInfo: ArgumentsInfo,
        argumentsFromStrings: CommonToolArguments,
        finalArguments: CommonToolArguments,
    ): List<Any> {
        val cliArgumentName = field.argument.value
        val typedApiValues = valuesFromTypedApi[cliArgumentName].orEmpty()
        val values = mutableListOf<Any>()
        for (batchIndex in valuesPerBatch.indices) {
            typedApiValues.forEach { if (it.index == batchIndex) values.add(it.value) }
            valuesPerBatch[batchIndex][field]?.let { values.addAll(it) }
        }
        // a removed argument has no property to read the final value from
        if (argumentsInfo.cliArgNameToArguments[cliArgumentName] !== field) return values
        val finalValue = field.getter.invoke(finalArguments) ?: return values
        if (!valuesEqual(field.getter.invoke(argumentsFromStrings), finalValue)) {
            values += finalValue.normalizeForReporting()
        }
        return values
    }

    private fun valuesEqual(first: Any?, second: Any?): Boolean = when {
        first is Array<*> && second is Array<*> -> first.contentEquals(second)
        else -> first == second
    }

    /**
     * The argument parser stores array-valued arguments as lists, so values read back from the arguments instance have
     * to be converted to match, or the same value would render differently depending on where it came from.
     */
    private fun Any.normalizeForReporting(): Any = if (this is Array<*>) toList() else this
}
