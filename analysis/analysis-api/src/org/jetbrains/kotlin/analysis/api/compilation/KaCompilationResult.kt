/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.compilation

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.compile.KaCodeFragmentCapturedValue
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic

/**
 * An in-memory compilation result returned from [compile].
 *
 * Compilation fails if there are critical errors reported either on the frontend or on the backend side.
 * Keep in mind that [KaCompilationResult] is a part of the Analysis API, so it should only be used inside an
 * [analysis block][org.jetbrains.kotlin.analysis.api.session.analyze].
 */
@KaExperimentalApi
public sealed class KaCompilationResult(
    /** A list of exceptions that were thrown during compilation but workaround somehow */
    public val mutedExceptions: List<Throwable>,
) {
    /**
     * A successful compilation result.
     *
     * @property output Output files produced by the compiler. For the JVM target, these are class files and '.kotlin_module'.
     * @property capturedValues Context values captured by a [KtCodeFragment][org.jetbrains.kotlin.psi.KtCodeFragment]. Empty for an ordinary [KtFile][org.jetbrains.kotlin.psi.KtFile].
     * @property canBeCached When the flag is raised, this compilation result is safe to cache and avoid re-compilation
     *  Suppose we have the following code and evaluating `T::class`
     *
     *  inline fun <reified T> foo() {
     *      //Breakpoint!
     *      println()
     *  }
     *
     *  fun main() {
     *      foo<Int>()
     *      foo<String>()
     *  }
     *
     *  We should emit different bytecode in <Int> and <String> calls, yet we are at the same line and compiling the same code.
     */
    @KaExperimentalApi
    public class Success(
        public val output: List<KaCompiledFile>,
        public val capturedValues: List<KaCodeFragmentCapturedValue>,
        public var canBeCached: Boolean,
        mutedExceptions: List<Throwable> = emptyList(),
    ) : KaCompilationResult(mutedExceptions)

    /**
     * A failed compilation result.
     *
     * @property errors Non-recoverable errors which occurred either during code analysis or during code generation.
     */
    @KaExperimentalApi
    public class Failure(
        public val errors: List<KaDiagnostic>,
        mutedExceptions: List<Throwable> = emptyList(),
    ) : KaCompilationResult(mutedExceptions)
}
