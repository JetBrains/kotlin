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
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.projectStructure.KaJvmTarget
import org.jetbrains.kotlin.config.*

/** Simple class name for the code fragment facade class. */
@KaImplementationDetail
val CODE_FRAGMENT_CLASS_NAME: CompilerConfigurationKey<String> = CompilerConfigurationKey("code fragment class name")

/** Entry point method name for the code fragment. */
@KaImplementationDetail
val CODE_FRAGMENT_METHOD_NAME: CompilerConfigurationKey<String> = CompilerConfigurationKey("code fragment method name")

/** A custom actualizer for the source module. */
@KaImplementationDetail
val MODULE_ACTUALIZER: CompilerConfigurationKey<KaCompilerFacilityModuleActualizer> =
    CompilerConfigurationKey("custom module actualizer")

/**
 * Whether unbound IR symbols should be stubbed instead of linked.
 *
 * This should be enabled if the compiled file could refer to symbols defined in another file of the same module.
 * Such symbols are not compiled (only the file is passed to the backend) and so they cannot be linked from a dependency.
 */
@KaImplementationDetail
val STUB_UNBOUND_IR_SYMBOLS: CompilerConfigurationKey<Boolean> = CompilerConfigurationKey("stub unbound IR symbols")

/**
 * Internal representation of the compilation target with JVM-specific options.
 *
 * Used by [KaCompilerFacility] implementations to bridge between the public [KaCompilationOptions] and internal compiler APIs.
 */
@KaImplementationDetail
sealed class KaCompilerTarget {
    @KaImplementationDetail
    class Jvm(
        val isTestMode: Boolean,
        val compiledClassHandler: KaCompiledClassHandler?,
        val debuggerExtension: KaDebuggerExtension?,
    ) : KaCompilerTarget()
}

/**
 * Provides an extension point for the compiler to retrieve additional information from the debugger API.
 *
 * Used for debugger code fragment compilation.
 *
 * @property stack A sequence of PSI elements of the expressions (function calls or property accesses) in the current execution stack,
 * listed from the top to the bottom.
 */
@KaImplementationDetail
class KaDebuggerExtension(val stack: Sequence<PsiElement?>)

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

    fun build(): KaBaseCompilationOptions = withValidityAssertion {
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

    override fun target(value: KaCompilationTarget) = withValidityAssertion {
        targetValue = value
    }

    override fun moduleName(value: String) = withValidityAssertion {
        configuration.put(CommonConfigurationKeys.MODULE_NAME, value)
    }

    override fun moduleActualizer(value: KaCompilerFacilityModuleActualizer) = withValidityAssertion {
        configuration.put(MODULE_ACTUALIZER, value)
    }

    override fun languageVersionSettings(value: LanguageVersionSettings) = withValidityAssertion {
        configuration.put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, value)
    }

    override fun allowedErrorFilter(value: (KaDiagnostic) -> Boolean) = withValidityAssertion {
        allowedErrorFilterValue = value
    }

    override fun codeFragmentClassName(value: String) = withValidityAssertion {
        configuration.put(CODE_FRAGMENT_CLASS_NAME, value)
    }

    override fun codeFragmentMethodName(value: String) = withValidityAssertion {
        configuration.put(CODE_FRAGMENT_METHOD_NAME, value)
    }

    override fun jvmTarget(value: KaJvmTarget) = withValidityAssertion {
        val jvmTarget = JvmTarget.fromString(value.name)
            ?: error("Unsupported JVM target: ${value.name}")
        configuration.put(JVMConfigurationKeys.JVM_TARGET, jvmTarget)
    }

    override fun jvmCompiledClassHandler(value: KaCompiledClassHandler) = withValidityAssertion {
        compiledClassHandlerValue = value
    }

    @KaNonPublicApi
    override fun jvmOutputAsmListing(value: Boolean) = withValidityAssertion {
        jvmOutputAsmListingValue = value
    }

    @KaIdeApi
    override fun stubUnboundIrSymbols(value: Boolean) = withValidityAssertion {
        configuration.put(STUB_UNBOUND_IR_SYMBOLS, value)
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

    @KaIdeApi
    override fun jvmLinkViaSignatures(value: Boolean) = withValidityAssertion {
        configuration.put(JVMConfigurationKeys.LINK_VIA_SIGNATURES, value)
    }
}
