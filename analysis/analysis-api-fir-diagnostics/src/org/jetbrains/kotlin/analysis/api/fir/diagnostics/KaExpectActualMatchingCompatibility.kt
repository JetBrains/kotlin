/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

/**
 * The result of matching an `expect` callable against an `actual` one.
 *
 * All entries except [MATCHED_SUCCESSFULLY] are mismatches.
 */
@KaUnstableDiagnosticApi
public enum class KaExpectActualMatchingCompatibility {
    /** One declaration is a function, while the other is a property. */
    CALLABLE_KIND,

    /** The `actual` declaration is a Java field. */
    ACTUAL_JAVA_FIELD,

    /** One declaration is an extension, while the other is not. */
    PARAMETER_SHAPE,

    /** The number of value parameters is different. */
    PARAMETER_COUNT,

    /** The number of context parameters is different. */
    CONTEXT_PARAMETER_COUNT,

    /** The number of type parameters is different. */
    FUNCTION_TYPE_PARAMETER_COUNT,

    /** The value parameter types are different. */
    PARAMETER_TYPES,

    /** The value parameter names are different. */
    PARAMETER_NAMES,

    /** The context parameter types are different. */
    CONTEXT_PARAMETER_TYPES,

    /** The upper bounds of type parameters are different. */
    FUNCTION_TYPE_PARAMETER_UPPER_BOUNDS,

    /** The declarations match. */
    MATCHED_SUCCESSFULLY,
}
