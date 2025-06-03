package org.jetbrains.kotlin.build.tools.api

import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.Int
import kotlin.String
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import kotlin.jvm.JvmField

public class NativeArguments : CommonKlibBasedArguments() {
  private val optionsMap: MutableMap<NativeArgument<*>, Any?> = mutableMapOf()

  public operator fun <V> `get`(key: NativeArgument<V>): V? = optionsMap[key] as V?

  public operator fun <V> `set`(key: NativeArgument<V>, `value`: V) {
    optionsMap[key] = `value`
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
    public val BUNDLE_ID: NativeArgument<String?> = NativeArgument("BUNDLE_ID")

    /**
     * Path to the directory containing caches.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val CACHE_DIRECTORY: NativeArgument<Array<String>?> = NativeArgument("CACHE_DIRECTORY")

    /**
     * Paths to a library and its cache, separated by a comma.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val CACHED_LIBRARY: NativeArgument<Array<String>?> = NativeArgument("CACHED_LIBRARY")

    /**
     * Path to the root directory from which dependencies are to be cached automatically.
     * By default caches will be placed into the kotlin-native system cache directory.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val AUTO_CACHE_FROM: NativeArgument<Array<String>?> = NativeArgument("AUTO_CACHE_FROM")

    /**
     * Path to the directory where caches for auto-cacheable dependencies should be put.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val AUTO_CACHE_DIR: NativeArgument<String?> = NativeArgument("AUTO_CACHE_DIR")

    /**
     * Path to the directory where incremental build caches should be put.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val IC_CACHE_DIR: NativeArgument<String?> = NativeArgument("IC_CACHE_DIR")

    /**
     * Check dependencies and download the missing ones.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val CHECK_DEPENDENCIES: NativeArgument<Boolean> = NativeArgument("CHECK_DEPENDENCIES")

    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val EMIT_LAZY_OBJC_HEADER: NativeArgument<String?> =
        NativeArgument("EMIT_LAZY_OBJC_HEADER")

    /**
     * A library to be included in the produced framework API.
     * This library must be one of the ones passed with '-library'.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val EXPORT_LIBRARY: NativeArgument<Array<String>?> = NativeArgument("EXPORT_LIBRARY")

    /**
     * Path to the file containing external dependencies.
     * External dependencies are required for verbose output in the event of IR linker errors,
     * but they do not affect compilation at all.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val EXTERNAL_DEPENDENCIES: NativeArgument<String?> =
        NativeArgument("EXTERNAL_DEPENDENCIES")

    /**
     * Enable the IR fake override validator.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val FAKE_OVERRIDE_VALIDATOR: NativeArgument<Boolean> =
        NativeArgument("FAKE_OVERRIDE_VALIDATOR")

    /**
     * Add an additional header import to the framework header.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val FRAMEWORK_IMPORT_HEADER: NativeArgument<Array<String>?> =
        NativeArgument("FRAMEWORK_IMPORT_HEADER")

    /**
     * Add light debug information for optimized builds. This option is skipped in debug builds.
     * It's enabled by default on Darwin platforms where collected debug information is stored in a .dSYM file.
     * Currently this option is disabled by default on other platforms.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val ADD_LIGHT_DEBUG: NativeArgument<String?> = NativeArgument("ADD_LIGHT_DEBUG")

    /**
     * Add light debug information. This option has been deprecated. Please use '-Xadd-light-debug=enable' instead.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val G0: NativeArgument<Boolean> = NativeArgument("G0")

    /**
     * Generate trampolines to make debugger breakpoint resolution more accurate (inlines, 'when', etc.).
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val G_GENERATE_DEBUG_TRAMPOLINE: NativeArgument<String?> =
        NativeArgument("G_GENERATE_DEBUG_TRAMPOLINE")

    /**
     * Path to a library to be added to the cache.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val ADD_CACHE: NativeArgument<String?> = NativeArgument("ADD_CACHE")

    /**
     * Path to the file to cache.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val FILE_TO_CACHE: NativeArgument<Array<String>?> = NativeArgument("FILE_TO_CACHE")

    /**
     * Force the compiler to produce per-file caches.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val MAKE_PER_FILE_CACHE: NativeArgument<Boolean> = NativeArgument("MAKE_PER_FILE_CACHE")

    /**
     * Run codegen by file in N parallel threads.
     * 0 means use one thread per processor core.
     * The default value is 1.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val BACKEND_THREADS: NativeArgument<Int> = NativeArgument("BACKEND_THREADS")

    /**
     * Export KDoc entries in the framework header.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val EXPORT_KDOC: NativeArgument<Boolean> = NativeArgument("EXPORT_KDOC")

    /**
     * Print LLVM bitcode.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val PRINT_BITCODE: NativeArgument<Boolean> = NativeArgument("PRINT_BITCODE")

    /**
     * Ensure that all calls of possibly long external functions are done in the native thread state.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val CHECK_STATE_AT_EXTERNAL_CALLS: NativeArgument<Boolean> =
        NativeArgument("CHECK_STATE_AT_EXTERNAL_CALLS")

    /**
     * Print IR.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val PRINT_IR: NativeArgument<Boolean> = NativeArgument("PRINT_IR")

    /**
     * Print files.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val PRINT_FILES: NativeArgument<Boolean> = NativeArgument("PRINT_FILES")

    /**
     * Don't link unused libraries even if explicitly specified.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val PURGE_USER_LIBS: NativeArgument<Boolean> = NativeArgument("PURGE_USER_LIBS")

    /**
     * Write file containing the paths of dependencies used during klib compilation to the provided path
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO: NativeArgument<String?> =
        NativeArgument("WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO")

    /**
     * Override the standard 'runtime.bc' location.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val RUNTIME: NativeArgument<String?> = NativeArgument("RUNTIME")

    /**
     * A path to an intermediate library that should be processed in the same manner as source files.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val INCLUDE: NativeArgument<Array<String>?> = NativeArgument("INCLUDE")

    /**
     * A short name used to denote this library in the IDE and in a generated Objective-C header.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val SHORT_MODULE_NAME: NativeArgument<String?> = NativeArgument("SHORT_MODULE_NAME")

    /**
     * Create a framework with a static library instead of a dynamic one.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val STATIC_FRAMEWORK: NativeArgument<Boolean> = NativeArgument("STATIC_FRAMEWORK")

    /**
     * Save temporary files to the given directory.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val TEMPORARY_FILES_DIR: NativeArgument<String?> = NativeArgument("TEMPORARY_FILES_DIR")

    /**
     * Save the result of the Kotlin IR to LLVM IR translation to '-Xsave-llvm-ir-directory'.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val SAVE_LLVM_IR_AFTER: NativeArgument<Array<String>?> =
        NativeArgument("SAVE_LLVM_IR_AFTER")

    /**
     * Verify LLVM bitcode after each method.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val VERIFY_BITCODE: NativeArgument<Boolean> = NativeArgument("VERIFY_BITCODE")

    /**
     * Verify the compiler.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val VERIFY_COMPILER: NativeArgument<String?> = NativeArgument("VERIFY_COMPILER")

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
    public val REFINES_PATHS: NativeArgument<Array<String>?> = NativeArgument("REFINES_PATHS")

    /**
     * Generate debug info of the given version (1, 2).
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val DEBUG_INFO_VERSION: NativeArgument<Int> = NativeArgument("DEBUG_INFO_VERSION")

    /**
     * Disable generics support for framework header.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val NO_OBJC_GENERICS: NativeArgument<Boolean> = NativeArgument("NO_OBJC_GENERICS")

    /**
     * Explicit list of Clang options.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val OVERRIDE_CLANG_OPTIONS: NativeArgument<Array<String>?> =
        NativeArgument("OVERRIDE_CLANG_OPTIONS")

    /**
     * Allocator used at runtime.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val ALLOCATOR: NativeArgument<String?> = NativeArgument("ALLOCATOR")

    /**
     * Save a klib that only contains the public ABI to the given path.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val HEADER_KLIB_PATH: NativeArgument<String?> = NativeArgument("HEADER_KLIB_PATH")

    /**
     * Remap file source directory paths in debug info.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val DEBUG_PREFIX_MAP: NativeArgument<Array<String>?> = NativeArgument("DEBUG_PREFIX_MAP")

    /**
     * Perform caches pre-linking.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val PRE_LINK_CACHES: NativeArgument<String?> = NativeArgument("PRE_LINK_CACHES")

    /**
     * Override values from 'konan.properties' with the given ones.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val OVERRIDE_KONAN_PROPERTIES: NativeArgument<Array<String>?> =
        NativeArgument("OVERRIDE_KONAN_PROPERTIES")

    /**
     * When to destroy the runtime – 'legacy' and 'on-shutdown' are currently supported. Note that 'legacy' mode is deprecated and will be removed.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val DESTROY_RUNTIME_MODE: NativeArgument<String?> =
        NativeArgument("DESTROY_RUNTIME_MODE")

    /**
     * GC to use – 'noop', 'stms', and 'cms' are currently supported. This works only with '-memory-model experimental'.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val GC: NativeArgument<String?> = NativeArgument("GC")

    /**
     * Initialize top level properties lazily per file.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val IR_PROPERTY_LAZY_INITIALIZATION: NativeArgument<String?> =
        NativeArgument("IR_PROPERTY_LAZY_INITIALIZATION")

    /**
     * Unhandled exception processing in 'Worker.executeAfter'. Possible values: 'legacy' and 'use-hook'. The default value is 'legacy' and for '-memory-model experimental', the default value is 'use-hook'.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val WORKER_EXCEPTION_HANDLING: NativeArgument<String?> =
        NativeArgument("WORKER_EXCEPTION_HANDLING")

    /**
     * Choose the LLVM distribution that will be used during compilation.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val LLVM_VARIANT: NativeArgument<String?> = NativeArgument("LLVM_VARIANT")

    /**
     * Specify a binary option.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val BINARY: NativeArgument<Array<String>?> = NativeArgument("BINARY")

    /**
     * Enable logging of Native runtime internals.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val RUNTIME_LOGS: NativeArgument<String?> = NativeArgument("RUNTIME_LOGS")

    /**
     * Path to a file for dumping the list of all available tests.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val DUMP_TESTS_TO: NativeArgument<String?> = NativeArgument("DUMP_TESTS_TO")

    /**
     * Use lazy IR for cached libraries.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val LAZY_IR_FOR_CACHES: NativeArgument<String?> = NativeArgument("LAZY_IR_FOR_CACHES")

    /**
     * Omit binary when compiling the framework.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val OMIT_FRAMEWORK_BINARY: NativeArgument<Boolean> =
        NativeArgument("OMIT_FRAMEWORK_BINARY")

    /**
     * Continue compilation from the given bitcode file.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val COMPILE_FROM_BITCODE: NativeArgument<String?> =
        NativeArgument("COMPILE_FROM_BITCODE")

    /**
     * Serialized dependencies to use for linking.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val READ_DEPENDENCIES_FROM: NativeArgument<String?> =
        NativeArgument("READ_DEPENDENCIES_FROM")

    /**
     * Path for writing backend dependencies.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val WRITE_DEPENDENCIES_TO: NativeArgument<String?> =
        NativeArgument("WRITE_DEPENDENCIES_TO")

    /**
     * Directory that should contain the results of '-Xsave-llvm-ir-after=<phase>'.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val SAVE_LLVM_IR_DIRECTORY: NativeArgument<String?> =
        NativeArgument("SAVE_LLVM_IR_DIRECTORY")

    /**
     * Custom path to the location of konan distributions.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val KONAN_DATA_DIR: NativeArgument<String?> = NativeArgument("KONAN_DATA_DIR")

    /**
     * Custom set of LLVM passes to run as the ModuleOptimizationPipeline.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val LLVM_MODULE_PASSES: NativeArgument<String?> = NativeArgument("LLVM_MODULE_PASSES")

    /**
     * Custom set of LLVM passes to run as the LTOOptimizationPipeline.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val LLVM_LTO_PASSES: NativeArgument<String?> = NativeArgument("LLVM_LTO_PASSES")

    /**
     * Comma-separated list that will be written as the value of 'native_targets' property in the .klib manifest. Unknown values are discarded.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val MANIFEST_NATIVE_TARGETS: NativeArgument<Array<String>?> =
        NativeArgument("MANIFEST_NATIVE_TARGETS")

    /**
     * Path to a directory to dump synthetic accessors and their use sites.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val DUMP_SYNTHETIC_ACCESSORS_TO: NativeArgument<String?> =
        NativeArgument("DUMP_SYNTHETIC_ACCESSORS_TO")
  }
}
