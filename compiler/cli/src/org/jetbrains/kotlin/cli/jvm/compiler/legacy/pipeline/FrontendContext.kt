/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline

import org.jetbrains.kotlin.cli.common.LegacyK2CliPipeline
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

@LegacyK2CliPipeline
internal class MinimizedFrontendContext(
    override val projectEnvironment: VfsBasedProjectEnvironment,
    override val messageCollector: MessageCollector,
    override val extensionRegistrars: List<FirExtensionRegistrar>,
    override val configuration: CompilerConfiguration
) : FrontendContext

@LegacyK2CliPipeline
interface FrontendContext {
    val projectEnvironment: VfsBasedProjectEnvironment
    val messageCollector: MessageCollector
    val extensionRegistrars: List<FirExtensionRegistrar>
    val configuration: CompilerConfiguration
}
