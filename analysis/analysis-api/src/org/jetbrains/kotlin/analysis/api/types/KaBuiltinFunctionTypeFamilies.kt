/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail

/**
 * Provides access to the four built-in [function type families][KaFunctionTypeFamily].
 *
 * @see builtinFunctionTypeFamilies
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaBuiltinFunctionTypeFamilies {
    /**
     * The `Function` family representing regular function types
     * (e.g., `Function0`, `Function1`, ..., `FunctionN`).
     */
    public val function: KaFunctionTypeFamily

    /**
     * The `SuspendFunction` family representing suspend function types
     * (e.g., `SuspendFunction0`, `SuspendFunction1`, ..., `SuspendFunctionN`).
     */
    public val suspendFunction: KaFunctionTypeFamily

    /**
     * The `KFunction` family representing reflection types for regular functions
     * (e.g., `KFunction0`, `KFunction1`, ..., `KFunctionN`).
     */
    public val kFunction: KaFunctionTypeFamily

    /**
     * The `KSuspendFunction` family representing reflection types for suspend functions
     * (e.g., `KSuspendFunction0`, `KSuspendFunction1`, ..., `KSuspendFunctionN`).
     */
    public val kSuspendFunction: KaFunctionTypeFamily
}
