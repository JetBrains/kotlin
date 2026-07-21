package org.jetbrains.kotlin.initialization.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.compiler.plugin.registerExtension
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.initialization.plugin.checker.StaticInitializationChecker
import org.jetbrains.kotlin.initialization.plugin.logic.DefaultFunctionParametersCollector
import org.jetbrains.kotlin.initialization.plugin.logic.OverridingCallablesCollector

@OptIn(ExperimentalCompilerApi::class)
class UseBeforeStaticInitializationPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String get() = "org.jetbrains.kotlin.initialization.plugin"
    override val supportsK2: Boolean get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(OverridingCallablesCollector)
        IrGenerationExtension.registerExtension(DefaultFunctionParametersCollector)
        IrGenerationExtension.registerExtension(StaticInitializationChecker)
        FirExtensionRegistrar.registerExtension(FirStaticInitializationExtensionRegistrar())
    }
}
