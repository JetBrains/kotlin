/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("ArgumentsToStrings")

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.isAdvanced
import org.jetbrains.kotlin.cli.common.arguments.resolvedDelimiter
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

@Suppress("UNCHECKED_CAST")
@JvmOverloads
fun CommonToolArguments.toArgumentStrings(shortArgumentKeys: Boolean = false, compactArgumentValues: Boolean = true): List<String> {
    return toArgumentStrings(
        this, this::class as KClass<CommonToolArguments>,
        shortArgumentKeys = shortArgumentKeys,
        compactArgumentValues = compactArgumentValues
    )
}

@PublishedApi
internal fun <T : CommonToolArguments> toArgumentStrings(
    thisArguments: T, type: KClass<T>,
    shortArgumentKeys: Boolean,
    compactArgumentValues: Boolean
): List<String> = ArrayList<String>().apply {
    val defaultArguments = type.newArgumentsInstance()
    type.memberProperties.forEach { property ->
        val argumentAnnotation = property.javaField?.getAnnotation(Argument::class.java) ?: return@forEach
        val rawPropertyValue = property.get(thisArguments)
        val rawDefaultValue = property.get(defaultArguments)

        /* Default value can be omitted */
        if (rawPropertyValue == rawDefaultValue) {
            return@forEach
        }

        val argumentStringValues = when {
            property.returnType.classifier == Boolean::class -> listOf(rawPropertyValue?.toString() ?: false.toString())

            (property.returnType.classifier as? KClass<*>)?.java?.isArray == true ->
                getArgumentStringValue(argumentAnnotation, rawPropertyValue as Array<*>?, compactArgumentValues)

            property.returnType.classifier == List::class ->
                getArgumentStringValue(argumentAnnotation, (rawPropertyValue as List<*>?)?.toTypedArray(), compactArgumentValues)

            else -> listOf(rawPropertyValue.toString())
        }

        val argumentName = if (shortArgumentKeys && argumentAnnotation.shortName.isNotEmpty()) argumentAnnotation.shortName
        else argumentAnnotation.value

        argumentStringValues.forEach { argumentStringValue ->

            when {
                /* We can just enable the flag by passing the argument name like -myFlag: Value not required */
                rawPropertyValue is Boolean && rawPropertyValue -> {
                    add(argumentName)
                }

                /* Advanced (e.g. -X arguments) or boolean properties need to be passed using the '=' */
                argumentAnnotation.isAdvanced || property.returnType.classifier == Boolean::class -> {
                    add("$argumentName=$argumentStringValue")
                }
                else -> {
                    add(argumentName)
                    add(argumentStringValue)
                }
            }
        }
    }

    addAll(thisArguments.freeArgs)
    addAll(thisArguments.internalArguments.map { it.stringRepresentation })
}

private fun getArgumentStringValue(argumentAnnotation: Argument, values: Array<*>?, compactArgumentValues: Boolean): List<String> {
    if (values.isNullOrEmpty()) return emptyList()
    val delimiter = argumentAnnotation.resolvedDelimiter
    return if (delimiter.isNullOrEmpty() || !compactArgumentValues) values.map { it.toString() }
    else listOf(values.joinToString(delimiter))
}

private fun <T : CommonToolArguments> KClass<T>.newArgumentsInstance(): T {
    val argumentConstructor = constructors.find { it.parameters.isEmpty() } ?: throw IllegalArgumentException(
        "$qualifiedName has no empty constructor"
    )
    return argumentConstructor.call()
}