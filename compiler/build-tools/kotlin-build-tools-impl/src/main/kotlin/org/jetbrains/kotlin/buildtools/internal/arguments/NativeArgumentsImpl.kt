/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.arguments

import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.internal.DeepCopyable
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.copyK2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.copyOf
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArgumentsAllErrors
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings

internal class NativeArgumentsImpl private constructor(
    private val nativeArguments: K2NativeCompilerArguments,
    argumentValidationErrors: Set<String>,
    restrictedArgViolations: List<RestrictedArgViolation>,
    argumentParseDiagnostics: ArgumentParseDiagnostics,
) : CommonCompilerArgumentsImpl(argumentValidationErrors, restrictedArgViolations, argumentParseDiagnostics),
    DeepCopyable<NativeArgumentsImpl> {

    constructor() : this(K2NativeCompilerArguments(), emptySet(), emptyList(), ArgumentParseDiagnostics()) {
        applyCompilerArguments(nativeArguments)
    }

    override fun deepCopy(): NativeArgumentsImpl =
        NativeArgumentsImpl(
            nativeArguments.copyOf(),
            argumentValidationErrors.toSet(),
            restrictedArgViolations.toList(),
            argumentParseDiagnostics.copy(),
        ).also { newArgs -> newArgs.applyCompilerArguments(toCompilerArguments()) }

    override fun build(): NativeArgumentsImpl = deepCopy()

    fun toCompilerArguments(): K2NativeCompilerArguments {
        val arguments = nativeArguments.copyOf()
        super.toCompilerArguments(arguments)
        arguments.internalArguments = parseCommandLineArguments<K2NativeCompilerArguments>(internalArguments.toList()).internalArguments
        populateExplicitArguments(arguments)
        return arguments
    }

    @Deprecated(
        message = "This method is deprecated. Use applyCommandLineArguments instead.",
        level = DeprecationLevel.WARNING,
    )
    override fun applyArgumentStrings(arguments: List<String>) {
        val compilerArgs: K2NativeCompilerArguments = parseCommandLineArguments(arguments)
        collectRestrictedArgViolations(compilerArgs, K2NativeCompilerArguments())
        validateArgumentsAllErrors(compilerArgs.errors).forEach { _argumentValidationErrors.add(it) }
        argumentParseDiagnostics.record(compilerArgs, arguments) { toCompilerArguments() }
        applyParsedArguments(compilerArgs)
    }

    @DelicateBuildToolsApi
    override fun applyCommandLineArguments(arguments: List<String>) {
        val compilerArgs = toCompilerArguments()
        parseCommandLineArguments(arguments, compilerArgs, false)
        handleCustomPluginArguments(this, compilerArgs)
        collectRestrictedArgViolations(compilerArgs, K2NativeCompilerArguments())
        validateArgumentsAllErrors(compilerArgs.errors).forEach { _argumentValidationErrors.add(it) }
        argumentParseDiagnostics.record(compilerArgs, arguments) { toCompilerArguments() }
        applyParsedArguments(compilerArgs)
    }

    private fun applyParsedArguments(compilerArgs: K2NativeCompilerArguments) {
        // replaces the content of nativeArguments, so that it can stay a `val`
        copyK2NativeCompilerArguments(compilerArgs, nativeArguments)
        applyCompilerArguments(compilerArgs)
    }

    override fun toArgumentStrings(): List<String> = toCompilerArguments().compilerToArgumentStrings(allowArgFileInValues = false)
}
