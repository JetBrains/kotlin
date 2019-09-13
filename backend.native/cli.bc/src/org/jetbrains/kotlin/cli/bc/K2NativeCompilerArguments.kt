/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.cli.bc

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.AnalysisFlags

class K2NativeCompilerArguments : CommonCompilerArguments() {
    // First go the options interesting to the general public.
    // Prepend them with a single dash.
    // Keep the list lexically sorted.

    @Argument(value = "-enable-assertions", deprecatedName = "-enable_assertions", shortName = "-ea", description = "Enable runtime assertions in generated code")
    var enableAssertions: Boolean = false

    @Argument(value = "-g", description = "Enable emitting debug information")
    var debug: Boolean = false

    @Argument(value = "-generate-test-runner", deprecatedName = "-generate_test_runner",
            shortName = "-tr", description = "Produce a runner for unit tests")
    var generateTestRunner = false
    @Argument(value = "-generate-worker-test-runner",
            shortName = "-trw", description = "Produce a worker runner for unit tests")
    var generateWorkerTestRunner = false
    @Argument(value = "-generate-no-exit-test-runner",
            shortName = "-trn", description = "Produce a runner for unit tests not forcing exit")
    var generateNoExitTestRunner = false

    @Argument(value="-include-binary", deprecatedName = "-includeBinary", shortName = "-ib", valueDescription = "<path>", description = "Pack external binary within the klib")
    var includeBinaries: Array<String>? = null

    @Argument(value = "-library", shortName = "-l", valueDescription = "<path>", description = "Link with the library", delimiter = "")
    var libraries: Array<String>? = null

    @Argument(value = "-library-version", shortName = "-lv", valueDescription = "<version>", description = "Set library version")
    var libraryVersion: String? = null

    @Argument(value = "-list-targets", deprecatedName = "-list_targets", description = "List available hardware targets")
    var listTargets: Boolean = false

    @Argument(value = "-manifest", valueDescription = "<path>", description = "Provide a maniferst addend file")
    var manifestFile: String? = null

    @Argument(value="-memory-model", valueDescription = "<model>", description = "Memory model to use, 'strict' and 'relaxed' are currently supported")
    var memoryModel: String? = "strict"

    @Argument(value="-module-name", deprecatedName = "-module_name", valueDescription = "<name>", description = "Specify a name for the compilation module")
    var moduleName: String? = null

    @Argument(value = "-native-library", deprecatedName = "-nativelibrary", shortName = "-nl",
            valueDescription = "<path>", description = "Include the native bitcode library", delimiter = "")
    var nativeLibraries: Array<String>? = null

    @Argument(value = "-no-default-libs", deprecatedName = "-nodefaultlibs", description = "Don't link the libraries from dist/klib automatically")
    var nodefaultlibs: Boolean = false

    @Argument(value = "-no-endorsed-libs", description = "Don't link the endorsed libraries from dist automatically")
    var noendorsedlibs: Boolean = false

    @Argument(value = "-nomain", description = "Assume 'main' entry point to be provided by external libraries")
    var nomain: Boolean = false

    @Argument(value = "-nopack", description = "Don't pack the library into a klib file")
    var nopack: Boolean = false

    @Argument(value="-linker-options", deprecatedName = "-linkerOpts", valueDescription = "<arg>", description = "Pass arguments to linker", delimiter = " ")
    var linkerArguments: Array<String>? = null

    @Argument(value="-linker-option", valueDescription = "<arg>", description = "Pass argument to linker", delimiter = "")
    var singleLinkerArguments: Array<String>? = null

    @Argument(value = "-nostdlib", description = "Don't link with stdlib")
    var nostdlib: Boolean = false

    @Argument(value = "-opt", description = "Enable optimizations during compilation")
    var optimization: Boolean = false

    @Argument(value = "-output", shortName = "-o", valueDescription = "<name>", description = "Output name")
    var outputName: String? = null

    @Argument(value = "-entry", shortName = "-e", valueDescription = "<name>", description = "Qualified entry point name")
    var mainPackage: String? = null

    @Argument(value = "-produce", shortName = "-p",
            valueDescription = "{program|static|dynamic|framework|library|bitcode}",
            description = "Specify output file kind")
    var produce: String? = null

    @Argument(value = "-repo", shortName = "-r", valueDescription = "<path>", description = "Library search path")
    var repositories: Array<String>? = null

    @Argument(value = "-target", valueDescription = "<target>", description = "Set hardware target")
    var target: String? = null

    // The rest of the options are only interesting to the developers.
    // Make sure to prepend them with -X.
    // Keep the list lexically sorted.

    @Argument(
            value = "-Xcache-directory",
            valueDescription = "<path>",
            description = "Path to the directory containing caches",
            delimiter = ""
    )
    var cacheDirectories: Array<String>? = null

    @Argument(
            value = CACHED_LIBRARY,
            valueDescription = "<library path>,<cache path>",
            description = "Comma-separated paths of a library and its cache",
            delimiter = ""
    )
    var cachedLibraries: Array<String>? = null

    @Argument(value="-Xcheck-dependencies", deprecatedName = "--check_dependencies", description = "Check dependencies and download the missing ones")
    var checkDependencies: Boolean = false

    @Argument(value="-Xcompatible-compiler-version", valueDescription = "<version>", description = "Assume the given compiler version to be binary compatible")
    var compatibleCompilerVersions: Array<String>? = null

    @Argument(value = EMBED_BITCODE_FLAG, description = "Embed LLVM IR bitcode as data")
    var embedBitcode: Boolean = false

    @Argument(value = EMBED_BITCODE_MARKER_FLAG, description = "Embed placeholder LLVM IR data as a marker")
    var embedBitcodeMarker: Boolean = false

    @Argument(value = "-Xemit-lazy-objc-header", description = "")
    var emitLazyObjCHeader: String? = null

    @Argument(value = "-Xenable", deprecatedName = "--enable", valueDescription = "<Phase>", description = "Enable backend phase")
    var enablePhases: Array<String>? = null

    @Argument(
            value = "-Xexport-library",
            valueDescription = "<path>",
            description = "A library to be included into produced framework API.\n" +
                    "Must be one of libraries passed with '-library'",
            delimiter = ""
    )
    var exportedLibraries: Array<String>? = null

    @Argument(
            value = "-Xframework-import-header",
            valueDescription = "<header>",
            description = "Add additional header import to framework header"
    )
    var frameworkImportHeaders: Array<String>? = null

    @Argument(value = "-Xg0", description = "Add light debug information")
    var lightDebug: Boolean = false

    @Argument(
            value = MAKE_CACHE,
            valueDescription = "<path>",
            description = "Path of the library to be compiled to cache",
            delimiter = ""
    )
    var librariesToCache: Array<String>? = null

    @Argument(value = "-Xprint-bitcode", deprecatedName = "--print_bitcode", description = "Print llvm bitcode")
    var printBitCode: Boolean = false

    @Argument(value = "-Xprint-descriptors", deprecatedName = "--print_descriptors", description = "Print descriptor tree")
    var printDescriptors: Boolean = false

    @Argument(value = "-Xprint-ir", deprecatedName = "--print_ir", description = "Print IR")
    var printIr: Boolean = false

    @Argument(value = "-Xprint-ir-with-descriptors", deprecatedName = "--print_ir_with_descriptors", description = "Print IR with descriptors")
    var printIrWithDescriptors: Boolean = false

    @Argument(value = "-Xprint-locations", deprecatedName = "--print_locations", description = "Print locations")
    var printLocations: Boolean = false

    @Argument(value="-Xpurge-user-libs", deprecatedName = "--purge_user_libs", description = "Don't link unused libraries even explicitly specified")
    var purgeUserLibs: Boolean = false

    @Argument(value = "-Xruntime", deprecatedName = "--runtime", valueDescription = "<path>", description = "Override standard 'runtime.bc' location")
    var runtimeFile: String? = null

    @Argument(
        value = INCLUDE_ARG,
        valueDescription = "<path>",
        description = "A path to an intermediate library that should be processed in the same manner as source files.\n"
    )
    var includes: Array<String>? = null

    @Argument(value = STATIC_FRAMEWORK_FLAG, description = "Create a framework with a static library instead of a dynamic one")
    var staticFramework: Boolean = false

    @Argument(value = "-Xtemporary-files-dir", deprecatedName = "--temporary_files_dir", valueDescription = "<path>", description = "Save temporary files to the given directory")
    var temporaryFilesDir: String? = null

    @Argument(value = "-Xverify-bitcode", deprecatedName = "--verify_bitcode", description = "Verify llvm bitcode after each method")
    var verifyBitCode: Boolean = false

    @Argument(value = "-Xverify-compiler", description = "Verify compiler")
    var verifyCompiler: String? = null

    @Argument(
            value = "-friend-modules",
            valueDescription = "<path>",
            description = "Paths to friend modules"
    )
    var friendModules: String? = null

    @Argument(value = "-Xdebug-info-version", description = "generate debug info of given version (1, 2)")
    var debugInfoFormatVersion: String = "1" /* command line parser doesn't accept kotlin.Int type */

    @Argument(value = "-Xcoverage", description = "emit coverage")
    var coverage: Boolean = false

    @Argument(
            value = "-Xlibrary-to-cover",
            valueDescription = "<path>",
            description = "Provide code coverage for the given library.\n" +
                    "Must be one of libraries passed with '-library'",
            delimiter = ""
    )
    var coveredLibraries: Array<String>? = null

    @Argument(value = "-Xcoverage-file", valueDescription = "<path>", description = "Save coverage information to the given file")
    var coverageFile: String? = null

    @Argument(value = "-Xobjc-generics", description = "Enable experimental generics support for framework header")
    var objcGenerics: Boolean = false

    override fun configureAnalysisFlags(collector: MessageCollector): MutableMap<AnalysisFlag<*>, Any> =
            super.configureAnalysisFlags(collector).also {
                val useExperimental = it[AnalysisFlags.useExperimental] as List<*>
                it[AnalysisFlags.useExperimental] = useExperimental + listOf("kotlin.ExperimentalUnsignedTypes")
                if (printIr)
                    phasesToDumpAfter = arrayOf("ALL")
            }
}

const val EMBED_BITCODE_FLAG = "-Xembed-bitcode"
const val EMBED_BITCODE_MARKER_FLAG = "-Xembed-bitcode-marker"
const val STATIC_FRAMEWORK_FLAG = "-Xstatic-framework"
const val INCLUDE_ARG = "-Xinclude"
const val CACHED_LIBRARY = "-Xcached-library"
const val MAKE_CACHE = "-Xmake-cache"