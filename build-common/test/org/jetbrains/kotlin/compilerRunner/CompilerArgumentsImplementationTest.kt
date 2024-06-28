/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.test.fail


class CompilerArgumentsImplementationTest {

    @ParameterizedTest
    @MethodSource("implementations")
    fun `test - all properties with Argument annotation - are public`(implementation: KClass<out CommonToolArguments>) {
        implementation.memberProperties.forEach { property ->
            if (property.javaField?.getAnnotation(Argument::class.java) != null) {
                if (property.visibility != KVisibility.PUBLIC) {
                    fail(
                        "Property '${property.name}: ${property.returnType}' " +
                                "is marked with @${Argument::class.java.simpleName}, but is not public (${property.visibility})"
                    )
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun implementations() = getCompilerArgumentImplementations()
    }
}
