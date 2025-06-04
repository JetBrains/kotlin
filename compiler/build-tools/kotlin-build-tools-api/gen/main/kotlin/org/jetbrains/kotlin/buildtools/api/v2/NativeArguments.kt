package org.jetbrains.kotlin.buildtools.api.v2

import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments

public class NativeArguments : CommonKlibBasedArguments() {
  private val optionsMap: MutableMap<NativeArgument<*>, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: NativeArgument<V>): V = optionsMap[key] as V

  public operator fun <V> `set`(key: NativeArgument<V>, `value`: V) {
    optionsMap[key] = `value`
  }

  public fun toCompilerArguments(): K2NativeCompilerArguments {
    val arguments = K2NativeCompilerArguments()
    if (ENABLE_ASSERTIONS in optionsMap) { arguments.enableAssertions = get(ENABLE_ASSERTIONS) }
    if (G in optionsMap) { arguments.debug = get(G) }
    if (GENERATE_TEST_RUNNER in optionsMap) { arguments.generateTestRunner = get(GENERATE_TEST_RUNNER) }
    if (GENERATE_WORKER_TEST_RUNNER in optionsMap) { arguments.generateWorkerTestRunner = get(GENERATE_WORKER_TEST_RUNNER) }
    if (GENERATE_NO_EXIT_TEST_RUNNER in optionsMap) { arguments.generateNoExitTestRunner = get(GENERATE_NO_EXIT_TEST_RUNNER) }
    if (INCLUDE_BINARY in optionsMap) { arguments.includeBinaries = get(INCLUDE_BINARY) }
    if (LIBRARY in optionsMap) { arguments.libraries = get(LIBRARY) }
    if (LIBRARY_VERSION in optionsMap) { arguments.libraryVersion = get(LIBRARY_VERSION) }
    if (LIST_TARGETS in optionsMap) { arguments.listTargets = get(LIST_TARGETS) }
    if (MANIFEST in optionsMap) { arguments.manifestFile = get(MANIFEST) }
    if (MEMORY_MODEL in optionsMap) { arguments.memoryModel = get(MEMORY_MODEL) }
    if (MODULE_NAME in optionsMap) { arguments.moduleName = get(MODULE_NAME) }
    if (NATIVE_LIBRARY in optionsMap) { arguments.nativeLibraries = get(NATIVE_LIBRARY) }
    if (NO_DEFAULT_LIBS in optionsMap) { arguments.nodefaultlibs = get(NO_DEFAULT_LIBS) }
    if (NO_ENDORSED_LIBS in optionsMap) { arguments.noendorsedlibs = get(NO_ENDORSED_LIBS) }
    if (NOMAIN in optionsMap) { arguments.nomain = get(NOMAIN) }
    if (NOPACK in optionsMap) { arguments.nopack = get(NOPACK) }
    if (LINKER_OPTIONS in optionsMap) { arguments.linkerArguments = get(LINKER_OPTIONS) }
    if (LINKER_OPTION in optionsMap) { arguments.singleLinkerArguments = get(LINKER_OPTION) }
    if (NOSTDLIB in optionsMap) { arguments.nostdlib = get(NOSTDLIB) }
    if (OPT in optionsMap) { arguments.optimization = get(OPT) }
    if (OUTPUT in optionsMap) { arguments.outputName = get(OUTPUT) }
    if (ENTRY in optionsMap) { arguments.mainPackage = get(ENTRY) }
    if (PRODUCE in optionsMap) { arguments.produce = get(PRODUCE) }
    if (TARGET in optionsMap) { arguments.target = get(TARGET) }
    if (XBUNDLE_ID in optionsMap) { arguments.bundleId = get(XBUNDLE_ID) }
    if (XCACHE_DIRECTORY in optionsMap) { arguments.cacheDirectories = get(XCACHE_DIRECTORY) }
    if (XCACHED_LIBRARY in optionsMap) { arguments.cachedLibraries = get(XCACHED_LIBRARY) }
    if (XAUTO_CACHE_FROM in optionsMap) { arguments.autoCacheableFrom = get(XAUTO_CACHE_FROM) }
    if (XAUTO_CACHE_DIR in optionsMap) { arguments.autoCacheDir = get(XAUTO_CACHE_DIR) }
    if (XIC_CACHE_DIR in optionsMap) { arguments.incrementalCacheDir = get(XIC_CACHE_DIR) }
    if (XCHECK_DEPENDENCIES in optionsMap) { arguments.checkDependencies = get(XCHECK_DEPENDENCIES) }
    if (XEMIT_LAZY_OBJC_HEADER in optionsMap) { arguments.emitLazyObjCHeader = get(XEMIT_LAZY_OBJC_HEADER) }
    if (XEXPORT_LIBRARY in optionsMap) { arguments.exportedLibraries = get(XEXPORT_LIBRARY) }
    if (XEXTERNAL_DEPENDENCIES in optionsMap) { arguments.externalDependencies = get(XEXTERNAL_DEPENDENCIES) }
    if (XFAKE_OVERRIDE_VALIDATOR in optionsMap) { arguments.fakeOverrideValidator = get(XFAKE_OVERRIDE_VALIDATOR) }
    if (XFRAMEWORK_IMPORT_HEADER in optionsMap) { arguments.frameworkImportHeaders = get(XFRAMEWORK_IMPORT_HEADER) }
    if (XADD_LIGHT_DEBUG in optionsMap) { arguments.lightDebugString = get(XADD_LIGHT_DEBUG) }
    if (XG0 in optionsMap) { arguments.lightDebugDeprecated = get(XG0) }
    if (XG_GENERATE_DEBUG_TRAMPOLINE in optionsMap) { arguments.generateDebugTrampolineString = get(XG_GENERATE_DEBUG_TRAMPOLINE) }
    if (XADD_CACHE in optionsMap) { arguments.libraryToAddToCache = get(XADD_CACHE) }
    if (XFILE_TO_CACHE in optionsMap) { arguments.filesToCache = get(XFILE_TO_CACHE) }
    if (XMAKE_PER_FILE_CACHE in optionsMap) { arguments.makePerFileCache = get(XMAKE_PER_FILE_CACHE) }
    if (XBACKEND_THREADS in optionsMap) { arguments.backendThreads = get(XBACKEND_THREADS).toString() }
    if (XEXPORT_KDOC in optionsMap) { arguments.exportKDoc = get(XEXPORT_KDOC) }
    if (XPRINT_BITCODE in optionsMap) { arguments.printBitCode = get(XPRINT_BITCODE) }
    if (XCHECK_STATE_AT_EXTERNAL_CALLS in optionsMap) { arguments.checkExternalCalls = get(XCHECK_STATE_AT_EXTERNAL_CALLS) }
    if (XPRINT_IR in optionsMap) { arguments.printIr = get(XPRINT_IR) }
    if (XPRINT_FILES in optionsMap) { arguments.printFiles = get(XPRINT_FILES) }
    if (XPURGE_USER_LIBS in optionsMap) { arguments.purgeUserLibs = get(XPURGE_USER_LIBS) }
    if (XWRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO in optionsMap) { arguments.writeDependenciesOfProducedKlibTo = get(XWRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO) }
    if (XRUNTIME in optionsMap) { arguments.runtimeFile = get(XRUNTIME) }
    if (XINCLUDE in optionsMap) { arguments.includes = get(XINCLUDE) }
    if (XSHORT_MODULE_NAME in optionsMap) { arguments.shortModuleName = get(XSHORT_MODULE_NAME) }
    if (XSTATIC_FRAMEWORK in optionsMap) { arguments.staticFramework = get(XSTATIC_FRAMEWORK) }
    if (XTEMPORARY_FILES_DIR in optionsMap) { arguments.temporaryFilesDir = get(XTEMPORARY_FILES_DIR) }
    if (XSAVE_LLVM_IR_AFTER in optionsMap) { arguments.saveLlvmIrAfter = get(XSAVE_LLVM_IR_AFTER) }
    if (XVERIFY_BITCODE in optionsMap) { arguments.verifyBitCode = get(XVERIFY_BITCODE) }
    if (XVERIFY_COMPILER in optionsMap) { arguments.verifyCompiler = get(XVERIFY_COMPILER) }
    if (FRIEND_MODULES in optionsMap) { arguments.friendModules = get(FRIEND_MODULES) }
    if (XREFINES_PATHS in optionsMap) { arguments.refinesPaths = get(XREFINES_PATHS) }
    if (XDEBUG_INFO_VERSION in optionsMap) { arguments.debugInfoFormatVersion = get(XDEBUG_INFO_VERSION).toString() }
    if (XNO_OBJC_GENERICS in optionsMap) { arguments.noObjcGenerics = get(XNO_OBJC_GENERICS) }
    if (XOVERRIDE_CLANG_OPTIONS in optionsMap) { arguments.clangOptions = get(XOVERRIDE_CLANG_OPTIONS) }
    if (XALLOCATOR in optionsMap) { arguments.allocator = get(XALLOCATOR) }
    if (XHEADER_KLIB_PATH in optionsMap) { arguments.headerKlibPath = get(XHEADER_KLIB_PATH) }
    if (XDEBUG_PREFIX_MAP in optionsMap) { arguments.debugPrefixMap = get(XDEBUG_PREFIX_MAP) }
    if (XPRE_LINK_CACHES in optionsMap) { arguments.preLinkCaches = get(XPRE_LINK_CACHES) }
    if (XOVERRIDE_KONAN_PROPERTIES in optionsMap) { arguments.overrideKonanProperties = get(XOVERRIDE_KONAN_PROPERTIES) }
    if (XDESTROY_RUNTIME_MODE in optionsMap) { arguments.destroyRuntimeMode = get(XDESTROY_RUNTIME_MODE) }
    if (XGC in optionsMap) { arguments.gc = get(XGC) }
    if (XIR_PROPERTY_LAZY_INITIALIZATION in optionsMap) { arguments.propertyLazyInitialization = get(XIR_PROPERTY_LAZY_INITIALIZATION) }
    if (XWORKER_EXCEPTION_HANDLING in optionsMap) { arguments.workerExceptionHandling = get(XWORKER_EXCEPTION_HANDLING) }
    if (XLLVM_VARIANT in optionsMap) { arguments.llvmVariant = get(XLLVM_VARIANT) }
    if (XBINARY in optionsMap) { arguments.binaryOptions = get(XBINARY) }
    if (XRUNTIME_LOGS in optionsMap) { arguments.runtimeLogs = get(XRUNTIME_LOGS) }
    if (XDUMP_TESTS_TO in optionsMap) { arguments.testDumpOutputPath = get(XDUMP_TESTS_TO) }
    if (XLAZY_IR_FOR_CACHES in optionsMap) { arguments.lazyIrForCaches = get(XLAZY_IR_FOR_CACHES) }
    if (XOMIT_FRAMEWORK_BINARY in optionsMap) { arguments.omitFrameworkBinary = get(XOMIT_FRAMEWORK_BINARY) }
    if (XCOMPILE_FROM_BITCODE in optionsMap) { arguments.compileFromBitcode = get(XCOMPILE_FROM_BITCODE) }
    if (XREAD_DEPENDENCIES_FROM in optionsMap) { arguments.serializedDependencies = get(XREAD_DEPENDENCIES_FROM) }
    if (XWRITE_DEPENDENCIES_TO in optionsMap) { arguments.saveDependenciesPath = get(XWRITE_DEPENDENCIES_TO) }
    if (XSAVE_LLVM_IR_DIRECTORY in optionsMap) { arguments.saveLlvmIrDirectory = get(XSAVE_LLVM_IR_DIRECTORY) }
    if (XKONAN_DATA_DIR in optionsMap) { arguments.konanDataDir = get(XKONAN_DATA_DIR) }
    if (XLLVM_MODULE_PASSES in optionsMap) { arguments.llvmModulePasses = get(XLLVM_MODULE_PASSES) }
    if (XLLVM_LTO_PASSES in optionsMap) { arguments.llvmLTOPasses = get(XLLVM_LTO_PASSES) }
    if (XMANIFEST_NATIVE_TARGETS in optionsMap) { arguments.manifestNativeTargets = get(XMANIFEST_NATIVE_TARGETS) }
    if (XDUMP_SYNTHETIC_ACCESSORS_TO in optionsMap) { arguments.dumpSyntheticAccessorsTo = get(XDUMP_SYNTHETIC_ACCESSORS_TO) }
    return arguments
  }

  public class NativeArgument<V>(
    public val id: String,
  )

  public companion object {
    /**
     * Enable runtime assertions in generated code.
     */
    @JvmField
    public val ENABLE_ASSERTIONS: NativeArgument<Boolean> = NativeArgument("ENABLE_ASSERTIONS")

    /**
     * Enable the emission of debug information.
     */
    @JvmField
    public val G: NativeArgument<Boolean> = NativeArgument("G")

    /**
     * Produce a runner for unit tests.
     */
    @JvmField
    public val GENERATE_TEST_RUNNER: NativeArgument<Boolean> =
        NativeArgument("GENERATE_TEST_RUNNER")

    /**
     * Produce a worker runner for unit tests.
     */
    @JvmField
    public val GENERATE_WORKER_TEST_RUNNER: NativeArgument<Boolean> =
        NativeArgument("GENERATE_WORKER_TEST_RUNNER")

    /**
     * Produce a runner for unit tests that doesn't force an exit.
     */
    @JvmField
    public val GENERATE_NO_EXIT_TEST_RUNNER: NativeArgument<Boolean> =
        NativeArgument("GENERATE_NO_EXIT_TEST_RUNNER")

    /**
     * Pack the given external binary into the klib.
     */
    @JvmField
    public val INCLUDE_BINARY: NativeArgument<Array<String>?> = NativeArgument("INCLUDE_BINARY")

    /**
     * Link with the given library.
     */
    @JvmField
    public val LIBRARY: NativeArgument<Array<String>?> = NativeArgument("LIBRARY")

    /**
     * The library version.
     * Note: This option is deprecated and will be removed in one of the future releases.
     */
    @JvmField
    public val LIBRARY_VERSION: NativeArgument<String?> = NativeArgument("LIBRARY_VERSION")

    /**
     * List available hardware targets.
     */
    @JvmField
    public val LIST_TARGETS: NativeArgument<Boolean> = NativeArgument("LIST_TARGETS")

    /**
     * Provide a manifest addend file.
     */
    @JvmField
    public val MANIFEST: NativeArgument<String?> = NativeArgument("MANIFEST")

    /**
     * Choose the memory model to be used – 'strict' and 'experimental' are currently supported.
     */
    @JvmField
    public val MEMORY_MODEL: NativeArgument<String?> = NativeArgument("MEMORY_MODEL")

    /**
     * Specify a name for the compilation module.
     */
    @JvmField
    public val MODULE_NAME: NativeArgument<String?> = NativeArgument("MODULE_NAME")

    /**
     * Include the given native bitcode library.
     */
    @JvmField
    public val NATIVE_LIBRARY: NativeArgument<Array<String>?> = NativeArgument("NATIVE_LIBRARY")

    /**
     * Don't link the libraries from dist/klib automatically.
     */
    @JvmField
    public val NO_DEFAULT_LIBS: NativeArgument<Boolean> = NativeArgument("NO_DEFAULT_LIBS")

    /**
     * Don't link endorsed libraries from the dist automatically. This option has been deprecated, as the dist no longer has any endorsed libraries.
     */
    @JvmField
    public val NO_ENDORSED_LIBS: NativeArgument<Boolean> = NativeArgument("NO_ENDORSED_LIBS")

    /**
     * Assume the 'main' entry point will be provided by external libraries.
     */
    @JvmField
    public val NOMAIN: NativeArgument<Boolean> = NativeArgument("NOMAIN")

    /**
     * Don't pack the library into a klib file.
     */
    @JvmField
    public val NOPACK: NativeArgument<Boolean> = NativeArgument("NOPACK")

    /**
     * Pass arguments to the linker.
     */
    @JvmField
    public val LINKER_OPTIONS: NativeArgument<Array<String>?> = NativeArgument("LINKER_OPTIONS")

    /**
     * Pass the given argument to the linker.
     */
    @JvmField
    public val LINKER_OPTION: NativeArgument<Array<String>?> = NativeArgument("LINKER_OPTION")

    /**
     * Don't link with the stdlib.
     */
    @JvmField
    public val NOSTDLIB: NativeArgument<Boolean> = NativeArgument("NOSTDLIB")

    /**
     * Enable optimizations during compilation.
     */
    @JvmField
    public val OPT: NativeArgument<Boolean> = NativeArgument("OPT")

    /**
     * Output name.
     */
    @JvmField
    public val OUTPUT: NativeArgument<String?> = NativeArgument("OUTPUT")

    /**
     * Qualified entry point name.
     */
    @JvmField
    public val ENTRY: NativeArgument<String?> = NativeArgument("ENTRY")

    /**
     * Specify the output file kind.
     */
    @JvmField
    public val PRODUCE: NativeArgument<String?> = NativeArgument("PRODUCE")

    /**
     * Set the hardware target.
     */
    @JvmField
    public val TARGET: NativeArgument<String?> = NativeArgument("TARGET")

    /**
     * Bundle ID to be set in the Info.plist file of the produced framework. This option is deprecated. Please use '-Xbinary=bundleId=<id>'.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XBUNDLE_ID: NativeArgument<String?> = NativeArgument("XBUNDLE_ID")

    /**
     * Path to the directory containing caches.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XCACHE_DIRECTORY: NativeArgument<Array<String>?> = NativeArgument("XCACHE_DIRECTORY")

    /**
     * Paths to a library and its cache, separated by a comma.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XCACHED_LIBRARY: NativeArgument<Array<String>?> = NativeArgument("XCACHED_LIBRARY")

    /**
     * Path to the root directory from which dependencies are to be cached automatically.
     * By default caches will be placed into the kotlin-native system cache directory.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XAUTO_CACHE_FROM: NativeArgument<Array<String>?> = NativeArgument("XAUTO_CACHE_FROM")

    /**
     * Path to the directory where caches for auto-cacheable dependencies should be put.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XAUTO_CACHE_DIR: NativeArgument<String?> = NativeArgument("XAUTO_CACHE_DIR")

    /**
     * Path to the directory where incremental build caches should be put.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIC_CACHE_DIR: NativeArgument<String?> = NativeArgument("XIC_CACHE_DIR")

    /**
     * Check dependencies and download the missing ones.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XCHECK_DEPENDENCIES: NativeArgument<Boolean> = NativeArgument("XCHECK_DEPENDENCIES")

    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XEMIT_LAZY_OBJC_HEADER: NativeArgument<String?> =
        NativeArgument("XEMIT_LAZY_OBJC_HEADER")

    /**
     * A library to be included in the produced framework API.
     * This library must be one of the ones passed with '-library'.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XEXPORT_LIBRARY: NativeArgument<Array<String>?> = NativeArgument("XEXPORT_LIBRARY")

    /**
     * Path to the file containing external dependencies.
     * External dependencies are required for verbose output in the event of IR linker errors,
     * but they do not affect compilation at all.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XEXTERNAL_DEPENDENCIES: NativeArgument<String?> =
        NativeArgument("XEXTERNAL_DEPENDENCIES")

    /**
     * Enable the IR fake override validator.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XFAKE_OVERRIDE_VALIDATOR: NativeArgument<Boolean> =
        NativeArgument("XFAKE_OVERRIDE_VALIDATOR")

    /**
     * Add an additional header import to the framework header.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XFRAMEWORK_IMPORT_HEADER: NativeArgument<Array<String>?> =
        NativeArgument("XFRAMEWORK_IMPORT_HEADER")

    /**
     * Add light debug information for optimized builds. This option is skipped in debug builds.
     * It's enabled by default on Darwin platforms where collected debug information is stored in a .dSYM file.
     * Currently this option is disabled by default on other platforms.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XADD_LIGHT_DEBUG: NativeArgument<String?> = NativeArgument("XADD_LIGHT_DEBUG")

    /**
     * Add light debug information. This option has been deprecated. Please use '-Xadd-light-debug=enable' instead.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XG0: NativeArgument<Boolean> = NativeArgument("XG0")

    /**
     * Generate trampolines to make debugger breakpoint resolution more accurate (inlines, 'when', etc.).
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XG_GENERATE_DEBUG_TRAMPOLINE: NativeArgument<String?> =
        NativeArgument("XG_GENERATE_DEBUG_TRAMPOLINE")

    /**
     * Path to a library to be added to the cache.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XADD_CACHE: NativeArgument<String?> = NativeArgument("XADD_CACHE")

    /**
     * Path to the file to cache.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XFILE_TO_CACHE: NativeArgument<Array<String>?> = NativeArgument("XFILE_TO_CACHE")

    /**
     * Force the compiler to produce per-file caches.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XMAKE_PER_FILE_CACHE: NativeArgument<Boolean> =
        NativeArgument("XMAKE_PER_FILE_CACHE")

    /**
     * Run codegen by file in N parallel threads.
     * 0 means use one thread per processor core.
     * The default value is 1.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XBACKEND_THREADS: NativeArgument<Int> = NativeArgument("XBACKEND_THREADS")

    /**
     * Export KDoc entries in the framework header.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XEXPORT_KDOC: NativeArgument<Boolean> = NativeArgument("XEXPORT_KDOC")

    /**
     * Print LLVM bitcode.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XPRINT_BITCODE: NativeArgument<Boolean> = NativeArgument("XPRINT_BITCODE")

    /**
     * Ensure that all calls of possibly long external functions are done in the native thread state.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XCHECK_STATE_AT_EXTERNAL_CALLS: NativeArgument<Boolean> =
        NativeArgument("XCHECK_STATE_AT_EXTERNAL_CALLS")

    /**
     * Print IR.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XPRINT_IR: NativeArgument<Boolean> = NativeArgument("XPRINT_IR")

    /**
     * Print files.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XPRINT_FILES: NativeArgument<Boolean> = NativeArgument("XPRINT_FILES")

    /**
     * Don't link unused libraries even if explicitly specified.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XPURGE_USER_LIBS: NativeArgument<Boolean> = NativeArgument("XPURGE_USER_LIBS")

    /**
     * Write file containing the paths of dependencies used during klib compilation to the provided path
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XWRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO: NativeArgument<String?> =
        NativeArgument("XWRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO")

    /**
     * Override the standard 'runtime.bc' location.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XRUNTIME: NativeArgument<String?> = NativeArgument("XRUNTIME")

    /**
     * A path to an intermediate library that should be processed in the same manner as source files.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XINCLUDE: NativeArgument<Array<String>?> = NativeArgument("XINCLUDE")

    /**
     * A short name used to denote this library in the IDE and in a generated Objective-C header.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XSHORT_MODULE_NAME: NativeArgument<String?> = NativeArgument("XSHORT_MODULE_NAME")

    /**
     * Create a framework with a static library instead of a dynamic one.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XSTATIC_FRAMEWORK: NativeArgument<Boolean> = NativeArgument("XSTATIC_FRAMEWORK")

    /**
     * Save temporary files to the given directory.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XTEMPORARY_FILES_DIR: NativeArgument<String?> =
        NativeArgument("XTEMPORARY_FILES_DIR")

    /**
     * Save the result of the Kotlin IR to LLVM IR translation to '-Xsave-llvm-ir-directory'.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XSAVE_LLVM_IR_AFTER: NativeArgument<Array<String>?> =
        NativeArgument("XSAVE_LLVM_IR_AFTER")

    /**
     * Verify LLVM bitcode after each method.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XVERIFY_BITCODE: NativeArgument<Boolean> = NativeArgument("XVERIFY_BITCODE")

    /**
     * Verify the compiler.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XVERIFY_COMPILER: NativeArgument<String?> = NativeArgument("XVERIFY_COMPILER")

    /**
     * Paths to friend modules.
     */
    @JvmField
    public val FRIEND_MODULES: NativeArgument<String?> = NativeArgument("FRIEND_MODULES")

    /**
     * Paths to output directories for refined modules (modules whose 'expect' declarations this module can actualize).
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XREFINES_PATHS: NativeArgument<Array<String>?> = NativeArgument("XREFINES_PATHS")

    /**
     * Generate debug info of the given version (1, 2).
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XDEBUG_INFO_VERSION: NativeArgument<Int> = NativeArgument("XDEBUG_INFO_VERSION")

    /**
     * Disable generics support for framework header.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XNO_OBJC_GENERICS: NativeArgument<Boolean> = NativeArgument("XNO_OBJC_GENERICS")

    /**
     * Explicit list of Clang options.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XOVERRIDE_CLANG_OPTIONS: NativeArgument<Array<String>?> =
        NativeArgument("XOVERRIDE_CLANG_OPTIONS")

    /**
     * Allocator used at runtime.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XALLOCATOR: NativeArgument<String?> = NativeArgument("XALLOCATOR")

    /**
     * Save a klib that only contains the public ABI to the given path.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XHEADER_KLIB_PATH: NativeArgument<String?> = NativeArgument("XHEADER_KLIB_PATH")

    /**
     * Remap file source directory paths in debug info.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XDEBUG_PREFIX_MAP: NativeArgument<Array<String>?> =
        NativeArgument("XDEBUG_PREFIX_MAP")

    /**
     * Perform caches pre-linking.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XPRE_LINK_CACHES: NativeArgument<String?> = NativeArgument("XPRE_LINK_CACHES")

    /**
     * Override values from 'konan.properties' with the given ones.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XOVERRIDE_KONAN_PROPERTIES: NativeArgument<Array<String>?> =
        NativeArgument("XOVERRIDE_KONAN_PROPERTIES")

    /**
     * When to destroy the runtime – 'legacy' and 'on-shutdown' are currently supported. Note that 'legacy' mode is deprecated and will be removed.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XDESTROY_RUNTIME_MODE: NativeArgument<String?> =
        NativeArgument("XDESTROY_RUNTIME_MODE")

    /**
     * GC to use – 'noop', 'stms', and 'cms' are currently supported. This works only with '-memory-model experimental'.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XGC: NativeArgument<String?> = NativeArgument("XGC")

    /**
     * Initialize top level properties lazily per file.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_PROPERTY_LAZY_INITIALIZATION: NativeArgument<String?> =
        NativeArgument("XIR_PROPERTY_LAZY_INITIALIZATION")

    /**
     * Unhandled exception processing in 'Worker.executeAfter'. Possible values: 'legacy' and 'use-hook'. The default value is 'legacy' and for '-memory-model experimental', the default value is 'use-hook'.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XWORKER_EXCEPTION_HANDLING: NativeArgument<String?> =
        NativeArgument("XWORKER_EXCEPTION_HANDLING")

    /**
     * Choose the LLVM distribution that will be used during compilation.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XLLVM_VARIANT: NativeArgument<String?> = NativeArgument("XLLVM_VARIANT")

    /**
     * Specify a binary option.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XBINARY: NativeArgument<Array<String>?> = NativeArgument("XBINARY")

    /**
     * Enable logging of Native runtime internals.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XRUNTIME_LOGS: NativeArgument<String?> = NativeArgument("XRUNTIME_LOGS")

    /**
     * Path to a file for dumping the list of all available tests.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XDUMP_TESTS_TO: NativeArgument<String?> = NativeArgument("XDUMP_TESTS_TO")

    /**
     * Use lazy IR for cached libraries.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XLAZY_IR_FOR_CACHES: NativeArgument<String?> = NativeArgument("XLAZY_IR_FOR_CACHES")

    /**
     * Omit binary when compiling the framework.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XOMIT_FRAMEWORK_BINARY: NativeArgument<Boolean> =
        NativeArgument("XOMIT_FRAMEWORK_BINARY")

    /**
     * Continue compilation from the given bitcode file.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XCOMPILE_FROM_BITCODE: NativeArgument<String?> =
        NativeArgument("XCOMPILE_FROM_BITCODE")

    /**
     * Serialized dependencies to use for linking.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XREAD_DEPENDENCIES_FROM: NativeArgument<String?> =
        NativeArgument("XREAD_DEPENDENCIES_FROM")

    /**
     * Path for writing backend dependencies.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XWRITE_DEPENDENCIES_TO: NativeArgument<String?> =
        NativeArgument("XWRITE_DEPENDENCIES_TO")

    /**
     * Directory that should contain the results of '-Xsave-llvm-ir-after=<phase>'.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XSAVE_LLVM_IR_DIRECTORY: NativeArgument<String?> =
        NativeArgument("XSAVE_LLVM_IR_DIRECTORY")

    /**
     * Custom path to the location of konan distributions.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XKONAN_DATA_DIR: NativeArgument<String?> = NativeArgument("XKONAN_DATA_DIR")

    /**
     * Custom set of LLVM passes to run as the ModuleOptimizationPipeline.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XLLVM_MODULE_PASSES: NativeArgument<String?> = NativeArgument("XLLVM_MODULE_PASSES")

    /**
     * Custom set of LLVM passes to run as the LTOOptimizationPipeline.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XLLVM_LTO_PASSES: NativeArgument<String?> = NativeArgument("XLLVM_LTO_PASSES")

    /**
     * Comma-separated list that will be written as the value of 'native_targets' property in the .klib manifest. Unknown values are discarded.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XMANIFEST_NATIVE_TARGETS: NativeArgument<Array<String>?> =
        NativeArgument("XMANIFEST_NATIVE_TARGETS")

    /**
     * Path to a directory to dump synthetic accessors and their use sites.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XDUMP_SYNTHETIC_ACCESSORS_TO: NativeArgument<String?> =
        NativeArgument("XDUMP_SYNTHETIC_ACCESSORS_TO")
  }
}
