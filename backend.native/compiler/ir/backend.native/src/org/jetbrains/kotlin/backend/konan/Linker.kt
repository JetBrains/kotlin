package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.konan.KonanExternalToolFailure
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.LinkerOutputKind

internal fun determineLinkerOutput(context: Context): LinkerOutputKind =
        when (context.config.produce) {
            CompilerOutputKind.FRAMEWORK -> {
                val staticFramework = context.config.produceStaticFramework
                if (staticFramework) LinkerOutputKind.STATIC_LIBRARY else LinkerOutputKind.DYNAMIC_LIBRARY
            }
            CompilerOutputKind.DYNAMIC -> LinkerOutputKind.DYNAMIC_LIBRARY
            CompilerOutputKind.STATIC -> LinkerOutputKind.STATIC_LIBRARY
            CompilerOutputKind.PROGRAM -> LinkerOutputKind.EXECUTABLE
            else -> TODO("${context.config.produce} should not reach native linker stage")
        }

// TODO: We have a Linker.kt file in the shared module.
internal class Linker(val context: Context) {

    private val platform = context.config.platform
    private val config = context.config.configuration
    private val linkerOutput = determineLinkerOutput(context)
    private val nomain = config.get(KonanConfigKeys.NOMAIN) ?: false
    private val linker = platform.linker
    private val target = context.config.target
    private val optimize = context.shouldOptimize()
    private val debug = context.config.debug

    // Ideally we'd want to have
    //      #pragma weak main = Konan_main
    // in the launcher.cpp.
    // Unfortunately, anything related to weak linking on MacOS
    // only seems to be working with dynamic libraries.
    // So we stick to "-alias _main _konan_main" on Mac.
    // And just do the same on Linux.
    private val entryPointSelector: List<String>
        get() = if (nomain || linkerOutput != LinkerOutputKind.EXECUTABLE) emptyList() else platform.entrySelector

    fun link(objectFiles: List<ObjectFile>) {
        val nativeDependencies = context.llvm.nativeDependenciesToLink
        val includedBinaries = nativeDependencies.map { it.includedPaths }.flatten()
        val libraryProvidedLinkerFlags = nativeDependencies.map { it.linkerOpts }.flatten()
        runLinker(objectFiles, includedBinaries, libraryProvidedLinkerFlags)
    }

    private fun asLinkerArgs(args: List<String>): List<String> {
        if (linker.useCompilerDriverAsLinker) {
            return args
        }

        val result = mutableListOf<String>()
        for (arg in args) {
            // If user passes compiler arguments to us - transform them to linker ones.
            if (arg.startsWith("-Wl,")) {
                result.addAll(arg.substring(4).split(','))
            } else {
                result.add(arg)
            }
        }
        return result
    }

    private fun runLinker(objectFiles: List<ObjectFile>,
                          includedBinaries: List<String>,
                          libraryProvidedLinkerFlags: List<String>): ExecutableFile? {
        val frameworkLinkerArgs: List<String>
        val executable: String

        if (context.config.produce != CompilerOutputKind.FRAMEWORK) {
            frameworkLinkerArgs = emptyList()
            executable = context.config.outputFile
        } else {
            val framework = File(context.config.outputFile)
            val dylibName = framework.name.removeSuffix(".framework")
            val dylibRelativePath = when (target.family) {
                Family.IOS -> dylibName
                Family.OSX -> "Versions/A/$dylibName"
                else -> error(target)
            }
            frameworkLinkerArgs = listOf("-install_name", "@rpath/${framework.name}/$dylibRelativePath")
            val dylibPath = framework.child(dylibRelativePath)
            dylibPath.parentFile.mkdirs()
            executable = dylibPath.absolutePath
        }

        try {
            File(executable).delete()
            linker.linkCommands(objectFiles = objectFiles, executable = executable,
                    libraries = linker.linkStaticLibraries(includedBinaries) + context.config.defaultSystemLibraries,
                    linkerArgs = entryPointSelector +
                            asLinkerArgs(config.getNotNull(KonanConfigKeys.LINKER_ARGS)) +
                            BitcodeEmbedding.getLinkerOptions(context.config) +
                            libraryProvidedLinkerFlags + frameworkLinkerArgs,
                    optimize = optimize, debug = debug, kind = linkerOutput,
                    outputDsymBundle = context.config.outputFile + ".dSYM").forEach {
                it.logWith(context::log)
                it.execute()
            }
        } catch (e: KonanExternalToolFailure) {
            context.reportCompilationError("${e.toolName} invocation reported errors")
        }
        return executable
    }

}