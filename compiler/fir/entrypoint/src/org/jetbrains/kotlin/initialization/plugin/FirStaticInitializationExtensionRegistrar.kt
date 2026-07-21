package org.jetbrains.kotlin.initialization.plugin

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.initialization.plugin.checker.StaticInitializationDiagnostics

class FirStaticInitializationExtensionRegistrar : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        registerDiagnosticContainers(StaticInitializationDiagnostics)
    }
}