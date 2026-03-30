/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments.model.jvm

import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.tests.arguments.model.ArgumentTestDescriptor

internal class JvmArgumentTestDescriptor<T>(
    override val argumentName: String,
    override val argument: JvmCompilerArguments.JvmCompilerArgument<T>,
    override val argumentValues: List<T>,
    override val invalidArgumentValue: T? = null,
    val valueString: (T?) -> String?,
    val expectedArgumentStringsFor: (String) -> List<String>,
) : ArgumentTestDescriptor<T> {
    override fun getValueString(argument: T?): String? = valueString.invoke(argument)

    override fun expectedArgumentStringsFor(value: String): List<String> =
        expectedArgumentStringsFor.invoke(value)
}