package org.jetbrains.kotlin.codegen.debugInformation

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys

abstract class AbstractIrLocalVariableTest : AbstractLocalVariableTest() {

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        super.updateConfiguration(configuration)
        configuration.put(JVMConfigurationKeys.IR, true)
    }
}