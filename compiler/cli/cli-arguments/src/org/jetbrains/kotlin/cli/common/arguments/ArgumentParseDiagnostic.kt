/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

sealed class ArgumentParseDiagnostic(val isFatal: Boolean = false) {
    class ArgfileError(val message: String) :
        ArgumentParseDiagnostic(isFatal = true)

    class ArgumentWithoutValue(val argument: String) :
        ArgumentParseDiagnostic(isFatal = true)

    class BooleanArgumentWithIncorrectValue(val argument: String) :
        ArgumentParseDiagnostic(isFatal = true)

    class BooleanLanguageFeatureArgumentWithValue(val argument: String) :
        ArgumentParseDiagnostic(isFatal = true)

    class StringLanguageFeatureArgumentWithIncorrectValue(val argument: String, val value: String, val allowedValues: Collection<String>) :
        ArgumentParseDiagnostic(isFatal = true)

    class InvalidArgument(val argument: String) :
        ArgumentParseDiagnostic(isFatal = true)

    class UnknownExtraFlag(val argument: String) :
        ArgumentParseDiagnostic()

    class ExtraArgumentInObsoleteForm(val argument: String) :
        ArgumentParseDiagnostic()

    class ArgumentWithDeprecatedName(val deprecatedName: String, val newName: String) :
        ArgumentParseDiagnostic()

    class InternalArgumentError(val message: String) :
        ArgumentParseDiagnostic()

    class InternalArgumentWarning(val message: String) :
        ArgumentParseDiagnostic()
}
