package org.jetbrains.kotlin.cli.bc

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.konan.llvm.emitLLVM
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
import org.jetbrains.kotlin.psi.KtFile
import java.lang.System.out
import java.util.*

class NativeAnalyzer(
        val environment: KotlinCoreEnvironment, 
        val  sources: Collection<KtFile>, 
        val  config: KonanConfig) : AnalyzerWithCompilerReport.Analyzer {

    override fun  analyze(): AnalysisResult {
        return TopDownAnalyzerFacadeForKonan.analyzeFiles(sources, config);
    }

    override fun reportEnvironmentErrors() {
    }
}

class K2Native : CLICompiler<K2NativeCompilerArguments>() { 

    val defaultModuleName = "main";

    override fun doExecute(arguments     : K2NativeCompilerArguments,
                           configuration : CompilerConfiguration,
                           rootDisposable: Disposable
                          ): ExitCode {

        configuration.put(CommonConfigurationKeys.MODULE_NAME, defaultModuleName)

        configuration.addKotlinSourceRoots(arguments.freeArgs)

        val libraries = arguments.libraries?.asList<String>() ?: listOf<String>()
        configuration.put(KonanConfigurationKeys.LIBRARY_FILES, libraries)
        libraries.forEach{println(it)}

        val environment = KotlinCoreEnvironment.createForProduction(rootDisposable,
            configuration, Arrays.asList<String>("extensions/common.xml"))

        val collector = configuration.getNotNull(
            CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        val analyzerWithCompilerReport = AnalyzerWithCompilerReport(collector)

        val project = environment.project
        val config = KonanConfig(project, configuration)


        // Build AST and binding info.
        analyzerWithCompilerReport.analyzeAndReport(environment.getSourceFiles(),
                NativeAnalyzer(environment, environment.getSourceFiles(), config))

        // Translate AST to high level IR.
        val translator = Psi2IrTranslator(Psi2IrConfiguration(false))
        val module = translator.generateModule(
                                    analyzerWithCompilerReport.analysisResult.moduleDescriptor,
                                    environment.getSourceFiles(),
                                    analyzerWithCompilerReport.analysisResult.bindingContext)

        // Emit LLVM code.
        module.accept(DumpIrTreeVisitor(out), "")
        emitLLVM(module, arguments.runtimeFile, arguments.outputFile)

        return ExitCode.OK
    }

    override fun setupPlatformSpecificArgumentsAndServices(
            configuration: CompilerConfiguration,
            arguments    : K2NativeCompilerArguments,
            services     : Services) {}

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

