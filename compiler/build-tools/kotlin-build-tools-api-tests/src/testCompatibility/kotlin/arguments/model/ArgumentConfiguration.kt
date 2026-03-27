/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments.model

import org.jetbrains.kotlin.arguments.description.actualCommonCompilerArguments
import org.jetbrains.kotlin.arguments.description.actualJvmCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.ExperimentalArgumentApi
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.CommonCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.JvmCompilerArgument
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.tooling.core.toKotlinVersion

internal interface ArgumentDescriptor<T> {
    val argumentName: String
    val argumentValues: List<T>
    val isEnum: Boolean
    val isNullable: Boolean
    val valueString: (T?) -> String?
    val expectedArgumentStringsFor: (String) -> List<String>
}

internal abstract class BaseArgumentConfiguration<T>(
    val kotlinToolchain: KotlinToolchains,
    private val argumentDescription: ArgumentDescriptor<T>,
) {
    private val argumentName: String = argumentDescription.argumentName

    fun getValueString(argument: T?): String? = argumentDescription.valueString(argument)

    fun expectedArgumentStringsFor(value: String?): List<String> {
        if (value == null || value == getDefaultValueString()) {
            return emptyList()
        }

        return argumentDescription.expectedArgumentStringsFor(value)
    }

    fun getDefaultValueString(): String? {
        val argument = actualJvmCompilerArguments.arguments.firstOrNull { it.name == argumentName }
            ?: actualCommonCompilerArguments.arguments.firstOrNull { it.name == argumentName }
            ?: error("Argument '$argumentName' not found.")

        val kotlinToolingVersion = KotlinToolingVersion(kotlinToolchain.getCompilerVersion())
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
        val minorVersion = baseVersionSplit.getOrNull(1)?.toIntOrNull()
            ?: error("Invalid Kotlin version: $kotlinVersionString (Failed parsing minor version)")
        val patchVersion = baseVersionSplit.getOrNull(2)?.toIntOrNull() ?: 0

        return KotlinReleaseVersion.entries.last { releaseVersion ->
            releaseVersion.major < majorVersion || (releaseVersion.major == majorVersion && releaseVersion.minor < minorVersion) || (releaseVersion.major == majorVersion && releaseVersion.minor == minorVersion && releaseVersion.patch <= patchVersion)
        }
    }

    @OptIn(ExperimentalArgumentApi::class)
    private fun KotlinCompilerArgument.defaultValueString(kotlinReleaseVersion: KotlinReleaseVersion): String? {
        @Suppress("UNCHECKED_CAST") val argumentType = argumentType as KotlinArgumentValueType<Any>

        var defaultValue = argumentType.defaultValue.current
        for ((range, value) in argumentType.defaultValue.valueInVersions) {
            if (kotlinReleaseVersion in range) {
                defaultValue = value
            }
        }

        return argumentType.stringRepresentation(defaultValue)?.removeSurrounding("\"")
    }
}

internal class JvmArgumentConfiguration<T>(
    kotlinToolchain: KotlinToolchains,
    argumentDescription: JvmArgumentDescriptor<T>,
) : BaseArgumentConfiguration<T>(kotlinToolchain, argumentDescription) {
    val argumentKey: JvmCompilerArgument<T> = argumentDescription.argumentKey
    val argumentValues: List<T> = argumentDescription.argumentValues
}

internal class CommonArgumentConfiguration<T>(
    kotlinToolchain: KotlinToolchains,
    argumentDescription: CommonArgumentDescriptor<T>,
) : BaseArgumentConfiguration<T>(kotlinToolchain, argumentDescription) {
    val argumentKey: CommonCompilerArgument<T> = argumentDescription.argumentKey
    val argumentValues: List<T> = argumentDescription.argumentValues
}