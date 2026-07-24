/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments.model.js

import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments
import org.jetbrains.kotlin.buildtools.tests.arguments.model.ArgumentTestDescriptor

internal class JsArgumentTestDescriptor<T>(
    override val argumentName: String,
    override val argument: Any,
    override val availableSinceVersion: KotlinReleaseVersion,
    override val argumentValues: List<T>,
    override val argumentRawValues: List<String>,
    override val invalidArgumentValues: List<T> = emptyList(),
    override val invalidRawValues: List<String> = emptyList(),
    override val runsNullableTest: Boolean = false,
    // The whole JS BTA toolchain is v2-only (introduced in 2.4.20), so it is not reachable through the
    // v1 adapter; default to skipping v1 for all JS arguments.
    override val skipBtaV1: Boolean = true,
    private val valueString: (T?) -> String?,
    private val expectedArgumentStringsFor: (String) -> List<String>,
    private val setArgumentValue: CommonToolArguments.Builder.(T) -> Unit,
    private val getArgumentValue: CommonToolArguments.() -> T,
) : ArgumentTestDescriptor<T> {
    override fun getValueString(argument: T?): String? = valueString(argument)

    override fun expectedArgumentStringsFor(value: String): List<String> = expectedArgumentStringsFor.invoke(value)

    fun setArgument(arguments: CommonToolArguments.Builder, value: T) {
        arguments.setArgumentValue(value)
    }

    fun getArgument(arguments: CommonToolArguments): T = arguments.getArgumentValue()
}
