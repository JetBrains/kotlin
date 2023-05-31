/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.cli.common.arguments.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.Base64.getEncoder
import kotlin.random.Random
import kotlin.reflect.*
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.javaField
import kotlin.test.assertContentEquals
import kotlin.test.fail

class CompilerArgumentParsingTest {

    @ParameterizedTest
    @MethodSource("parameters")
    fun `test - parsing random compiler arguments`(
        type: KClass<out CommonToolArguments>,
        seed: Int,
        shortArgumentKeys: Boolean,
        compactArgumentValues: Boolean
    ) {
        val constructor = type.constructors.find { it.parameters.isEmpty() } ?: error("Missing empty constructor on $type")
        val arguments = constructor.call()
        arguments.fillRandomValues(Random(seed))
        val argumentsAsStrings = arguments.toArgumentStrings(
            shortArgumentKeys = shortArgumentKeys,
            compactArgumentValues = compactArgumentValues
        )
        val parsedArguments = parseCommandLineArguments(type, argumentsAsStrings)
        assertEqualArguments(arguments, parsedArguments)
        assertEquals(
            argumentsAsStrings,
            parsedArguments.toArgumentStrings(
                shortArgumentKeys = shortArgumentKeys,
                compactArgumentValues = compactArgumentValues
            )
        )
    }

    companion object {
        @JvmStatic
        fun parameters(): List<Arguments> = getCompilerArgumentImplementations()
            .flatMap { clazz ->
                listOf(1002, 2803, 2411).flatMap { seed ->
                    listOf(true, false).flatMap { shortArgumentKeys ->
                        listOf(true, false).map { compactArgumentValues ->
                            Arguments.of(
                                Named.of("${clazz.simpleName}", clazz),
                                Named.of("seed: $seed", seed),
                                Named.of("shortArgumentKeys: $shortArgumentKeys", shortArgumentKeys),
                                Named.of("compactArgumentValues: $compactArgumentValues", compactArgumentValues)
                            )
                        }
                    }
                }
            }
    }
}

private fun assertEqualArguments(expected: CommonToolArguments, actual: CommonToolArguments) {
    if (expected::class != actual::class) fail("Expected class '${expected::class}', found: '${actual::class}'")
    expected::class.memberProperties
        .filter { it.javaField?.getAnnotation(Argument::class.java) != null }
        .ifEmpty { fail("No members with ${Argument::class} annotation") }
        .map { property ->
            @Suppress("UNCHECKED_CAST")
            property as KProperty1<Any, Any?>
            val expectedValue = property.get(expected)
            val actualValue = property.get(actual)

            val message = "Unexpected value in '${property.name}: '${property.returnType}'"
            if (property.returnType.isSubtypeOf(typeOf<Array<*>?>())) {
                @Suppress("UNCHECKED_CAST")
                assertContentEquals(
                    expectedValue as Array<Any?>?, actualValue as Array<Any?>?,
                    message
                )
            } else assertEquals(
                expectedValue, actualValue,
                message
            )
        }
}

private fun CommonToolArguments.fillRandomValues(random: Random) {
    this::class.memberProperties.filterIsInstance<KMutableProperty1<*, *>>().forEach { property ->
        @Suppress("UNCHECKED_CAST")
        property as KMutableProperty1<Any, Any>
        runCatching {
            property.set(this, random.randomValue(property.returnType) ?: return@forEach)
        }.getOrElse {
            throw Throwable("Failed setting random value for: ${property.name}: ${property.returnType}", it)
        }
    }
}

private fun Random.randomString() = nextBytes(nextInt(8, 12)).let { data ->
    getEncoder().withoutPadding().encodeToString(data)
}

private fun Random.randomBoolean() = nextBoolean()

private fun Random.randomStringArray(): Array<String> {
    val size = nextInt(5, 10)
    return Array(size) {
        randomString()
    }
}

private fun Random.randomList(elementType: KType): List<Any>? {
    val size = nextInt(5, 10)
    return List(size) {
        randomValue(elementType) ?: return null
    }
}

fun Random.randomValue(type: KType): Any? {
    @Suppress("NAME_SHADOWING")
    val type = type.withNullability(false)
    return when {
        type == typeOf<String>() -> randomString()
        type == typeOf<Boolean>() -> randomBoolean()
        type == typeOf<Array<String>>() -> randomStringArray()
        type.isSubtypeOf(typeOf<List<*>>()) -> randomList(type.arguments.first().type ?: error("Missing elementType on $type"))
        type == typeOf<InternalArgument>() -> return null
        (type.classifier as? KClass<*>)?.isData == true -> null
        else -> error("Unsupported type '$type'")
    }
}
