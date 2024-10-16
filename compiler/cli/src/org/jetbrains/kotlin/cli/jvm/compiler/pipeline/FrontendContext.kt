/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.pipeline

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.GroupedKtSources
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.psi.KtFile

internal class FrontendContextForSingleModulePsi(
    module: Module,
    val allSources: List<KtFile>,
    projectEnvironment: VfsBasedProjectEnvironment,
    messageCollector: MessageCollector,
    configuration: CompilerConfiguration
) : FrontendContextForSingleModule(module, projectEnvironment, messageCollector, configuration)

internal class FrontendContextForSingleModuleLightTree(
    module: Module,
    val groupedSources: GroupedKtSources,
    projectEnvironment: VfsBasedProjectEnvironment,
    messageCollector: MessageCollector,
    configuration: CompilerConfiguration
) : FrontendContextForSingleModule(module, projectEnvironment, messageCollector, configuration)

internal sealed class FrontendContextForSingleModule(
    val module: Module,
    override val projectEnvironment: VfsBasedProjectEnvironment,
    override val messageCollector: MessageCollector,
    override val configuration: CompilerConfiguration,
) : FrontendContext {
    val renderDiagnosticName: Boolean
        get() = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)

    override val extensionRegistrars: List<FirExtensionRegistrar> = FirExtensionRegistrar.getInstances(projectEnvironment.project)
}

internal fun createFrontendContextForMultiChunkMode(
    projectEnvironment: VfsBasedProjectEnvironment,
    messageCollector: MessageCollector,
    compilerConfiguration: CompilerConfiguration,
    project: Project?,
): FrontendContext = MinimizedFrontendContext(
    projectEnvironment,
    messageCollector,
    extensionRegistrars = project?.let { FirExtensionRegistrar.getInstances(it) } ?: emptyList(),
    configuration = compilerConfiguration
)

internal class MinimizedFrontendContext(
    override val projectEnvironment: VfsBasedProjectEnvironment,
    override val messageCollector: MessageCollector,
    override val extensionRegistrars: List<FirExtensionRegistrar>,
    override val configuration: CompilerConfiguration
) : FrontendContext

internal interface FrontendContext {
    val projectEnvironment: VfsBasedProjectEnvironment
    val messageCollector: MessageCollector
    val extensionRegistrars: List<FirExtensionRegistrar>
    val configuration: CompilerConfiguration
}
