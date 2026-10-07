/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.extensions.ExtensionPointDescriptor

/**
 * Allows compiling code that has frontend errors (Eclipse-style "proceed on error").
 *
 * If at least one extension is registered, the CLI pipeline doesn't stop after the frontend reports errors.
 * Instead, the error diagnostics are removed from the diagnostics collector (and reported as warnings), and
 * the extension computes [FirErroneousCodePlan] which tells FIR2IR which bodies must not be converted.
 * Such bodies are replaced with an `IrErrorExpression`, which must be lowered by the extension's own
 * IR generation extension (e.g. into a `throw`) before code generation.
 */
abstract class FirErrorTolerantCompilationExtension {
    companion object : ExtensionPointDescriptor<FirErrorTolerantCompilationExtension>(
        "org.jetbrains.kotlin.fir.errorTolerantCompilationExtension",
        FirErrorTolerantCompilationExtension::class.java
    )

    /**
     * @param files all source files of the compiled module(s)
     * @param errorsByFile all diagnostics with error severity which were reported by the frontend, grouped by
     *  [FirFile.sourceFile] (the `null` key holds the diagnostics without source)
     */
    abstract fun computeErroneousCodePlan(
        files: List<FirFile>,
        errorsByFile: Map<KtSourceFile?, List<KtDiagnostic>>,
    ): FirErroneousCodePlan
}

interface FirErroneousCodePlan {
    /**
     * Returns a non-null message if the "body" of [declaration] must not be converted to IR and should be replaced with an error
     * expression with this message instead.
     *
     * [declaration] might be a function (including constructors and property accessors), a property (its initializer or delegate),
     * a value parameter (its default value) or an anonymous initializer.
     */
    fun getStubMessage(declaration: FirDeclaration): String?
}

val ERRONEOUS_CODE_PLAN: CompilerConfigurationKey<FirErroneousCodePlan> =
    CompilerConfigurationKey.create("plan of erroneous declarations to be stubbed by FIR2IR")
