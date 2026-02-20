/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.arguments.description.actualJvmCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.tooling.core.toKotlinVersion
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

abstract class BaseArgumentTest<T>(val argumentName: String) : BaseCompilationTest() {

    abstract fun expectedArgumentStringsFor(value: String?, compilerVersion: String): List<String>

    abstract fun getValueString(argument: T?): String?

    protected fun getDefaultValueString(
        compilerVersion: String,
    ): String? {
        val argument =
            actualJvmCompilerArguments.arguments.firstOrNull { it.name == argumentName }
                ?: error("Argument '$argumentName' not found.")

        val kotlinToolingVersion = KotlinToolingVersion(compilerVersion)
        return argument.defaultValueString(kotlinToolingVersion.toKotlinReleaseVersion())
    }

    private fun KotlinToolingVersion.toKotlinReleaseVersion(): KotlinReleaseVersion {
        val kotlinVersionString = toKotlinVersion().toString()

        return try {
            KotlinReleaseVersion.valueOf(kotlinVersionString)
        } catch (_: IllegalArgumentException) {
            parseLastKotlinReleaseVersion(kotlinVersionString)
        }
    }

    private fun parseLastKotlinReleaseVersion(kotlinVersionString: String): KotlinReleaseVersion {
        val baseVersion = kotlinVersionString.split("-", limit = 2)[0]

        val baseVersionSplit = baseVersion.split(".")

        val majorVersion =
            baseVersionSplit[0].toIntOrNull() ?: error("Invalid Kotlin version: $kotlinVersionString (Failed parsing major version)")
        val minorVersion =
            baseVersionSplit.getOrNull(1)?.toIntOrNull()
                ?: error("Invalid Kotlin version: $kotlinVersionString (Failed parsing minor version)")
        val patchVersion = baseVersionSplit.getOrNull(2)?.toIntOrNull() ?: 0

        return KotlinReleaseVersion.entries.last { releaseVersion ->
            releaseVersion.major < majorVersion ||
                    (releaseVersion.major == majorVersion && releaseVersion.minor < minorVersion) ||
                    (releaseVersion.major == majorVersion && releaseVersion.minor == minorVersion && releaseVersion.patch <= patchVersion)
        }
    }

    private fun KotlinCompilerArgument.defaultValueString(kotlinReleaseVersion: KotlinReleaseVersion): String? {
        @Suppress("UNCHECKED_CAST")
        val argumentType = argumentType as KotlinArgumentValueType<Any>

        var defaultValue = argumentType.defaultValue.current
        for ((range, value) in argumentType.defaultValue.valueInVersions) {
            if (kotlinReleaseVersion in range) {
                defaultValue = value
            }
        }

        return argumentType.stringRepresentation(defaultValue)?.removeSurrounding("\"")
    }
}