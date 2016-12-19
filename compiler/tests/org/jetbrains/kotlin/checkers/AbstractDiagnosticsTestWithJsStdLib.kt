/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.checkers

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.config.LibrarySourcesConfig
import org.jetbrains.kotlin.js.resolve.*
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.serialization.js.JsModuleDescriptor
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.test.KotlinTestUtils

import java.util.ArrayList

abstract class AbstractDiagnosticsTestWithJsStdLib : AbstractDiagnosticsTest() {
    protected var config: JsConfig? = null
        private set

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        val configuration = environment.configuration.copy()
        configuration.put(CommonConfigurationKeys.MODULE_NAME, KotlinTestUtils.TEST_MODULE_NAME)
        configuration.put(JSConfigurationKeys.LIBRARY_FILES, LibrarySourcesConfig.JS_STDLIB)
        config = LibrarySourcesConfig(project, configuration)
    }

    @Throws(Exception::class)
    override fun tearDown() {
        config = null
        super.tearDown()
    }

    override fun getEnvironmentConfigFiles(): List<String> {
        return EnvironmentConfigFiles.JS_CONFIG_FILES
    }

    override fun analyzeModuleContents(
            moduleContext: ModuleContext,
            ktFiles: List<KtFile>,
            moduleTrace: BindingTrace,
            languageVersionSettings: LanguageVersionSettings?,
            separateModules: Boolean
    ): JsAnalysisResult {
        // TODO: support LANGUAGE directive in JS diagnostic tests
        assert(languageVersionSettings == null) { BaseDiagnosticsTest.LANGUAGE_DIRECTIVE + " directive is not supported in JS diagnostic tests" }
        moduleTrace.record<ModuleDescriptor, ModuleKind>(MODULE_KIND, moduleContext.module, getModuleKind(ktFiles))
        return TopDownAnalyzerFacadeForJS.analyzeFilesWithGivenTrace(ktFiles, moduleTrace, moduleContext, config!!)
    }

    private fun getModuleKind(ktFiles: List<KtFile>): ModuleKind {
        var kind = ModuleKind.PLAIN
        for (file in ktFiles) {
            val text = file.text
            for (line in StringUtil.splitByLines(text)) {
                line = line.trim { it <= ' ' }
                if (!line.startsWith("//")) continue
                line = line.substring(2).trim { it <= ' ' }
                val parts = StringUtil.split(line, ":")
                if (parts.size != 2) continue

                if (parts[0].trim { it <= ' ' } != "MODULE_KIND") continue
                kind = ModuleKind.valueOf(parts[1].trim { it <= ' ' })
            }
        }

        return kind
    }

    override fun getAdditionalDependencies(module: ModuleDescriptorImpl): List<ModuleDescriptorImpl> {
        val dependencies = ArrayList<ModuleDescriptorImpl>()
        for (moduleDescriptor in config!!.moduleDescriptors) {
            dependencies.add(moduleDescriptor.data)
        }
        return dependencies
    }

    override fun shouldSkipJvmSignatureDiagnostics(groupedByModule: Map<BaseDiagnosticsTest.TestModule, List<BaseDiagnosticsTest.TestFile>>): Boolean {
        return true
    }

    override fun createModule(moduleName: String, storageManager: StorageManager): ModuleDescriptorImpl {
        return ModuleDescriptorImpl(Name.special("<$moduleName>"), storageManager, JsPlatform.builtIns)
    }

    override fun createSealedModule(storageManager: StorageManager): ModuleDescriptorImpl {
        val module = createModule("kotlin-js-test-module", storageManager)

        val dependencies = ArrayList<ModuleDescriptorImpl>()
        dependencies.add(module)

        dependencies.addAll(getAdditionalDependencies(module))

        dependencies.add(module.builtIns.builtInsModule)
        module.setDependencies(dependencies)

        return module
    }
}
