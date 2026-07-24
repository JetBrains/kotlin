/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.compilation

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.projectStructure.KaJvmTarget
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.psi.KtCodeFragment

/**
 * A DSL builder for [KaCompilationOptions].
 *
 * Provides methods to configure the compilation target, language settings, JVM-specific options, error handling, and code fragment
 * parameters. Instances are created internally by [createCompilationOptions] and [modify].
 *
 * @see KaCompilationOptions
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaCompilationOptionsBuilder : KaLifetimeOwner {
    /** Sets the target platform for compilation. Must be provided. */
    public fun target(value: KaCompilationTarget)

    /** Sets the module name used for the compiled output. */
    public fun moduleName(value: String)

    /**
     * Sets a custom actualizer for common source modules in multiplatform projects.
     *
     * @see KaCompilerFacilityModuleActualizer
     */
    public fun moduleActualizer(value: KaCompilerFacilityModuleActualizer)

    /** Sets the language version settings used during compilation. */
    public fun languageVersionSettings(value: LanguageVersionSettings)

    /**
     * Sets a filter for allowed errors. Compilation will be aborted if there are errors that this filter rejects.
     *
     * A filter returning `true` means the error is allowed and will not abort compilation.
     * Defaults to rejecting all errors.
     */
    public fun allowedErrorFilter(value: (KaDiagnostic) -> Boolean)

    /**
     * Sets the simple class name for the code fragment facade class.
     * Only relevant when compiling [KtCodeFragment] files.
     */
    public fun codeFragmentClassName(value: String)

    /**
     * Sets the entry point method name for the code fragment.
     * Only relevant when compiling [KtCodeFragment] files.
     */
    public fun codeFragmentMethodName(value: String)

    /** Sets the JVM bytecode target version. */
    public fun jvmTarget(value: KaJvmTarget)

    /**
     * Sets a handler which is called whenever a new class file is produced.
     *
     * @see KaCompiledClassHandler
     */
    public fun jvmCompiledClassHandler(value: KaCompiledClassHandler)

    /** Enables output of ASM bytecode listing. When `true`, selects the test-mode class builder factory. */
    @KaNonPublicApi
    public fun jvmOutputAsmListing(value: Boolean)

    /**
     * Whether unbound IR symbols should be stubbed instead of linked.
     *
     * This should be enabled if the compiled file could refer to symbols defined in another file of the same module.
     */
    @KaIdeApi
    public fun stubUnboundIrSymbols(value: Boolean)

    /** Disables inlining of `inline` functions. */
    @KaIdeApi
    public fun disableInline(value: Boolean)

    /** Disables null-check assertions on receiver and arguments of platform-typed calls. */
    @KaIdeApi
    public fun disableCallAssertions(value: Boolean)

    /** Disables optimizations in generated code. */
    @KaIdeApi
    public fun disableOptimization(value: Boolean)

    /** Disables null-check assertions on parameters. */
    @KaIdeApi
    public fun disableParameterAssertions(value: Boolean)

    /** Ignores errors from constant expression optimization. */
    @KaIdeApi
    public fun ignoreConstOptimizationErrors(value: Boolean)

    /**
     * Sets the execution stack for debugger code fragment compilation.
     *
     * A sequence of PSI elements representing function calls or property accesses in the current execution stack,
     * listed from the top to the bottom.
     */
    @KaIdeApi
    public fun jvmExecutionStack(value: Sequence<PsiElement?>)

    /** Enables generation of parameter metadata (names and access flags) in class files. */
    @KaIdeApi
    public fun jvmGenerateParameterMetadata(value: Boolean)

    /** Uses `invokedynamic` for SAM conversions instead of generating anonymous classes. */
    @KaIdeApi
    public fun jvmUseInvokeDynamicForSamConversions(value: Boolean)

    /** Uses `invokedynamic` for lambdas instead of generating anonymous classes. */
    @KaIdeApi
    public fun jvmUseInvokeDynamicForLambdas(value: Boolean)
}
