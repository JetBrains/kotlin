/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.compilation

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile

/**
 * Compiles the given [file] in-memory using the specified [options].
 *
 * The function rethrows exceptions from the compiler, wrapped in [KaCodeCompilationException]. The implementation should wrap the
 * `compile()` call into a `try`/`catch` block when necessary.
 *
 * @param file A file to compile.
 *  The file must be either a source module file, or a [KtCodeFragment].
 *  For a [KtCodeFragment], a source module context, a compiled library source context, or an empty context (`null`) are supported.
 *
 * @param options The compilation options created via [createCompilationOptions].
 *
 * @see createCompilationOptions
 */
@KaExperimentalApi
@Throws(KaCodeCompilationException::class)
context(session: KaSession)
public fun compile(file: KtFile, options: KaCompilationOptions): KaCompilationResult {
    @OptIn(KaImplementationDetail::class)
    return internals.compilerFacility.compile(file, options)
}

/**
 * Creates a new [KaCompilationOptions] instance using the given DSL [init] block.
 *
 * Example usage:
 * ```kotlin
 * val options = createCompilationOptions {
 *     target(KaCompilationTarget.JVM)
 *     moduleName("myModule")
 *     languageVersionSettings(languageSettings)
 *     allowedErrorFilter { diagnostic -> diagnostic.factoryName in allowedErrors }
 * }
 * ```
 *
 * @param init A lambda that configures the [KaCompilationOptionsBuilder].
 */
@KaExperimentalApi
context(session: KaSession)
public fun createCompilationOptions(init: KaCompilationOptionsBuilder.() -> Unit): KaCompilationOptions {
    @OptIn(KaImplementationDetail::class)
    return internals.compilerFacility.createCompilationOptions(init)
}

/**
 * Creates a copy of these [KaCompilationOptions], applying the given [init] modifications.
 *
 * Options not explicitly overridden in [init] retain their original values.
 *
 * @param init A lambda that configures the [KaCompilationOptionsBuilder] with modifications to apply.
 */
@KaExperimentalApi
context(session: KaSession)
public fun KaCompilationOptions.modify(init: KaCompilationOptionsBuilder.() -> Unit): KaCompilationOptions {
    @OptIn(KaImplementationDetail::class)
    return internals.compilerFacility.modify(this, init)
}
