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
class KaBaseCompilerOptionsBuilder(val token: KaLifetimeToken, val configuration: CompilerConfiguration) : KaCompilerOptionsBuilder {
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

    fun build(): KaBaseCompilationOptions {
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

    override fun target(value: KaCompilationTarget) {
        targetValue = value
    }

    override fun moduleName(value: String) {
        configuration.put(CommonConfigurationKeys.MODULE_NAME, value)
    }

    override fun moduleActualizer(value: KaCompilerFacilityModuleActualizer) {
        moduleActualizerValue = value
    }

    override fun languageVersionSettings(value: LanguageVersionSettings) {
        configuration.put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, value)
    }

    override fun allowedErrorFilter(value: (KaDiagnostic) -> Boolean) {
        allowedErrorFilterValue = value
    }

    override fun codeFragmentClassName(value: String) {
        codeFragmentClassNameValue = value
    }

    override fun codeFragmentMethodName(value: String) {
        codeFragmentMethodNameValue = value
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
        stubUnboundIrSymbolsValue = value
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