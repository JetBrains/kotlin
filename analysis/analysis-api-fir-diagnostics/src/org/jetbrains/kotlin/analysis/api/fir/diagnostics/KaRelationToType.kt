/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

/**
 * The way a declaration which restricts the visibility of a type relates to that type.
 */
@KaUnstableDiagnosticApi
public enum class KaRelationToType {
    /** The declaration is the classifier of the type. */
    CONSTRUCTOR,

    /** The declaration contains the classifier of the type. */
    CONTAINER,

    /** The declaration is the classifier of a type argument. */
    ARGUMENT,

    /** The declaration contains the classifier of a type argument. */
    ARGUMENT_CONTAINER,
}
