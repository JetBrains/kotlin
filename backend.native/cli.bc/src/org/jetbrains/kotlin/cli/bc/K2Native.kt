package org.jetbrains.kotlin.cli.bc

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.native.llvm.emitLLVM
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.config.addKotlinSourceRoots
import org.jetbrains.kotlin.ir.util.DumpIrTreeVisitor
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import java.lang.System.out
import java.util.*
import java.util.Collections.emptyList
import kotlin.reflect.jvm.internal.impl.load.java.JvmAbi

class K2Native : CLICompiler<K2NativeCompilerArguments>() {
    override fun doExecute(arguments: K2NativeCompilerArguments, configuration: CompilerConfiguration, rootDisposable: Disposable): ExitCode {
        configuration.put(CommonConfigurationKeys.MODULE_NAME, JvmAbi.DEFAULT_MODULE_NAME)

        configuration.addKotlinSourceRoots(arguments.freeArgs)
        val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, Arrays.asList<String>("extensions/common.xml"))
        val collector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val analyzerWithCompilerReport = AnalyzerWithCompilerReport(collector)

        analyzerWithCompilerReport.analyzeAndReport(
                environment.getSourceFiles(), object : AnalyzerWithCompilerReport.Analyzer {
            override fun analyze(): AnalysisResult {
                val sharedTrace = CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace()

                /* replace this with native specific */
                val moduleContext =
                        TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(environment.project, environment.configuration)


                return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                        environment.project,
                        environment.getSourceFiles(),
                        sharedTrace,
                        environment.configuration,
                        { scope -> JvmPackagePartProvider(environment, scope) }
                )
            }
            override fun reportEnvironmentErrors() {
                TODO("implement me")
            }
        })

        val translator = Psi2IrTranslator(Psi2IrConfiguration(false))
        val module = translator.generateModule(analyzerWithCompilerReport.analysisResult.moduleDescriptor,
                environment.getSourceFiles(),
                analyzerWithCompilerReport.analysisResult.bindingContext)

        module.accept(DumpIrTreeVisitor(out), "")
        emitLLVM(module, arguments.runtimeFile, arguments.outputFile)
        return ExitCode.OK
    }

    override fun setupPlatformSpecificArgumentsAndServices(configuration: CompilerConfiguration, arguments: K2NativeCompilerArguments, services: Services) {}

    override fun createArguments(): K2NativeCompilerArguments {
        return K2NativeCompilerArguments()
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            CLICompiler.doMain(K2Native(), args)
        }
    }
}
fun main(args: Array<String>) = K2Native.main(args)
