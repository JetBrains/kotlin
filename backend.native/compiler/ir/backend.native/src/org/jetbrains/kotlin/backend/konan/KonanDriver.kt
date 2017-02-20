package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.konan.ir.ModuleIndex
import org.jetbrains.kotlin.backend.konan.llvm.emitLLVM
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.kotlinSourceRoots
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator

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

    val targets = TargetManager(config)
    if (config.get(KonanConfigKeys.LIST_TARGETS) ?: false) {
        targets.list()
    }

    KonanPhases.config(konanConfig)
    if (config.get(KonanConfigKeys.LIST_PHASES) ?: false) {
        KonanPhases.list()
    }

    if (config.kotlinSourceRoots.isEmpty()) return

    val collector = config.getNotNull(
        CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

    val analyzerWithCompilerReport = AnalyzerWithCompilerReport(collector)

    val context = Context(konanConfig)
    val phaser = PhaseManager(context)

    phaser.phase(KonanPhase.FRONTEND) {
        // Build AST and binding info.
        analyzerWithCompilerReport.analyzeAndReport(environment.getSourceFiles(),
            NativeAnalyzer(environment, environment.getSourceFiles(), konanConfig))
        if (analyzerWithCompilerReport.hasErrors()) {
            throw KonanCompilationException()
        }
        context.moduleDescriptor = analyzerWithCompilerReport.analysisResult.moduleDescriptor
    }

    val bindingContext = analyzerWithCompilerReport.analysisResult.bindingContext

    phaser.phase(KonanPhase.PSI_TO_IR) {
        // Translate AST to high level IR.
        val translator = Psi2IrTranslator(Psi2IrConfiguration(false))
        val module = translator.generateModule( context.moduleDescriptor!!,
            environment.getSourceFiles(), bindingContext)

        context.irModule = module
    }
    phaser.phase(KonanPhase.BACKEND) {
        phaser.phase(KonanPhase.LOWER) {
            KonanLower(context).lower()
            context.ir.moduleIndexForCodegen = ModuleIndex(context.ir.irModule)
        }
        phaser.phase(KonanPhase.BITCODE) {
            emitLLVM(context)
        }
    }

    phaser.phase(KonanPhase.LINKER) {
        LinkStage(context).linkStage()
    }
}

