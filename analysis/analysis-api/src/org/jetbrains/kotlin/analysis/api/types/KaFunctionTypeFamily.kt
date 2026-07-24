/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.name.ClassId

/**
 * Describes a family of numbered function types such as `Function0`, `Function1`, ..., `FunctionN`.
 *
 * Kotlin has the following built-in function type families:
 * - `Function` — regular function types, e.g., `(Int) -> String`
 * - `SuspendFunction` — suspend function types, e.g., `suspend () -> Unit`
 * - `KFunction` — reflection types for regular functions
 * - `KSuspendFunction` — reflection types for suspend functions
 *
 * Compiler plugins may introduce additional custom function type families.
 *
 * @see functionTypeFamily
 * @see KaBuiltinFunctionTypeFamilies
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaFunctionTypeFamily {
    /**
     * Whether this family represents reflection function types (`KFunction`, `KSuspendFunction`).
     */
    public val isReflect: Boolean

    /**
     * Whether this family represents suspend function types (`SuspendFunction`, `KSuspendFunction`).
     */
    public val isSuspend: Boolean

    /**
     * Whether function types in this family can be inlined by the compiler.
     *
     * For built-in families, `Function` and `SuspendFunction` are inlinable, while `KFunction` and `KSuspendFunction` are not.
     */
    public val isInlinable: Boolean

    /**
     * The maximum number of parameters supported by function types in this family.
     */
    public val maxArity: Int

    /**
     * Whether function references with a simple function type (e.g., `Function0`, `KFunction0`) can be converted to this family.
     */
    public val supportsConversionFromSimpleFunctionType: Boolean

    /**
     * The class name prefix shared by all types in this family.
     *
     * For example, `"Function"` for the `Function` family, `"SuspendFunction"` for the `SuspendFunction` family.
     */
    public val nameBase: String

    /**
     * Returns the [ClassId] of the function type interface for the given [arity].
     *
     * For example, `classId(2)` on the `Function` family returns the [ClassId] for `kotlin.Function2`.
     */
    public fun classId(arity: Int): ClassId
}
