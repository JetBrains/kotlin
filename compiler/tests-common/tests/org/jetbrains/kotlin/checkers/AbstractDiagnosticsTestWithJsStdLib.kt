/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.js.analyzer.WebAnalysisResult
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.js.resolve.MODULE_KIND
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.util.*

@ObsoleteTestInfrastructure("org.jetbrains.kotlin.test.runners.AbstractDiagnosticsTestWithJsStdLib")
abstract class AbstractDiagnosticsTestWithJsStdLib : AbstractDiagnosticsTest() {
    private var lazyConfig: Lazy<JsConfig>? = lazy(LazyThreadSafetyMode.NONE) {
        JsConfig(project, environment.configuration.copy().apply {
            put(CommonConfigurationKeys.MODULE_NAME, KotlinTestUtils.TEST_MODULE_NAME)
            put(JSConfigurationKeys.LIBRARIES, JsConfig.JS_STDLIB)
        }, CompilerEnvironment)
    }

    protected val config: JsConfig get() = lazyConfig!!.value

    override fun tearDown() {
        lazyConfig = null
        super.tearDown()
    }

    override fun getEnvironmentConfigFiles(): EnvironmentConfigFiles = EnvironmentConfigFiles.JS_CONFIG_FILES

    override fun analyzeModuleContents(
        moduleContext: ModuleContext,
        files: List<KtFile>,
        moduleTrace: BindingTrace,
        languageVersionSettings: LanguageVersionSettings,
        separateModules: Boolean,
        jvmTarget: JvmTarget
    ): WebAnalysisResult {
        // TODO: support LANGUAGE directive in JS diagnostic tests
        moduleTrace.record<ModuleDescriptor, ModuleKind>(MODULE_KIND, moduleContext.module, getModuleKind(files))
        config.configuration.languageVersionSettings = languageVersionSettings
        return TopDownAnalyzerFacadeForJS.analyzeFilesWithGivenTrace(
            files, moduleTrace, moduleContext, config.configuration, CompilerEnvironment, config.project
        )
    }

    private fun getModuleKind(ktFiles: List<KtFile>): ModuleKind {
        var kind = ModuleKind.PLAIN
        for (file in ktFiles) {
            val text = file.text
            for (textLine in StringUtil.splitByLines(text)) {
                var line = textLine.trim { it <= ' ' }
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

    override fun getAdditionalDependencies(module: ModuleDescriptorImpl): List<ModuleDescriptorImpl> =
        config.moduleDescriptors

    override fun shouldSkipJvmSignatureDiagnostics(groupedByModule: Map<TestModule?, List<TestFile>>): Boolean = true

    override fun createModule(moduleName: String, storageManager: StorageManager): ModuleDescriptorImpl =
        ModuleDescriptorImpl(Name.special("<$moduleName>"), storageManager, JsPlatformAnalyzerServices.builtIns)

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
