/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.projectStructure.KaJvmTarget
import org.jetbrains.kotlin.config.*

/**
 * Whether unbound IR symbols should be stubbed instead of linked.
 *
 * This should be enabled if the compiled file could refer to symbols defined in another file of the same module.
 * Such symbols are not compiled (only the file is passed to the backend) and so they cannot be linked from a dependency.
 */
@KaImplementationDetail
val STUB_UNBOUND_IR_SYMBOLS: CompilerConfigurationKey<Boolean> = CompilerConfigurationKey("stub unbound IR symbols")

@KaImplementationDetail
class KaBaseCompilationOptions(
    override val token: KaLifetimeToken,
    val configuration: CompilerConfiguration,
    val target: KaCompilationTarget?,
    val allowedErrorFilter: (KaDiagnostic) -> Boolean,
    val compiledClassHandler: KaCompiledClassHandler?,
    val jvmExecutionStack: Sequence<PsiElement?>?,
    val jvmOutputAsmListing: Boolean,
) : KaCompilationOptions {
    fun copy(init: KaCompilerOptionsBuilder.() -> Unit): KaCompilationOptions {
        return KaBaseCompilerOptionsBuilder(token, configuration.copy()).apply {
            target?.let { originalTarget ->
                target(originalTarget)
            }

            allowedErrorFilter(allowedErrorFilter)

            if (compiledClassHandler != null) {
                jvmCompiledClassHandler(compiledClassHandler)
            }

            if (jvmExecutionStack != null) {
                jvmExecutionStack(jvmExecutionStack)
            }

            jvmOutputAsmListing(jvmOutputAsmListing)

            init()
        }.build()
    }
}

@KaImplementationDetail
class KaBaseCompilerOptionsBuilder(val token: KaLifetimeToken, val configuration: CompilerConfiguration) : KaCompilerOptionsBuilder {
    init {
        require(!configuration.isReadOnly) { "The given configuration is read-only" }
    }

    private var targetValue: KaCompilationTarget? = null
    private var allowedErrorFilterValue: (KaDiagnostic) -> Boolean = { false }
    private var compiledClassHandlerValue: KaCompiledClassHandler? = null
    private var jvmExecutionStackValue: Sequence<PsiElement?>? = null
    private var jvmOutputAsmListingValue: Boolean = false

    fun build(): KaBaseCompilationOptions {
        return KaBaseCompilationOptions(
            token = token,
            configuration = configuration,
            target = targetValue,
            allowedErrorFilter = allowedErrorFilterValue,
            compiledClassHandler = compiledClassHandlerValue,
            jvmExecutionStack = jvmExecutionStackValue,
            jvmOutputAsmListing = jvmOutputAsmListingValue,
        )
    }

    override fun target(value: KaCompilationTarget) {
        targetValue = value
    }

    override fun moduleName(value: String) {
        configuration.put(CommonConfigurationKeys.MODULE_NAME, value)
    }

    override fun moduleActualizer(value: KaCompilerFacilityModuleActualizer) {
        configuration.put(MODULE_ACTUALIZER, value)
    }

    override fun languageVersionSettings(value: LanguageVersionSettings) {
        configuration.put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, value)
    }

    override fun allowedErrorFilter(value: (KaDiagnostic) -> Boolean) {
        allowedErrorFilterValue = value
    }

    override fun codeFragmentClassName(value: String) {
        configuration.put(CODE_FRAGMENT_CLASS_NAME, value)
    }

    override fun codeFragmentMethodName(value: String) {
        configuration.put(CODE_FRAGMENT_METHOD_NAME, value)
    }

    override fun jvmTarget(value: KaJvmTarget) {
        val jvmTarget = JvmTarget.fromString(value.name)
            ?: error("Unsupported JVM target: ${value.name}")
        configuration.put(JVMConfigurationKeys.JVM_TARGET, jvmTarget)
    }

    override fun jvmCompiledClassHandler(value: KaCompiledClassHandler) {
        compiledClassHandlerValue = value
    }

    @KaNonPublicApi
    override fun jvmOutputAsmListing(value: Boolean) {
        jvmOutputAsmListingValue = value
    }

    @KaIdeApi
    override fun stubUnboundIrSymbols(value: Boolean) {
        configuration.put(STUB_UNBOUND_IR_SYMBOLS, value)
    }

    @KaIdeApi
    override fun disableInline(value: Boolean) {
        configuration.put(CommonConfigurationKeys.DISABLE_INLINE, value)
    }

    @KaIdeApi
    override fun disableCallAssertions(value: Boolean) {
        configuration.put(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS, value)
    }

    @KaIdeApi
    override fun disableOptimization(value: Boolean) {
        configuration.put(JVMConfigurationKeys.DISABLE_OPTIMIZATION, value)
    }

    @KaIdeApi
    override fun disableParameterAssertions(value: Boolean) {
        configuration.put(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS, value)
    }

    @KaIdeApi
    override fun ignoreConstOptimizationErrors(value: Boolean) {
        configuration.put(CommonConfigurationKeys.IGNORE_CONST_OPTIMIZATION_ERRORS, value)
    }

    @KaIdeApi
    override fun jvmExecutionStack(value: Sequence<PsiElement?>) {
        jvmExecutionStackValue = value
    }

    @KaIdeApi
    override fun jvmGenerateParameterMetadata(value: Boolean) {
        configuration.put(JVMConfigurationKeys.PARAMETERS_METADATA, value)
    }

    @KaIdeApi
    override fun jvmUseInvokeDynamicForSamConversions(value: Boolean) {
        val scheme = if (value) JvmClosureGenerationScheme.INDY else JvmClosureGenerationScheme.CLASS
        configuration.put(JVMConfigurationKeys.SAM_CONVERSIONS, scheme)
    }

    @KaIdeApi
    override fun jvmUseInvokeDynamicForLambdas(value: Boolean) {
        val scheme = if (value) JvmClosureGenerationScheme.INDY else JvmClosureGenerationScheme.CLASS
        configuration.put(JVMConfigurationKeys.LAMBDAS, scheme)
    }

    @KaIdeApi
    override fun jvmLinkViaSignatures(value: Boolean) {
        configuration.put(JVMConfigurationKeys.LINK_VIA_SIGNATURES, value)
    }
}