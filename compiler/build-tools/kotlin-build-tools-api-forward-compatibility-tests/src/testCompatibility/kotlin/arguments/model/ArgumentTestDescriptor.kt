/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments.model

import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion

internal interface ArgumentTestDescriptor<T> {
    val argumentName: String
    val argument: Any
    val availableSinceVersion: KotlinReleaseVersion

    val argumentValues: List<T>

    val runsEnumTest: Boolean
    val runsNullableTest: Boolean
    val skipBtaV1: Boolean

    fun getValueString(argument: T?): String?
    fun expectedArgumentStringsFor(value: String): List<String>
}
