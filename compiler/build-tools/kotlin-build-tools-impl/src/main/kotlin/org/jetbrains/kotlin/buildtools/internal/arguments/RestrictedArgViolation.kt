/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import kotlinx.serialization.Serializable

/**
 * Represents a violation of a restricted compiler argument detected during
 * [applyArgumentStrings][org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments.Builder.applyArgumentStrings] or
 * [applyCompilerArguments][org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments.Builder.applyCommandLineArguments].
 */
@Serializable
internal sealed class RestrictedArgViolation {
    abstract val message: String

    /**
     * The only compiler phase the argument is restricted in, or `null` if the argument is restricted regardless of the phase.
     *
     * The arguments are not aware of the operation they are used by, so such violations are collected unconditionally
     * and filtered out by the operations performing a different phase.
     */
    abstract val phase: CompilerPhase?

    /** The argument usage should be reported as a warning to the user. */
    @Serializable
    data class Warning(override val message: String, override val phase: CompilerPhase? = null) : RestrictedArgViolation()

    /** The argument usage should cause a build failure. */
    @Serializable
    data class Error(override val message: String, override val phase: CompilerPhase? = null) : RestrictedArgViolation()
}

/**
 * The phase of the compilation pipeline of the klib-based platforms performed by an operation.
 */
@Serializable
internal enum class CompilerPhase {
    KLIB_COMPILATION,
    BACKEND_COMPILATION,
}
