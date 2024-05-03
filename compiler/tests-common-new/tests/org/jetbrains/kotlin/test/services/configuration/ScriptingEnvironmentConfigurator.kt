/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.script.loadScriptingPlugin
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*

class ScriptingEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (module.files.any { it.isKtsFile }) {
            val pluginFiles = testServices.standardLibrariesPathProvider.scriptingPluginFilesForTests().map { it.path }
            loadScriptingPlugin(configuration, pluginFiles)
        }
    }
}
