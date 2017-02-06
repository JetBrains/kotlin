package org.jetbrains.kotlin.cli.bc

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.util.profile
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
import org.jetbrains.kotlin.backend.konan.Distribution
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

        with (KonanConfigKeys) { with (configuration) {

            put(NOSTDLIB, arguments.nostdlib)
            put(NOLINK, arguments.nolink)
            put(LIBRARY_FILES, 
                arguments.libraries.toNonNullList())

            put(NATIVE_LIBRARY_FILES,
                    arguments.nativeLibraries.toNonNullList())

            // TODO: Collect all the explicit file names into an object 
            // and teach the compiler to work with temporaries and -save-temps.
            val bitcodeFile = if (arguments.nolink) {
                arguments.outputFile ?: "program.kt.bc"
            } else {
                "${arguments.outputFile ?: "program"}.kt.bc"
            }
            put(BITCODE_FILE, bitcodeFile)

            // This is a decision we could change
            put(CommonConfigurationKeys.MODULE_NAME, bitcodeFile)
            put(ABI_VERSION, 1)

            put(EXECUTABLE_FILE, 
                arguments.outputFile ?: "program.kexe")
            if (arguments.runtimeFile != null) 
                put(RUNTIME_FILE, arguments.runtimeFile)
            if (arguments.propertyFile != null) 
                put(PROPERTY_FILE, arguments.propertyFile)
            if (arguments.target != null) 
                put(TARGET, arguments.target)
            put(LIST_TARGETS, arguments.listTargets)
            put(OPTIMIZATION, arguments.optimization)

            put(PRINT_IR, arguments.printIr)
            put(PRINT_DESCRIPTORS, arguments.printDescriptors)
            put(PRINT_BITCODE, arguments.printBitCode)

            put(VERIFY_IR, arguments.verifyIr)
            put(VERIFY_DESCRIPTORS, arguments.verifyDescriptors)
            put(VERIFY_BITCODE, arguments.verifyBitCode)

            put(ENABLED_PHASES, 
                arguments.enablePhases.toNonNullList())
            put(DISABLED_PHASES, 
                arguments.disablePhases.toNonNullList())
            put(VERBOSE_PHASES, 
                arguments.verbosePhases.toNonNullList())
            put(LIST_PHASES, arguments.listPhases)
            put(TIME_PHASES, arguments.timePhases)
        }}
    }

    override fun createArguments(): K2NativeCompilerArguments {
        return K2NativeCompilerArguments()
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            profile("Total compiler main()") {
                CLICompiler.doMain(K2Native(), args)
            }
        }
    }
}
fun main(args: Array<String>) = K2Native.main(args)

