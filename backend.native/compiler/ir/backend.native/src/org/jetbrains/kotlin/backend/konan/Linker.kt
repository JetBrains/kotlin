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
            CompilerOutputKind.DYNAMIC_CACHE,
            CompilerOutputKind.DYNAMIC -> LinkerOutputKind.DYNAMIC_LIBRARY
            CompilerOutputKind.STATIC_CACHE,
            CompilerOutputKind.STATIC -> LinkerOutputKind.STATIC_LIBRARY
            CompilerOutputKind.PROGRAM -> LinkerOutputKind.EXECUTABLE
            else -> TODO("${context.config.produce} should not reach native linker stage")
        }

// TODO: We have a Linker.kt file in the shared module.
internal class Linker(val context: Context) {

    private val platform = context.config.platform
    private val config = context.config.configuration
    private val linkerOutput = determineLinkerOutput(context)
    private val linker = platform.linker
    private val target = context.config.target
    private val optimize = context.shouldOptimize()
    private val debug = context.config.debug || context.config.lightDebug

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
                Family.IOS,
                Family.TVOS,
                Family.WATCHOS -> dylibName
                Family.OSX -> "Versions/A/$dylibName"
                else -> error(target)
            }
            frameworkLinkerArgs = listOf("-install_name", "@rpath/${framework.name}/$dylibRelativePath")
            val dylibPath = framework.child(dylibRelativePath)
            dylibPath.parentFile.mkdirs()
            executable = dylibPath.absolutePath
        }

        val needsProfileLibrary = context.coverage.enabled

        val caches = determineCachesToLink(context)

        try {
            File(executable).delete()
            linker.linkCommands(objectFiles = objectFiles, executable = executable,
                    libraries = linker.linkStaticLibraries(includedBinaries) + context.config.defaultSystemLibraries +
                            caches.static.takeIf { context.config.produce != CompilerOutputKind.STATIC_CACHE }.orEmpty(),
                    linkerArgs = asLinkerArgs(config.getNotNull(KonanConfigKeys.LINKER_ARGS)) +
                            BitcodeEmbedding.getLinkerOptions(context.config) +
                            caches.dynamic +
                            libraryProvidedLinkerFlags + frameworkLinkerArgs,
                    optimize = optimize, debug = debug, kind = linkerOutput,
                    outputDsymBundle = context.config.outputFile + ".dSYM",
                    needsProfileLibrary = needsProfileLibrary).forEach {
                it.logWith(context::log)
                it.execute()
            }
        } catch (e: KonanExternalToolFailure) {
            context.reportCompilationError("${e.toolName} invocation reported errors")
        }
        return executable
    }

}

private class CachesToLink(val static: List<String>, val dynamic: List<String>)

private fun determineCachesToLink(context: Context): CachesToLink {
    val staticCaches = mutableListOf<String>()
    val dynamicCaches = mutableListOf<String>()

    // TODO: suboptimal, see e.g. [LlvmImports].
    context.librariesWithDependencies.forEach { library ->
        val currentBinaryContainsLibrary = context.llvmModuleSpecification.containsLibrary(library)
        val cache = context.config.cachedLibraries.getLibraryCache(library)
        val libraryIsCached = cache != null

        // Consistency check. Generally guaranteed by implementation.
        if (currentBinaryContainsLibrary && libraryIsCached) {
            error("Library ${library.libraryName} is found in both cache and current binary")
        } else if (!currentBinaryContainsLibrary && !libraryIsCached) {
            error("Library ${library.libraryName} is not found neither in cache nor in current binary")
        }

        if (cache != null) {
            val list = when (cache.kind) {
                CachedLibraries.Cache.Kind.DYNAMIC -> dynamicCaches
                CachedLibraries.Cache.Kind.STATIC -> staticCaches
            }

            list += cache.path
        }
    }

    return CachesToLink(static = staticCaches.distinct(), dynamic = dynamicCaches.distinct())
}
