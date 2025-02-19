/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCompilerConfigurationExtension
import org.jetbrains.kotlin.scripting.test.configureWithCustomScriptDef
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

abstract class AbstractLLScriptWithCustomDefDiagnosticsTest : AbstractLLDiagnosticsTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureCustomScriptDefinitions()
    }
}

internal fun TestConfigurationBuilder.configureCustomScriptDefinitions() {
    configureWithCustomScriptDef()
    useConfigurators(::CustomScriptDefinitionEnvironmentConfigurator)
}

internal class CustomScriptDefinitionEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun legacyRegisterCompilerExtensions(project: Project, module: TestModule, configuration: CompilerConfiguration) {
        val hostConfiguration = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {}

        ScriptingCompilerConfigurationExtension(project as MockProject, hostConfiguration).updateConfiguration(configuration)
    }
}
