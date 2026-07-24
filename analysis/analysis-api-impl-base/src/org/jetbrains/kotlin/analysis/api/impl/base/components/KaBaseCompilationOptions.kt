/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.compilation.KaCompilationTarget
import org.jetbrains.kotlin.analysis.api.compilation.KaCompiledClassHandler
import org.jetbrains.kotlin.analysis.api.compilation.KaCompilerFacilityModuleActualizer
import org.jetbrains.kotlin.analysis.api.components.KaCompilationOptions
import org.jetbrains.kotlin.analysis.api.components.KaCompilationOptionsBuilder
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.projectStructure.KaJvmTarget
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.analysis.api.components.KaCompilationTarget as KaLegacyCompilationTarget
import org.jetbrains.kotlin.analysis.api.components.KaCompiledClassHandler as KaLegacyCompiledClassHandler
import org.jetbrains.kotlin.analysis.api.components.KaCompilerFacilityModuleActualizer as KaLegacyCompilerFacilityModuleActualizer

@KaImplementationDetail
class KaBaseCompilationOptions(
    override val token: KaLifetimeToken,
    val configuration: CompilerConfiguration,
    val target: KaCompilationTarget?,
    val allowedErrorFilter: (KaDiagnostic) -> Boolean,
    val compiledClassHandler: KaCompiledClassHandler?,
    val jvmExecutionStack: Sequence<PsiElement?>?,
    val jvmOutputAsmListing: Boolean,
    val codeFragmentClassName: String?,
    val codeFragmentMethodName: String?,
    val moduleActualizer: KaCompilerFacilityModuleActualizer?,
    val stubUnboundIrSymbols: Boolean,
) : KaCompilationOptions {
    fun modify(init: KaCompilationOptionsBuilder.() -> Unit): KaCompilationOptions = withValidityAssertion {
        return KaBaseCompilationOptionsBuilder(token, configuration.copy()).apply {
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

            if (codeFragmentClassName != null) {
                codeFragmentClassName(codeFragmentClassName)
            }

            if (codeFragmentMethodName != null) {
                codeFragmentMethodName(codeFragmentMethodName)
            }

            if (moduleActualizer != null) {
                moduleActualizer(moduleActualizer)
            }

            stubUnboundIrSymbols(stubUnboundIrSymbols)

            init()
        }.build()
    }
}

@KaImplementationDetail
class KaBaseCompilationOptionsBuilder(
    override val token: KaLifetimeToken,
    val configuration: CompilerConfiguration,
) : KaCompilationOptionsBuilder {
    init {
        require(!configuration.isReadOnly) { "The given configuration is read-only" }
    }

    private var targetValue: KaCompilationTarget? = null
    private var allowedErrorFilterValue: (KaDiagnostic) -> Boolean = { false }
    private var compiledClassHandlerValue: KaCompiledClassHandler? = null
    private var jvmExecutionStackValue: Sequence<PsiElement?>? = null
    private var jvmOutputAsmListingValue: Boolean = false
    private var codeFragmentClassNameValue: String? = null
    private var codeFragmentMethodNameValue: String? = null
    private var moduleActualizerValue: KaCompilerFacilityModuleActualizer? = null
    private var stubUnboundIrSymbolsValue: Boolean = false

    fun build(): KaBaseCompilationOptions = withValidityAssertion {
        return KaBaseCompilationOptions(
            token = token,
            configuration = configuration,
            target = targetValue,
            allowedErrorFilter = allowedErrorFilterValue,
            compiledClassHandler = compiledClassHandlerValue,
            jvmExecutionStack = jvmExecutionStackValue,
            jvmOutputAsmListing = jvmOutputAsmListingValue,
            codeFragmentClassName = codeFragmentClassNameValue,
            codeFragmentMethodName = codeFragmentMethodNameValue,
            moduleActualizer = moduleActualizerValue,
            stubUnboundIrSymbols = stubUnboundIrSymbolsValue,
        )
    }

    override fun target(value: KaCompilationTarget) = withValidityAssertion {
        targetValue = value
    }

    /** Legacy overload routing the `components` target enum to the canonical `compilation` one. */
    override fun target(value: KaLegacyCompilationTarget) = withValidityAssertion {
        targetValue = value.toNewCompilationTarget()
    }

    override fun moduleName(value: String) = withValidityAssertion {
        configuration.put(CommonConfigurationKeys.MODULE_NAME, value)
    }

    override fun moduleActualizer(value: KaCompilerFacilityModuleActualizer) = withValidityAssertion {
        moduleActualizerValue = value
    }

    /** Legacy overload wrapping the `components` actualizer into the canonical `compilation` one. */
    override fun moduleActualizer(value: KaLegacyCompilerFacilityModuleActualizer) = withValidityAssertion {
        moduleActualizerValue = value.toNewModuleActualizer()
    }

    override fun languageVersionSettings(value: LanguageVersionSettings) = withValidityAssertion {
        configuration.put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, value)
    }

    override fun allowedErrorFilter(value: (KaDiagnostic) -> Boolean) = withValidityAssertion {
        allowedErrorFilterValue = value
    }

    override fun codeFragmentClassName(value: String) = withValidityAssertion {
        codeFragmentClassNameValue = value
    }

    override fun codeFragmentMethodName(value: String) = withValidityAssertion {
        codeFragmentMethodNameValue = value
    }

    override fun jvmTarget(value: KaJvmTarget) = withValidityAssertion {
        val jvmTarget = JvmTarget.fromString(value.name)
            ?: error("Unsupported JVM target: ${value.name}")
        configuration.put(JVMConfigurationKeys.JVM_TARGET, jvmTarget)
    }

    override fun jvmCompiledClassHandler(value: KaCompiledClassHandler) = withValidityAssertion {
        compiledClassHandlerValue = value
    }

    /** Legacy overload accepting the `components` class handler, which is a subtype of the canonical `compilation` one. */
    override fun jvmCompiledClassHandler(value: KaLegacyCompiledClassHandler) = withValidityAssertion {
        compiledClassHandlerValue = value
    }

    @KaNonPublicApi
    override fun jvmOutputAsmListing(value: Boolean) = withValidityAssertion {
        jvmOutputAsmListingValue = value
    }

    @KaIdeApi
    override fun stubUnboundIrSymbols(value: Boolean) = withValidityAssertion {
        stubUnboundIrSymbolsValue = value
    }

    @KaIdeApi
    override fun disableInline(value: Boolean) = withValidityAssertion {
        configuration.put(CommonConfigurationKeys.DISABLE_INLINE, value)
    }

    @KaIdeApi
    override fun disableCallAssertions(value: Boolean) = withValidityAssertion {
        configuration.put(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS, value)
    }

    @KaIdeApi
    override fun disableOptimization(value: Boolean) = withValidityAssertion {
        configuration.put(JVMConfigurationKeys.DISABLE_OPTIMIZATION, value)
    }

    @KaIdeApi
    override fun disableParameterAssertions(value: Boolean) = withValidityAssertion {
        configuration.put(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS, value)
    }

    @KaIdeApi
    override fun ignoreConstOptimizationErrors(value: Boolean) = withValidityAssertion {
        configuration.put(CommonConfigurationKeys.IGNORE_CONST_OPTIMIZATION_ERRORS, value)
    }

    @KaIdeApi
    override fun jvmExecutionStack(value: Sequence<PsiElement?>) = withValidityAssertion {
        jvmExecutionStackValue = value
    }

    @KaIdeApi
    override fun jvmGenerateParameterMetadata(value: Boolean) = withValidityAssertion {
        configuration.put(JVMConfigurationKeys.PARAMETERS_METADATA, value)
    }

    @KaIdeApi
    override fun jvmUseInvokeDynamicForSamConversions(value: Boolean) = withValidityAssertion {
        val scheme = if (value) JvmClosureGenerationScheme.INDY else JvmClosureGenerationScheme.CLASS
        configuration.put(JVMConfigurationKeys.SAM_CONVERSIONS, scheme)
    }

    @KaIdeApi
    override fun jvmUseInvokeDynamicForLambdas(value: Boolean) = withValidityAssertion {
        val scheme = if (value) JvmClosureGenerationScheme.INDY else JvmClosureGenerationScheme.CLASS
        configuration.put(JVMConfigurationKeys.LAMBDAS, scheme)
    }
}

internal fun KaLegacyCompilationTarget.toNewCompilationTarget(): KaCompilationTarget =
    when (this) {
        KaLegacyCompilationTarget.JVM -> KaCompilationTarget.JVM
    }

internal fun KaCompilationTarget.toLegacyCompilationTarget(): KaLegacyCompilationTarget =
    when (this) {
        KaCompilationTarget.JVM -> KaLegacyCompilationTarget.JVM
    }

internal fun KaLegacyCompilerFacilityModuleActualizer.toNewModuleActualizer(): KaCompilerFacilityModuleActualizer =
    KaCompilerFacilityModuleActualizer { module, target ->
        actualize(module, target.toLegacyCompilationTarget())
    }
