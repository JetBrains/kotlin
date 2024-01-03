/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*

class K2NativeCompilerArguments : CommonCompilerArguments() {
    // First go the options interesting to the general public.
    // Prepend them with a single dash.
    // Keep the list lexically sorted.

    @Argument(value = "-enable-assertions", deprecatedName = "-enable_assertions", shortName = "-ea", description = "Enable runtime assertions in generated code.")
    var enableAssertions: Boolean = false

    @Argument(value = "-g", description = "Enable the emission of debug information.")
    var debug: Boolean = false

    @Argument(
        value = "-generate-test-runner",
        deprecatedName = "-generate_test_runner",
        shortName = "-tr", description = "Produce a runner for unit tests."
    )
    var generateTestRunner = false

    @Argument(
        value = "-generate-worker-test-runner",
        shortName = "-trw",
        description = "Produce a worker runner for unit tests."
    )
    var generateWorkerTestRunner = false

    @Argument(
        value = "-generate-no-exit-test-runner",
        shortName = "-trn",
        description = "Produce a runner for unit tests that doesn't force an exit."
    )
    var generateNoExitTestRunner = false

    @Argument(value="-include-binary", deprecatedName = "-includeBinary", shortName = "-ib", valueDescription = "<path>", description = "Pack the given external binary into the klib.")
    var includeBinaries: Array<String>? = null

    @Argument(value = "-library", shortName = "-l", valueDescription = "<path>", description = "Link with the given library.", delimiter = Argument.Delimiters.none)
    var libraries: Array<String>? = null

    @Argument(value = "-library-version", shortName = "-lv", valueDescription = "<version>", description = "Set the library version.")
    var libraryVersion: String? = null

    @Argument(value = "-list-targets", deprecatedName = "-list_targets", description = "List available hardware targets.")
    var listTargets: Boolean = false

    @Argument(value = "-manifest", valueDescription = "<path>", description = "Provide a manifest addend file.")
    var manifestFile: String? = null

    @Argument(value="-memory-model", valueDescription = "<model>", description = "Choose the memory model to be used – 'strict' and 'experimental' are currently supported.")
    var memoryModel: String? = null

    @GradleOption(
        value = DefaultValue.STRING_NULL_DEFAULT,
        gradleInputType = GradleInputTypes.INPUT
    )
    @Argument(
        value = "-module-name",
        deprecatedName = "-module_name",
        valueDescription = "<name>",
        description = "Specify a name for the compilation module."
    )
    var moduleName: String? = null

    @Argument(
        value = "-native-library",
        deprecatedName = "-nativelibrary",
        shortName = "-nl",
        valueDescription = "<path>",
        description = "Include the given native bitcode library.", delimiter = Argument.Delimiters.none
    )
    var nativeLibraries: Array<String>? = null

    @Argument(value = "-no-default-libs", deprecatedName = "-nodefaultlibs", description = "Don't link the libraries from dist/klib automatically.")
    var nodefaultlibs: Boolean = false

    @Argument(
        value = "-no-endorsed-libs",
        description = "Don't link endorsed libraries from the dist automatically. " +
                "This option has been deprecated, as the dist no longer has any endorsed libraries."
    )
    var noendorsedlibs: Boolean = false

    @Argument(value = "-nomain", description = "Assume the 'main' entry point will be provided by external libraries.")
    var nomain: Boolean = false

    @Argument(value = "-nopack", description = "Don't pack the library into a klib file.")
    var nopack: Boolean = false

    @Argument(value="-linker-options", deprecatedName = "-linkerOpts", valueDescription = "<arg>", description = "Pass arguments to the linker.", delimiter = " ")
    var linkerArguments: Array<String>? = null

    @Argument(value="-linker-option", valueDescription = "<arg>", description = "Pass the given argument to the linker.", delimiter = Argument.Delimiters.none)
    var singleLinkerArguments: Array<String>? = null

    @Argument(value = "-nostdlib", description = "Don't link with the stdlib.")
    var nostdlib: Boolean = false

    @Argument(value = "-opt", description = "Enable optimizations during compilation.")
    var optimization: Boolean = false

    @Argument(value = "-output", shortName = "-o", valueDescription = "<name>", description = "Output name.")
    var outputName: String? = null

    @Argument(value = "-entry", shortName = "-e", valueDescription = "<name>", description = "Qualified entry point name.")
    var mainPackage: String? = null

    @Argument(
        value = "-produce", shortName = "-p",
        valueDescription = "{program|static|dynamic|framework|library|bitcode}",
        description = "Specify the output file kind."
    )
    var produce: String? = null

    // TODO: remove after 2.0, KT-61098
    @Argument(
        value = "-repo",
        shortName = "-r",
        valueDescription = "<path>",
        description = "Library search path.\n" +
                "Note: This option is deprecated and will be removed in one of the future releases.\n" +
                "Please use library paths instead of library names in all compiler options such as '-library' ('-l')."
    )
    var repositories: Array<String>? = null

    @Argument(value = "-target", valueDescription = "<target>", description = "Set the hardware target.")
    var target: String? = null

    // The rest of the options are only interesting to the developers.
    // Make sure to prepend them with -X.
    // Keep the list lexically sorted.

    @Argument(
        value = "-Xbundle-id",
        valueDescription = "<id>",
        description = "Bundle ID to be set in the Info.plist file of the produced framework. This option is deprecated. Please use '-Xbinary=bundleId=<id>'."
    )
    var bundleId: String? = null

    @Argument(
        value = "-Xcache-directory",
        valueDescription = "<path>",
        description = "Path to the directory containing caches.",
        delimiter = Argument.Delimiters.none
    )
    var cacheDirectories: Array<String>? = null

    @Argument(
        value = CACHED_LIBRARY,
        valueDescription = "<library path>,<cache path>",
        description = "Paths to a library and its cache, separated by a comma.",
        delimiter = Argument.Delimiters.none
    )
    var cachedLibraries: Array<String>? = null

    @Argument(
        value = "-Xauto-cache-from",
        valueDescription = "<path>",
        description = """Path to the root directory from which dependencies are to be cached automatically.
By default caches will be placed into the kotlin-native system cache directory.""",
        delimiter = Argument.Delimiters.none
    )
    var autoCacheableFrom: Array<String>? = null

    @Argument(
        value = "-Xauto-cache-dir",
        valueDescription = "<path>",
        description = "Path to the directory where caches for auto-cacheable dependencies should be put.",
        delimiter = Argument.Delimiters.none
    )
    var autoCacheDir: String? = null

    @Argument(
        value = INCREMENTAL_CACHE_DIR,
        valueDescription = "<path>",
        description = "Path to the directory where incremental build caches should be put.",
        delimiter = ""
    )
    var incrementalCacheDir: String? = null

    @Argument(value="-Xcheck-dependencies", deprecatedName = "--check_dependencies", description = "Check dependencies and download the missing ones.")
    var checkDependencies: Boolean = false

    @Argument(value = EMBED_BITCODE_FLAG, description = "Embed LLVM IR bitcode as data.")
    var embedBitcode: Boolean = false

    @Argument(value = EMBED_BITCODE_MARKER_FLAG, description = "Embed placeholder LLVM IR data as a marker.")
    var embedBitcodeMarker: Boolean = false

    @Argument(value = "-Xemit-lazy-objc-header", description = "")
    var emitLazyObjCHeader: String? = null

    @Argument(
        value = "-Xexport-library",
        valueDescription = "<path>",
        description = """A library to be included in the produced framework API.
This library must be one of the ones passed with '-library'.""",
        delimiter = Argument.Delimiters.none
    )
    var exportedLibraries: Array<String>? = null

    @Argument(
        value = "-Xexternal-dependencies",
        valueDescription = "<path>",
        description = """Path to the file containing external dependencies.
External dependencies are required for verbose output in the event of IR linker errors,
but they do not affect compilation at all."""
    )
    var externalDependencies: String? = null

    @Argument(value="-Xfake-override-validator", description = "Enable the IR fake override validator.")
    var fakeOverrideValidator: Boolean = false

    @Argument(
        value = "-Xframework-import-header",
        valueDescription = "<header>",
        description = "Add an additional header import to the framework header."
    )
    var frameworkImportHeaders: Array<String>? = null

    @Argument(
        value = "-Xadd-light-debug",
        valueDescription = "{disable|enable}",
        description = """Add light debug information for optimized builds. This option is skipped in debug builds.
It's enabled by default on Darwin platforms where collected debug information is stored in a .dSYM file.
Currently this option is disabled by default on other platforms."""
    )
    var lightDebugString: String? = null

    // TODO: remove after 1.4 release.
    @Argument(value = "-Xg0", description = "Add light debug information. This option has been deprecated. Please use '-Xadd-light-debug=enable' instead.")
    var lightDebugDeprecated: Boolean = false

    @Argument(
        value = "-Xg-generate-debug-trampoline",
        valueDescription = "{disable|enable}",
        description = """Generate trampolines to make debugger breakpoint resolution more accurate (inlines, 'when', etc.)."""
    )
    var generateDebugTrampolineString: String? = null


    @Argument(
        value = ADD_CACHE,
        valueDescription = "<path>",
        description = "Path to a library to be added to the cache.",
        delimiter = Argument.Delimiters.none
    )
    var libraryToAddToCache: String? = null

    @Argument(
        value = "-Xfile-to-cache",
        valueDescription = "<path>",
        description = "Path to the file to cache.",
        delimiter = Argument.Delimiters.none
    )
    var filesToCache: Array<String>? = null

    @Argument(value = "-Xmake-per-file-cache", description = "Force the compiler to produce per-file caches.")
    var makePerFileCache: Boolean = false

    @Argument(
        value = "-Xbackend-threads",
        valueDescription = "<N>",
        description = """Run codegen by file in N parallel threads.
0 means use one thread per processor core.
The default value is 1."""
    )
    var backendThreads: String = "1"

    @Argument(value = "-Xexport-kdoc", description = "Export KDoc entries in the framework header.")
    var exportKDoc: Boolean = false

    @Argument(value = "-Xprint-bitcode", deprecatedName = "--print_bitcode", description = "Print LLVM bitcode.")
    var printBitCode: Boolean = false

    @Argument(value = "-Xcheck-state-at-external-calls", description = "Ensure that all calls of possibly long external functions are done in the native thread state.")
    var checkExternalCalls: Boolean? = null

    @Argument(value = "-Xprint-ir", deprecatedName = "--print_ir", description = "Print IR.")
    var printIr: Boolean = false

    @Argument(value = "-Xprint-files", description = "Print files.")
    var printFiles: Boolean = false

    @Argument(value="-Xpurge-user-libs", deprecatedName = "--purge_user_libs", description = "Don't link unused libraries even if explicitly specified.")
    var purgeUserLibs: Boolean = false

    @Argument(value = "-Xruntime", deprecatedName = "--runtime", valueDescription = "<path>", description = "Override the standard 'runtime.bc' location.")
    var runtimeFile: String? = null

    @Argument(
        value = INCLUDE_ARG,
        valueDescription = "<path>",
        description = "A path to an intermediate library that should be processed in the same manner as source files."
    )
    var includes: Array<String>? = null

    @Argument(
        value = SHORT_MODULE_NAME_ARG,
        valueDescription = "<name>",
        description = "A short name used to denote this library in the IDE and in a generated Objective-C header."
    )
    var shortModuleName: String? = null

    @Argument(value = STATIC_FRAMEWORK_FLAG, description = "Create a framework with a static library instead of a dynamic one.")
    var staticFramework: Boolean = false

    @Argument(value = "-Xtemporary-files-dir", deprecatedName = "--temporary_files_dir", valueDescription = "<path>", description = "Save temporary files to the given directory.")
    var temporaryFilesDir: String? = null

    @Argument(value = "-Xsave-llvm-ir-after", description = "Save the result of the Kotlin IR to LLVM IR translation to '-Xsave-llvm-ir-directory'.")
    var saveLlvmIrAfter: Array<String> = emptyArray()

    @Argument(value = "-Xverify-bitcode", deprecatedName = "--verify_bitcode", description = "Verify LLVM bitcode after each method.")
    var verifyBitCode: Boolean = false

    @Argument(
        value = "-Xverify-ir",
        valueDescription = "{none|warning|error}",
        description = "IR verification mode (no verification by default)."
    )
    var verifyIr: String? = null

    @Argument(value = "-Xverify-compiler", description = "Verify the compiler.")
    var verifyCompiler: String? = null

    @Argument(
        value = "-friend-modules",
        valueDescription = "<path>",
        description = "Paths to friend modules."
    )
    var friendModules: String? = null

    /**
     * @see K2MetadataCompilerArguments.refinesPaths
     */
    @Argument(
        value = "-Xrefines-paths",
        valueDescription = "<path>",
        description = "Paths to output directories for refined modules (modules whose 'expect' declarations this module can actualize)."
    )
    var refinesPaths: Array<String>? = null

    @Argument(value = "-Xdebug-info-version", description = "Generate debug info of the given version (1, 2).")
    var debugInfoFormatVersion: String = "1" /* command line parser doesn't accept kotlin.Int type */

    @Argument(value = "-Xno-objc-generics", description = "Disable generics support for framework header.")
    var noObjcGenerics: Boolean = false

    @Argument(value="-Xoverride-clang-options", valueDescription = "<arg1,arg2,...>", description = "Explicit list of Clang options.")
    var clangOptions: Array<String>? = null

    @Argument(value="-Xallocator", valueDescription = "std | mimalloc | custom", description = "Allocator used at runtime.")
    var allocator: String? = null

    @Argument(
        value = "-Xheader-klib-path",
        description = "Save a klib that only contains the public ABI to the given path."
    )
    var headerKlibPath: String? = null

    @Argument(value = "-Xdebug-prefix-map", valueDescription = "<old1=new1,old2=new2,...>", description = "Remap file source directory paths in debug info.")
    var debugPrefixMap: Array<String>? = null

    @Argument(
        value = "-Xpre-link-caches",
        valueDescription = "{disable|enable}",
        description = "Perform caches pre-linking."
    )
    var preLinkCaches: String? = null

    // We use `;` as delimiter because properties may contain comma-separated values.
    // For example, target cpu features.
    @Argument(
        value = "-Xoverride-konan-properties",
        valueDescription = "key1=value1;key2=value2;...",
        description = "Override values from 'konan.properties' with the given ones.",
        delimiter = ";"
    )
    var overrideKonanProperties: Array<String>? = null

    @Argument(value="-Xdestroy-runtime-mode", valueDescription = "<mode>", description = "When to destroy the runtime – 'legacy' and 'on-shutdown' are currently supported. Note that 'legacy' mode is deprecated and will be removed.")
    var destroyRuntimeMode: String? = null

    @Argument(value="-Xgc", valueDescription = "<gc>", description = "GC to use – 'noop', 'stms', and 'cms' are currently supported. This works only with '-memory-model experimental'.")
    var gc: String? = null

    @Argument(value = "-Xir-property-lazy-initialization", valueDescription = "{disable|enable}", description = "Initialize top level properties lazily per file.")
    var propertyLazyInitialization: String? = null

    // TODO: Remove when legacy MM is gone.
    @Argument(
        value = "-Xworker-exception-handling",
        valueDescription = "<mode>",
        description = "Unhandled exception processing in 'Worker.executeAfter'. Possible values: 'legacy' and 'use-hook'. The default value is 'legacy' and for '-memory-model experimental', the default value is 'use-hook'."
    )
    var workerExceptionHandling: String? = null

    @Argument(
        value = "-Xllvm-variant",
        valueDescription = "{dev|user|absolute path to llvm}",
        description = "Choose the LLVM distribution that will be used during compilation."
    )
    var llvmVariant: String? = null

    @Argument(
        value = "-Xbinary",
        valueDescription = "<option=value>",
        description = "Specify a binary option."
    )
    var binaryOptions: Array<String>? = null

    @Argument(value = "-Xruntime-logs", valueDescription = "<tag1=level1,tag2=level2,...>", description = "Enable logging of Native runtime internals.")
    var runtimeLogs: String? = null

    @Argument(
        value = "-Xdump-tests-to",
        valueDescription = "<path>",
        description = "Path to a file for dumping the list of all available tests."
    )
    var testDumpOutputPath: String? = null

    @Argument(value = "-Xlazy-ir-for-caches", valueDescription = "{disable|enable}", description = "Use lazy IR for cached libraries.")
    var lazyIrForCaches: String? = null

    @Argument(value = "-Xpartial-linkage", valueDescription = "{enable|disable}", description = "Use partial linkage mode.")
    var partialLinkageMode: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-Xpartial-linkage-loglevel", valueDescription = "{info|warning|error}", description = "Define the compile-time log level for partial linkage.")
    var partialLinkageLogLevel: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-Xomit-framework-binary", description = "Omit binary when compiling the framework.")
    var omitFrameworkBinary: Boolean = false

    @Argument(value = "-Xcompile-from-bitcode", description = "Continue compilation from the given bitcode file.", valueDescription = "<path>")
    var compileFromBitcode: String? = null

    @Argument(
        value = "-Xread-dependencies-from",
        description = "Serialized dependencies to use for linking.",
        valueDescription = "<path>"
    )
    var serializedDependencies: String? = null

    @Argument(value = "-Xwrite-dependencies-to", description = "Path for writing backend dependencies.")
    var saveDependenciesPath: String? = null

    @Argument(value = "-Xsave-llvm-ir-directory", description = "Directory that should contain the results of '-Xsave-llvm-ir-after=<phase>'.")
    var saveLlvmIrDirectory: String? = null

    @Argument(value = "-Xkonan-data-dir", description = "Custom path to the location of konan distributions.")
    var konanDataDir: String? = null

    override fun configureAnalysisFlags(collector: MessageCollector, languageVersion: LanguageVersion): MutableMap<AnalysisFlag<*>, Any> =
        super.configureAnalysisFlags(collector, languageVersion).also {
            val optInList = it[AnalysisFlags.optIn] as List<*>
            it[AnalysisFlags.optIn] = optInList + listOf("kotlin.ExperimentalUnsignedTypes")
            if (printIr)
                phasesToDumpAfter = arrayOf("ALL")
            if (metadataKlib) {
                it[AnalysisFlags.metadataCompilation] = true
            }
        }

    override fun checkIrSupport(languageVersionSettings: LanguageVersionSettings, collector: MessageCollector) {
        if (languageVersionSettings.languageVersion < LanguageVersion.KOTLIN_1_4
            || languageVersionSettings.apiVersion < ApiVersion.KOTLIN_1_4
        ) {
            collector.report(
                severity = CompilerMessageSeverity.ERROR,
                message = "Native backend cannot be used with language or API version below 1.4"
            )
        }
    }

    override fun copyOf(): Freezable = copyK2NativeCompilerArguments(this, K2NativeCompilerArguments())

    companion object {
        const val EMBED_BITCODE_FLAG = "-Xembed-bitcode"
        const val EMBED_BITCODE_MARKER_FLAG = "-Xembed-bitcode-marker"
        const val STATIC_FRAMEWORK_FLAG = "-Xstatic-framework"
        const val INCLUDE_ARG = "-Xinclude"
        const val CACHED_LIBRARY = "-Xcached-library"
        const val ADD_CACHE = "-Xadd-cache"
        const val INCREMENTAL_CACHE_DIR = "-Xic-cache-dir"
        const val SHORT_MODULE_NAME_ARG = "-Xshort-module-name"
    }
}
