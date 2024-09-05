/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.cli.jvm.compiler

import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.FrontendContextForSingleModulePsi
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.compileSourceFilesToAnalyzedFirViaPsi
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.PendingDiagnosticsCollectorWithSuppress
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.FirResult
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.psi.KtFile

object FirKotlinToJvmBytecodeCompiler {
    fun checkNotSupportedPlugins(
        compilerConfiguration: CompilerConfiguration,
        messageCollector: MessageCollector
    ): Boolean {
        val notSupportedPlugins = mutableListOf<String?>().apply {
            compilerConfiguration.get(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS)
                .collectIncompatiblePluginNamesTo(this, ComponentRegistrar::supportsK2)
            compilerConfiguration.get(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS)
                .collectIncompatiblePluginNamesTo(this, CompilerPluginRegistrar::supportsK2)
        }

        if (notSupportedPlugins.isNotEmpty()) {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                """
                    |There are some plugins incompatible with language version 2.0:
                    |${notSupportedPlugins.joinToString(separator = "\n|") { "  $it" }}
                    |Please use language version 1.9 or below
                """.trimMargin()
            )
            return false
        }

        return true
    }

    fun runFrontendForKapt(
        environment: VfsBasedProjectEnvironment,
        configuration: CompilerConfiguration,
        messageCollector: MessageCollector,
        sources: List<KtFile>,
        module: Module,
    ): FirResult {
        val context = FrontendContextForSingleModulePsi(
            module,
            sources,
            environment,
            messageCollector,
            configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME),
            configuration,
            configuration.get(JVMConfigurationKeys.MODULES)?.map(::TargetId),
            configuration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS),
            extensionRegistrars = FirExtensionRegistrar.getInstances(environment.project),
        )
        val diagnosticsReporter = createPendingReporter(messageCollector)
        return context.compileSourceFilesToAnalyzedFirViaPsi(
            sources, diagnosticsReporter, module.getModuleName(), module.getFriendPaths(), true
        )!!
    }

    private fun <T : Any> List<T>?.collectIncompatiblePluginNamesTo(
        destination: MutableList<String?>,
        supportsK2: T.() -> Boolean
    ) {
        this?.filter { !it.supportsK2() && it::class.java.canonicalName != CLICompiler.SCRIPT_PLUGIN_REGISTRAR_NAME }
            ?.mapTo(destination) { it::class.qualifiedName }
    }

    fun createPendingReporter(messageCollector: MessageCollector): PendingDiagnosticsCollectorWithSuppress =
        DiagnosticReporterFactory.createPendingReporter { message, severity ->
            messageCollector.report(severity, message)
        }
}
