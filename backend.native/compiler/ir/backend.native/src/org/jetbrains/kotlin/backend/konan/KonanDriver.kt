package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.konan.llvm.emitLLVM
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

class NativeAnalyzer(
        val environment: KotlinCoreEnvironment, 
        val  sources: Collection<KtFile>, 
        val  konanConfig: KonanConfig) : AnalyzerWithCompilerReport.Analyzer {

    override fun  analyze(): AnalysisResult {
        return TopDownAnalyzerFacadeForKonan.analyzeFiles(sources, konanConfig);
    }

    override fun reportEnvironmentErrors() {
    }
}

public fun runTopLevelPhases(konanConfig: KonanConfig, environment: KotlinCoreEnvironment) {

    val config = konanConfig.configuration

    KonanPhases.config(konanConfig)
    if (config.get(KonanConfigKeys.LIST_PHASES) ?: false) {
        KonanPhases.list()
    }

    val collector = config.getNotNull(
        CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

    val analyzerWithCompilerReport = AnalyzerWithCompilerReport(collector)

    // Build AST and binding info.
    analyzerWithCompilerReport.analyzeAndReport(environment.getSourceFiles(),
        NativeAnalyzer(environment, environment.getSourceFiles(), konanConfig))

    val bindingContext = analyzerWithCompilerReport.analysisResult.bindingContext
    val moduleDescriptor = analyzerWithCompilerReport.analysisResult.moduleDescriptor

    val context = Context(konanConfig, bindingContext, moduleDescriptor)

    // Translate AST to high level IR.
    val translator = Psi2IrTranslator(Psi2IrConfiguration(false))
    val module = translator.generateModule( moduleDescriptor,
        environment.getSourceFiles(), bindingContext)

    context.irModule = module
    val phaser = PhaseManager(context)

    phaser.phase("Optimizer") {
        KonanLower(context).lower(module)
    }

    phaser.phase("Bitcode") {
        emitLLVM(context)
    }

    phaser.phase("Linker") {
        //TODO: We don't have it yet.
        // invokeLinker()
    }
}

