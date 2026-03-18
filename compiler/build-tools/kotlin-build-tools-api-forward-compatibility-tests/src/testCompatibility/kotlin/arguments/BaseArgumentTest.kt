/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.arguments.description.actualCommonCompilerArguments
import org.jetbrains.kotlin.arguments.description.actualJvmCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.ExperimentalArgumentApi
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import org.jetbrains.kotlin.buildtools.tests.BaseCompilationTest

abstract class BaseArgumentTest<T>(val argumentName: String) : BaseCompilationTest() {

    abstract fun getValueString(argument: T?): String?

    @JvmName("expectedArgumentStringsForNullable")
    protected fun expectedArgumentStringsFor(value: String?): List<String> {
        if (value == null || value == getDefaultValueString()) {
            return emptyList()
        }

        return expectedArgumentStringsFor(value)
    }

    abstract fun expectedArgumentStringsFor(value: String): List<String>

    protected fun getDefaultValueString(): String? {
        val argument =
            actualJvmCompilerArguments.arguments.firstOrNull { it.name == argumentName }
                ?: actualCommonCompilerArguments.arguments.firstOrNull { it.name == argumentName }
                ?: error("Argument '$argumentName' not found.")

        return argument.defaultValueString()
    }

    @OptIn(ExperimentalArgumentApi::class)
    private fun KotlinCompilerArgument.defaultValueString(): String? {
        @Suppress("UNCHECKED_CAST")
        val argumentType = argumentType as KotlinArgumentValueType<Any>
        return argumentType.stringRepresentation(argumentType.defaultValue.current)?.removeSurrounding("\"")
    }
}