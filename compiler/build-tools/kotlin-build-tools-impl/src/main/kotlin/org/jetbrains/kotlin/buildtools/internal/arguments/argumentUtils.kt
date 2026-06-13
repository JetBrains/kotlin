/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("SpellCheckingInspection", "unused")

package org.jetbrains.kotlin.buildtools.internal.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.getArgumentsInfo
import java.nio.file.Path
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmName

internal fun CommonToolArgumentsImpl.reportRestrictedViolations(logger: KotlinLogger) {
    for (violation in restrictedArgViolations) {
        when (violation) {
            is RestrictedArgViolation.Error -> throw CompilerArgumentsParseException(violation.message)
            is RestrictedArgViolation.Warning -> logger.warn(violation.message)
        }
    }
}

internal fun CommonToolArgumentsImpl.hasValidationErrors(): Boolean =
    argumentValidationErrors.isNotEmpty()

internal fun CommonToolArgumentsImpl.reportValidationErrors(logger: KotlinLogger) {
    for (error in argumentValidationErrors) {
        logger.error(error)
    }
}

internal fun <T> CommonToolArguments.setUsingReflection(propertyName: String, value: T) {
    this::class.declaredMemberProperties.filterIsInstance<KMutableProperty<T>>().firstOrNull { it.name == propertyName }
        ?.let { property: KMutableProperty<T> ->
            property.setter.call(this, value)
        } ?: throw NoSuchMethodError("No property found with name $propertyName in ${this::class.jvmName}")
}

internal fun <T> CommonToolArguments.getUsingReflection(propertyName: String): T {
    return this::class.declaredMemberProperties.filterIsInstance<KProperty<T>>()
        .firstOrNull { it.name == propertyName }
        ?.let { property: KProperty<T> ->
            property.getter.call(this)
        } ?: throw NoSuchMethodError("No property found with name $propertyName in ${this::class.jvmName}")
}

internal fun Path.absolutePathStringOrThrow(): String = toFile().absolutePath

internal fun <T> Array<out T>?.toListOrEmpty(): List<T> = this?.toList() ?: emptyList()

internal fun <T, R> Array<out T>?.mapOrEmpty(transform: (T) -> R): List<R> = this?.map(transform) ?: emptyList()

internal fun List<String>.checkNoneContains(other: CharSequence) {
    val invalidItem = firstOrNull { it.contains(other) }
    if (invalidItem != null) {
        throw CompilerArgumentsParseException(
            "Invalid character '${other}' found in argument '$invalidItem'. " +
                    "This character is currently not supported in this context. " +
                    "If you need its support, please let us know: https://youtrack.jetbrains.com/issue/KT-85553"
        )
    }
}

internal fun checkCaseMatches(
    restrictedArgViolations: MutableList<RestrictedArgViolation>,
    argument: KProperty<*>,
    stringValue: String,
    passedValue: String
) {
    if (stringValue == passedValue) return
    else {
        val argumentName = argument.javaField?.getAnnotation(Argument::class.java)?.value!!
        restrictedArgViolations.add(RestrictedArgViolation.Warning("Case mismatch for $argumentName: expected '$stringValue', got '$passedValue'. This will become an error in Kotlin compiler version 2.6.0"))
    }
}

internal fun populateExplicitArguments(arguments: CommonToolArguments) {
    val argumentsInfo = getArgumentsInfo(arguments.javaClass)

    arguments.explicitArguments = buildMap {
        for (argumentField in argumentsInfo.cliArgNameToArguments.values) {
            val actualValue = argumentField.getter.invoke(arguments)
            val defaultValue = argumentsInfo.getDefaultValue(argumentField)

            val isDefaultValue = if (actualValue is Array<*>) {
                actualValue.contentEquals(defaultValue as Array<*>)
            } else {
                actualValue == defaultValue
            }

            if (!isDefaultValue) {
                this[argumentField] = listOf(actualValue)
            }
        }
    }
}
