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

    /**
     * Valid typed values.
     */
    val argumentValues: List<T>

    /**
     * Raw CLI string forms of [argumentValues]. Each string must round-trip: parsing it via
     * `applyArgumentStrings` produces a typed value whose [getValueString] returns the same string.
     */
    val argumentRawValues: List<String>

    /**
     * Typed values whose assignment must throw `CompilerArgumentsParseException`. Use for
     * structurally invalid values (e.g., a path containing the platform path separator inside a
     * single path-list entry).
     */
    val invalidArgumentValues: List<T>
    val runsInvalidArgumentValueTest: Boolean
        get() = invalidArgumentValues.isNotEmpty()

    /**
     * Raw CLI strings that must be rejected as a parse error. Typical use: a non-existent enum
     * entry for enum-backed arguments.
     */
    val invalidRawValues: List<String>
    val runsInvalidRawValueTest: Boolean
        get() = invalidRawValues.isNotEmpty()

    /**
     * `true` if the generated `*CompilerArgument<T>` is parameterized with a nullable `T?`, so
     * `null` is a legal assignment meaning "do not pass the flag to the compiler".
     */
    val runsNullableTest: Boolean

    /**
     * `true` if this argument's `*CompilerArguments.*` constant exists only in the v2 BTA and
     * is not reachable through the v1 adapter (`KotlinToolchainsV1Adapter`); the `[v1]` toolchain
     * parametrization will be dropped for it. Independent of [availableSinceVersion] (compiler
     * version, not API version).
     */
    val skipBtaV1: Boolean

    fun getValueString(argument: T?): String?
    fun expectedArgumentStringsFor(value: String): List<String>
}
