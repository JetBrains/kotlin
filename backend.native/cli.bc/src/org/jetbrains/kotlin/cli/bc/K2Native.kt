package org.jetbrains.kotlin.cli.bc

import org.jetbrains.kotlin.backend.konan.*
import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.config.addKotlinSourceRoots
import org.jetbrains.kotlin.ir.util.DumpIrTreeVisitor
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi.KtFile
import java.lang.System.out
import java.util.*

class K2Native : CLICompiler<K2NativeCompilerArguments>() { 


    override fun doExecute(arguments : K2NativeCompilerArguments,
                           configuration : CompilerConfiguration,
                           rootDisposable: Disposable
                          ): ExitCode {


        configuration.get(KonanConfigKeys.LIBRARY_FILES)?.forEach{ println(it) }

        val environment = KotlinCoreEnvironment.createForProduction(rootDisposable,
            configuration, Arrays.asList<String>("extensions/common.xml"))
        val project = environment.project
        val konanConfig = KonanConfig(project, configuration)

        runTopLevelPhases(konanConfig, environment)

        return ExitCode.OK
    }

    fun Array<String>?.toNonNullList(): List<String> {
        return this?.asList<String>() ?: listOf<String>()
    }


    // It is executed before doExecute().
    override fun setupPlatformSpecificArgumentsAndServices(
            configuration: CompilerConfiguration,
            arguments    : K2NativeCompilerArguments,
            services     : Services) {


        configuration.addKotlinSourceRoots(arguments.freeArgs)

        // This is a decision we could change
        configuration.put(CommonConfigurationKeys.MODULE_NAME, arguments.outputFile)

        configuration.put(KonanConfigKeys.LIBRARY_FILES, 
            arguments.libraries.toNonNullList())
        configuration.put(KonanConfigKeys.RUNTIME_FILE, arguments.runtimeFile)
        configuration.put(KonanConfigKeys.OUTPUT_FILE, arguments.outputFile)

        configuration.put(KonanConfigKeys.ABI_VERSION, 1)

        configuration.put(KonanConfigKeys.PRINT_IR, arguments.printIr)
        configuration.put(KonanConfigKeys.PRINT_DESCRIPTORS, arguments.printDescriptors)
        configuration.put(KonanConfigKeys.PRINT_BITCODE, arguments.printBitCode)

        configuration.put(KonanConfigKeys.VERIFY_IR, arguments.verifyIr)
        configuration.put(KonanConfigKeys.VERIFY_DESCRIPTORS, arguments.verifyDescriptors)
        configuration.put(KonanConfigKeys.VERIFY_BITCODE, arguments.verifyBitCode)

        configuration.put(KonanConfigKeys.ENABLED_PHASES, 
            arguments.enablePhases.toNonNullList())
        configuration.put(KonanConfigKeys.DISABLED_PHASES, 
            arguments.disablePhases.toNonNullList())
        configuration.put(KonanConfigKeys.VERBOSE_PHASES, 
            arguments.verbosePhases.toNonNullList())
        configuration.put(KonanConfigKeys.LIST_PHASES, arguments.listPhases)
    }

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

