package org.jetbrains.kotlin.cli.student

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM

/**
 * Created by minamoto on 14/09/16.
 */

class K2StudentLlvm : CLICompiler<K2StudentLlvmArguments>() {
    override fun setupPlatformSpecificArgumentsAndServices(configuration: CompilerConfiguration, arguments: K2StudentLlvmArguments, services: Services) {}

    override fun doExecute(arguments: K2StudentLlvmArguments, configuration: CompilerConfiguration, rootDisposable: Disposable): ExitCode {
        configuration.put(CommonConfigurationKeys.MODULE_NAME, /*arguments.moduleName ?: */ JvmAbi.DEFAULT_MODULE_NAME)
        val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, emptyList())
        val collector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val analyzerWithCompilerReport = AnalyzerWithCompilerReport(collector)
        analyzerWithCompilerReport.analyzeAndReport(
                environment.getSourceFiles(), object : AnalyzerWithCompilerReport.Analyzer {
            override fun analyze(): AnalysisResult {
                val sharedTrace = CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace()
                val moduleContext =
                        TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(environment.project, environment.configuration)

                return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                        moduleContext,
                        environment.getSourceFiles(),
                        sharedTrace,
                        environment.configuration,
                        JvmPackagePartProvider(environment)
                )
            }
            override fun reportEnvironmentErrors() {
                TODO(/* implement me*/)
                //KotlinToJVMBytecodeCompiler.reportRuntimeConflicts(collector, environment.configuration.jvmClasspathRoots)
            }
        })

        val translator = Psi2IrTranslator(Psi2IrConfiguration(false))
        val module = translator.generateModule(analyzerWithCompilerReport.analysisResult.moduleDescriptor,
                environment.getSourceFiles(),
                BindingContext.EMPTY)
        return ExitCode.OK
    }

    override fun createArguments(): K2StudentLlvmArguments {
        return K2StudentLlvmArguments()
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            CLICompiler.doMain(K2StudentLlvm(), args)
        }
    }
}

fun main(args: Array<String>) = K2StudentLlvm.main(args)
