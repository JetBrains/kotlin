/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.pipeline

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cli.common.GroupedKtSources
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.messageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.psi.KtFile
import kotlin.collections.map

internal class FrontendContextForSingleModulePsi(
    module: Module,
    val allSources: List<KtFile>,
    projectEnvironment: VfsBasedProjectEnvironment,
    messageCollector: MessageCollector,
    renderDiagnosticName: Boolean,
    configuration: CompilerConfiguration,
    targetIds: List<TargetId>?,
    incrementalComponents: IncrementalCompilationComponents?,
    extensionRegistrars: List<FirExtensionRegistrar>
) : FrontendContextForSingleModule(
    module, projectEnvironment, messageCollector, renderDiagnosticName,
    configuration, targetIds, incrementalComponents,
    extensionRegistrars
)

internal class FrontendContextForSingleModuleLightTree(
    module: Module,
    val groupedSources: GroupedKtSources,
    projectEnvironment: VfsBasedProjectEnvironment,
    messageCollector: MessageCollector,
    renderDiagnosticName: Boolean,
    configuration: CompilerConfiguration,
    targetIds: List<TargetId>?,
    incrementalComponents: IncrementalCompilationComponents?,
    extensionRegistrars: List<FirExtensionRegistrar>
) : FrontendContextForSingleModule(
    module, projectEnvironment, messageCollector, renderDiagnosticName,
    configuration, targetIds, incrementalComponents,
    extensionRegistrars
)

internal abstract class FrontendContextForSingleModule(
    val module: Module,
    override val projectEnvironment: VfsBasedProjectEnvironment,
    override val messageCollector: MessageCollector,
    val renderDiagnosticName: Boolean,
    override val configuration: CompilerConfiguration,
    override val targetIds: List<TargetId>?,
    override val incrementalComponents: IncrementalCompilationComponents?,
    override val extensionRegistrars: List<FirExtensionRegistrar>
) : FrontendContext

class FrontendContextForMultiChunkMode private constructor(
    override val projectEnvironment: VfsBasedProjectEnvironment,
    override val messageCollector: MessageCollector,
    override val incrementalComponents: IncrementalCompilationComponents?,
    override val extensionRegistrars: List<FirExtensionRegistrar>,
    override val configuration: CompilerConfiguration,
    override val targetIds: List<TargetId>?
) : FrontendContext {
    constructor(
        projectEnvironment: VfsBasedProjectEnvironment,
        environment: KotlinCoreEnvironment,
        compilerConfiguration: CompilerConfiguration,
        project: Project?
    ) : this(
        projectEnvironment,
        environment.messageCollector,
        compilerConfiguration,
        project
    )

    constructor(
        projectEnvironment: VfsBasedProjectEnvironment,
        messageCollector: MessageCollector,
        compilerConfiguration: CompilerConfiguration,
        project: Project?,
    ) : this(
        projectEnvironment,
        messageCollector,
        incrementalComponents = compilerConfiguration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS),
        extensionRegistrars = project?.let { FirExtensionRegistrar.getInstances(it) } ?: emptyList(),
        configuration = compilerConfiguration,
        targetIds = compilerConfiguration.get(JVMConfigurationKeys.MODULES)?.map(::TargetId)
    )
}

class FrontendContextForIncrementalCompilation(
    override val projectEnvironment: VfsBasedProjectEnvironment,
    override val messageCollector: MessageCollector,
    override val incrementalComponents: IncrementalCompilationComponents?,
    override val extensionRegistrars: List<FirExtensionRegistrar>,
    override val configuration: CompilerConfiguration,
    override val targetIds: List<TargetId>?,
) : FrontendContext {
    constructor(
        projectEnvironment: VfsBasedProjectEnvironment,
        messageCollector: MessageCollector,
        compilerConfiguration: CompilerConfiguration,
    ) : this(
        projectEnvironment,
        messageCollector,
        incrementalComponents = compilerConfiguration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS),
        extensionRegistrars = projectEnvironment.project.let { FirExtensionRegistrar.getInstances(it) },
        configuration = compilerConfiguration,
        targetIds = compilerConfiguration.get(JVMConfigurationKeys.MODULES)?.map(::TargetId)
    )
}

interface FrontendContext {
    val projectEnvironment: VfsBasedProjectEnvironment
    val messageCollector: MessageCollector
    val incrementalComponents: IncrementalCompilationComponents?
    val extensionRegistrars: List<FirExtensionRegistrar>
    val configuration: CompilerConfiguration
    val targetIds: List<TargetId>?
}
