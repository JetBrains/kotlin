/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.codegen.GeneratedClassLoader
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.*
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.builders.jvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFailingTestSuppressor
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.net.URLClassLoader

open class AbstractFirScriptCodegenTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {

    override fun TestConfigurationBuilder.configuration() {
        configureFirParser(FirParser.Psi)

        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator,
            ::ScriptingPluginEnvironmentConfigurator
        )

        facadeStep(::FirFrontendFacade)
        firHandlersStep {
            useHandlers(
                ::FirDiagnosticsHandler
            )
            commonFirHandlersForCodegenTest()
        }

        facadeStep(::Fir2IrResultsConverter)
        irHandlersStep {
            useHandlers(
                ::IrTextDumpHandler,
                ::IrPrettyKotlinDumpHandler,
            )
        }
        facadeStep(::JvmIrBackendFacade)
        jvmArtifactsHandlersStep {
            commonBackendHandlersForCodegenTest()
            useHandlers(
                ::BytecodeListingHandler,
                ::FirJvmScriptRunChecker
            )
        }

        enableMetaInfoHandler()

        useAfterAnalysisCheckers(
            ::FirFailingTestSuppressor,
            ::BlackBoxCodegenSuppressor
        )
    }
}

class FirJvmScriptRunChecker(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {

    private var scriptProcessed = false

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        val fileInfos = info.fileInfos.ifEmpty { return }
        val classLoader = generatedTestClassLoader(testServices, module, info.classFileFactory)
        try {
            for (fileInfo in fileInfos) {
                when (val sourceFile = fileInfo.sourceFile) {
                    is KtPsiSourceFile -> (sourceFile.psiFile as? KtFile)?.let { ktFile ->
                        ktFile.script?.fqName?.let { scriptFqName ->
                            runAndCheckScript(ktFile, scriptFqName, classLoader)
                            scriptProcessed = true
                        }
                    }
                    else -> {
                        assertions.fail { "Only PSI scripts are supported so far" }
                    }
                }
            }
        } finally {
            classLoader.dispose()
        }
    }

    private fun runAndCheckScript(
        ktFile: KtFile,
        scriptFqName: FqName,
        classLoader: GeneratedClassLoader,
    ) {
        val expected = Regex("// expected: (\\S+): (.*)").findAll(ktFile.text).map {
            it.groups[1]!!.value to it.groups[2]!!.value
        }

        val scriptClass = classLoader.loadClass(scriptFqName.asString())
        val ctor = scriptClass.constructors.single()
        val args: Array<String> =
            Regex("param: (\\S.*)").find(ktFile.text)?.let { it.groups[1]?.value?.split(" ") }
                .orEmpty().toTypedArray()
        val scriptInstance = ctor.newInstance(args)
        var anyExpectationFound = false
        for ((fieldName, expectedValue) in expected) {
            anyExpectationFound = true

            if (expectedValue == "<nofield>") {
                try {
                    scriptClass.getDeclaredField(fieldName)
                    assertions.fail { "must have no field $fieldName" }
                } catch (e: NoSuchFieldException) {
                    continue
                }
            }

            val field = scriptClass.getDeclaredField(fieldName)
            field.isAccessible = true
            val result = field[scriptInstance]
            val resultString = result?.toString() ?: "null"
            assertions.assertEquals(expectedValue, resultString) { "comparing field $fieldName" }
        }
        assertions.assertTrue(anyExpectationFound) { "expecting at least one expectation" }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!scriptProcessed) {
            assertions.fail { "Can't find script to test" }
        }
    }
}

class ScriptingPluginEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val libPath = PathUtil.kotlinPathsForCompiler.libPath
        val pluginClasspath = PathUtil.KOTLIN_SCRIPTING_PLUGIN_CLASSPATH_JARS.map { File(libPath, it) }
        val pluginClassLoader = URLClassLoader(pluginClasspath.map { it.toURI().toURL() }.toTypedArray(), this::class.java.classLoader)

        val pluginRegistrarClass = pluginClassLoader.loadClass(CLICompiler.SCRIPT_PLUGIN_REGISTRAR_NAME)
        @Suppress("DEPRECATION")
        (pluginRegistrarClass.getDeclaredConstructor().newInstance() as? ComponentRegistrar)?.also {
            configuration.add(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS, it)
        }

        val pluginK2RegistrarClass = pluginClassLoader.loadClass(CLICompiler.SCRIPT_PLUGIN_K2_REGISTRAR_NAME)
        (pluginK2RegistrarClass.getDeclaredConstructor().newInstance() as? CompilerPluginRegistrar)?.also {
            configuration.add(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, it)
        }
    }
}
