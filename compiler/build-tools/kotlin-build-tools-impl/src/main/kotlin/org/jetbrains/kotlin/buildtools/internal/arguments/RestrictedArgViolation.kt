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

    /** The argument usage should be reported as a warning to the user. */
    @Serializable
    data class Warning(override val message: String) : RestrictedArgViolation()

    /** The argument usage should cause a build failure. */
    @Serializable
    data class Error(override val message: String) : RestrictedArgViolation()
}

