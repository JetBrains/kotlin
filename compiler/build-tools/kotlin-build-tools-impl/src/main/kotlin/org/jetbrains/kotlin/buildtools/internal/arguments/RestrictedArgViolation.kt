/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.`internal`.arguments

/**
 * Represents a violation of a restricted compiler argument detected during [applyArgumentStrings][org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments.applyArgumentStrings].
 */
internal sealed class RestrictedArgViolation {
    abstract val message: String

    /** The argument usage should be reported as a warning to the user. */
    data class Warning(override val message: String) : RestrictedArgViolation()

    /** The argument usage should cause a build failure. */
    data class Error(override val message: String) : RestrictedArgViolation()
}

