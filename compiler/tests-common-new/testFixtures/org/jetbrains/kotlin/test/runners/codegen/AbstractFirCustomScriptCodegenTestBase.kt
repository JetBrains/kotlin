/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.scripting.compiler.plugin.configureScriptDefinitions
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.IrPrettyKotlinDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.builders.jvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonBackendHandlersForCodegenTest
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.KOTLIN_SCRIPT_DEFINITION
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirFailingTestSuppressor
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.runners.codegen.FirJvmScriptRunChecker
import org.jetbrains.kotlin.test.runners.codegen.FirPsiCodegenTest
import org.jetbrains.kotlin.test.runners.codegen.ScriptingPluginEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.PathUtil.KOTLIN_SCRIPTING_COMMON_JAR
import org.jetbrains.kotlin.utils.PathUtil.KOTLIN_SCRIPTING_COMPILER_IMPL_JAR
import org.jetbrains.kotlin.utils.PathUtil.KOTLIN_SCRIPTING_COMPILER_PLUGIN_JAR
import org.jetbrains.kotlin.utils.PathUtil.KOTLIN_SCRIPTING_JVM_JAR
import java.io.File
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.api.providedProperties
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContextOrStdlib

abstract class AbstractFirCustomScriptCodegenTestBase(
    val parser: FirParser,
) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {

    override fun configure(builder: TestConfigurationBuilder): Unit = with(builder) {
        configureFirParser(parser)

        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator,
            ::ScriptDefinitionConfigurator,
            ::ScriptingPluginEnvironmentConfigurator,
        )

        facadeStep(::FirCliJvmFacade)
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
                ::FirJvmScriptRunChecker,
            )
        }

        enableMetaInfoHandler()

        useFailureSuppressors(
            ::FirFailingTestSuppressor,
            ::BlackBoxCodegenSuppressor,
        )
    }
}

class ScriptDefinitionConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val registeredDirectives = module.directives
        val scriptDefinitions = registeredDirectives[KOTLIN_SCRIPT_DEFINITION]
        if (scriptDefinitions.isNotEmpty()) {
            val additionalDependencies =
                scriptCompilationClasspathFromContextOrStdlib("tests-common", "kotlin-stdlib") +
                        File(TestScriptWithReceivers::class.java.protectionDomain.codeSource.location.toURI().path)
            configuration.addJvmClasspathRoots(additionalDependencies)
            configureScriptDefinitions(
                scriptDefinitions, configuration, this::class.java.classLoader,
                MessageCollector.NONE, defaultJvmScriptingHostConfiguration
            )
        }
    }
}

open class AbstractFirLightTreeCustomScriptCodegenTest : AbstractFirCustomScriptCodegenTestBase(FirParser.LightTree)

@FirPsiCodegenTest
open class AbstractFirPsiCustomScriptCodegenTest : AbstractFirCustomScriptCodegenTestBase(FirParser.Psi)

@Suppress("JavaIoSerializableObjectMustHaveReadResolve")
object TestScriptWithReceiversConfiguration : ScriptCompilationConfiguration(
    {
        implicitReceivers(String::class)
    }
)

@Suppress("unused")
@KotlinScript(compilationConfiguration = TestScriptWithReceiversConfiguration::class)
abstract class TestScriptWithReceivers

@Suppress("JavaIoSerializableObjectMustHaveReadResolve")
object TestScriptWithSimpleEnvVarsConfiguration : ScriptCompilationConfiguration(
    {
        providedProperties("stringVar1" to String::class)
    }
)

@Suppress("unused")
@KotlinScript(compilationConfiguration = TestScriptWithSimpleEnvVarsConfiguration::class)
abstract class TestScriptWithSimpleEnvVars

@Suppress("unused")
@KotlinScript(fileExtension = "customext")
abstract class TestScriptWithNonKtsExtension(val name: String)

@Suppress("unused")
@KotlinScript(filePathPattern = "(.*/)?pathPattern[0-9]\\..+")
abstract class TestScriptWithPathPattern(val name2: String)
