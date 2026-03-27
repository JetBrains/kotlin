/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments.model

import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion

internal fun <T> argumentTestDescriptor(init: ArgumentTestDescriptor.Builder<T>.() -> Unit): ArgumentTestDescriptor<T> {
    return ArgumentTestDescriptor.Builder<T>().apply(init).build()
}

internal interface ArgumentTestDescriptor<T> {
    val argumentName: String
    val argumentId: String
    val availableSinceVersion: KotlinReleaseVersion

    val argumentValues: List<T>

    val runsEnumTest: Boolean
    val runsNullableTest: Boolean
    val skipBtaV1: Boolean

    fun getValueString(argument: T?): String?
    fun expectedArgumentStringsFor(value: String): List<String>

    class Builder<T> {
        var argumentName: String? = null
        var argumentId: String? = null
        var availableSinceVersion: KotlinReleaseVersion? = null
        var argumentValues: List<T>? = null
        var runsEnumTest: Boolean = false
        var runsNullableTest: Boolean = false
        var skipBtaV1: Boolean = false
        var valueString: ((T?) -> String?)? = null
        var expectedArgumentStringsFor: ((String) -> List<String>)? = null

        fun build(): ArgumentTestDescriptor<T> {
            return ArgumentTestDescriptorImpl(
                argumentName = argumentName ?: error("argumentName is required"),
                argumentId = argumentId ?: error("argumentId is required"),
                availableSinceVersion = availableSinceVersion ?: error("availableSinceVersion is required"),
                argumentValues = argumentValues ?: error("argumentValues is required"),
                runsEnumTest = runsEnumTest,
                runsNullableTest = runsNullableTest,
                skipBtaV1 = skipBtaV1,
                valueString = valueString ?: error("valueString is required"),
                expectedArgumentStringsFor = expectedArgumentStringsFor ?: error("expectedArgumentStringsFor is required"),
            )
        }
    }
}

private class ArgumentTestDescriptorImpl<T>(
    override val argumentName: String,
    override val argumentId: String,
    override val availableSinceVersion: KotlinReleaseVersion,
    override val argumentValues: List<T>,
    override val runsEnumTest: Boolean,
    override val runsNullableTest: Boolean,
    override val skipBtaV1: Boolean,
    val valueString: (T?) -> String?,
    val expectedArgumentStringsFor: (String) -> List<String>,
) : ArgumentTestDescriptor<T> {
    override fun getValueString(argument: T?): String? = valueString.invoke(argument)

    override fun expectedArgumentStringsFor(value: String): List<String> =
        expectedArgumentStringsFor.invoke(value)
}
