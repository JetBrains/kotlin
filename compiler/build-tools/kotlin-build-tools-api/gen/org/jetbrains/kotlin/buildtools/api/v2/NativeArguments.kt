package org.jetbrains.kotlin.buildtools.api.v2

import kotlin.Array
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.jvm.JvmField

public interface NativeArguments : CommonKlibBasedArguments {
  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: NativeArgument<V>): V

  public operator fun <V> `set`(key: NativeArgument<V>, `value`: V)

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
    @ExperimentalCompilerArgument
    public val XBUNDLE_ID: NativeArgument<String?> = NativeArgument("XBUNDLE_ID")

    /**
     * Path to the directory containing caches.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XCACHE_DIRECTORY: NativeArgument<Array<String>?> = NativeArgument("XCACHE_DIRECTORY")

    /**
     * Paths to a library and its cache, separated by a comma.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XCACHED_LIBRARY: NativeArgument<Array<String>?> = NativeArgument("XCACHED_LIBRARY")

    /**
     * Path to the root directory from which dependencies are to be cached automatically.
     * By default caches will be placed into the kotlin-native system cache directory.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XAUTO_CACHE_FROM: NativeArgument<Array<String>?> = NativeArgument("XAUTO_CACHE_FROM")

    /**
     * Path to the directory where caches for auto-cacheable dependencies should be put.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XAUTO_CACHE_DIR: NativeArgument<String?> = NativeArgument("XAUTO_CACHE_DIR")

    /**
     * Path to the directory where incremental build caches should be put.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XIC_CACHE_DIR: NativeArgument<String?> = NativeArgument("XIC_CACHE_DIR")

    /**
     * Check dependencies and download the missing ones.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XCHECK_DEPENDENCIES: NativeArgument<Boolean> = NativeArgument("XCHECK_DEPENDENCIES")

    @JvmField
    @ExperimentalCompilerArgument
    public val XEMIT_LAZY_OBJC_HEADER: NativeArgument<String?> =
        NativeArgument("XEMIT_LAZY_OBJC_HEADER")

    /**
     * A library to be included in the produced framework API.
     * This library must be one of the ones passed with '-library'.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XEXPORT_LIBRARY: NativeArgument<Array<String>?> = NativeArgument("XEXPORT_LIBRARY")

    /**
     * Path to the file containing external dependencies.
     * External dependencies are required for verbose output in the event of IR linker errors,
     * but they do not affect compilation at all.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XEXTERNAL_DEPENDENCIES: NativeArgument<String?> =
        NativeArgument("XEXTERNAL_DEPENDENCIES")

    /**
     * Enable the IR fake override validator.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XFAKE_OVERRIDE_VALIDATOR: NativeArgument<Boolean> =
        NativeArgument("XFAKE_OVERRIDE_VALIDATOR")

    /**
     * Add an additional header import to the framework header.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XFRAMEWORK_IMPORT_HEADER: NativeArgument<Array<String>?> =
        NativeArgument("XFRAMEWORK_IMPORT_HEADER")

    /**
     * Add light debug information for optimized builds. This option is skipped in debug builds.
     * It's enabled by default on Darwin platforms where collected debug information is stored in a .dSYM file.
     * Currently this option is disabled by default on other platforms.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XADD_LIGHT_DEBUG: NativeArgument<String?> = NativeArgument("XADD_LIGHT_DEBUG")

    /**
     * Add light debug information. This option has been deprecated. Please use '-Xadd-light-debug=enable' instead.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XG0: NativeArgument<Boolean> = NativeArgument("XG0")

    /**
     * Generate trampolines to make debugger breakpoint resolution more accurate (inlines, 'when', etc.).
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XG_GENERATE_DEBUG_TRAMPOLINE: NativeArgument<String?> =
        NativeArgument("XG_GENERATE_DEBUG_TRAMPOLINE")

    /**
     * Path to a library to be added to the cache.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XADD_CACHE: NativeArgument<String?> = NativeArgument("XADD_CACHE")

    /**
     * Path to the file to cache.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XFILE_TO_CACHE: NativeArgument<Array<String>?> = NativeArgument("XFILE_TO_CACHE")

    /**
     * Force the compiler to produce per-file caches.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XMAKE_PER_FILE_CACHE: NativeArgument<Boolean> =
        NativeArgument("XMAKE_PER_FILE_CACHE")

    /**
     * Run codegen by file in N parallel threads.
     * 0 means use one thread per processor core.
     * The default value is 1.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XBACKEND_THREADS: NativeArgument<Int> = NativeArgument("XBACKEND_THREADS")

    /**
     * Export KDoc entries in the framework header.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XEXPORT_KDOC: NativeArgument<Boolean> = NativeArgument("XEXPORT_KDOC")

    /**
     * Print LLVM bitcode.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XPRINT_BITCODE: NativeArgument<Boolean> = NativeArgument("XPRINT_BITCODE")

    /**
     * Ensure that all calls of possibly long external functions are done in the native thread state.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XCHECK_STATE_AT_EXTERNAL_CALLS: NativeArgument<Boolean> =
        NativeArgument("XCHECK_STATE_AT_EXTERNAL_CALLS")

    /**
     * Print IR.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XPRINT_IR: NativeArgument<Boolean> = NativeArgument("XPRINT_IR")

    /**
     * Print files.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XPRINT_FILES: NativeArgument<Boolean> = NativeArgument("XPRINT_FILES")

    /**
     * Don't link unused libraries even if explicitly specified.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XPURGE_USER_LIBS: NativeArgument<Boolean> = NativeArgument("XPURGE_USER_LIBS")

    /**
     * Write file containing the paths of dependencies used during klib compilation to the provided path
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XWRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO: NativeArgument<String?> =
        NativeArgument("XWRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO")

    /**
     * Override the standard 'runtime.bc' location.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XRUNTIME: NativeArgument<String?> = NativeArgument("XRUNTIME")

    /**
     * A path to an intermediate library that should be processed in the same manner as source files.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XINCLUDE: NativeArgument<Array<String>?> = NativeArgument("XINCLUDE")

    /**
     * A short name used to denote this library in the IDE and in a generated Objective-C header.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XSHORT_MODULE_NAME: NativeArgument<String?> = NativeArgument("XSHORT_MODULE_NAME")

    /**
     * Create a framework with a static library instead of a dynamic one.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XSTATIC_FRAMEWORK: NativeArgument<Boolean> = NativeArgument("XSTATIC_FRAMEWORK")

    /**
     * Save temporary files to the given directory.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XTEMPORARY_FILES_DIR: NativeArgument<String?> =
        NativeArgument("XTEMPORARY_FILES_DIR")

    /**
     * Save the result of the Kotlin IR to LLVM IR translation to '-Xsave-llvm-ir-directory'.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XSAVE_LLVM_IR_AFTER: NativeArgument<Array<String>?> =
        NativeArgument("XSAVE_LLVM_IR_AFTER")

    /**
     * Verify LLVM bitcode after each method.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XVERIFY_BITCODE: NativeArgument<Boolean> = NativeArgument("XVERIFY_BITCODE")

    /**
     * Verify the compiler.
     */
    @JvmField
    @ExperimentalCompilerArgument
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
    @ExperimentalCompilerArgument
    public val XREFINES_PATHS: NativeArgument<Array<String>?> = NativeArgument("XREFINES_PATHS")

    /**
     * Generate debug info of the given version (1, 2).
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XDEBUG_INFO_VERSION: NativeArgument<Int> = NativeArgument("XDEBUG_INFO_VERSION")

    /**
     * Disable generics support for framework header.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XNO_OBJC_GENERICS: NativeArgument<Boolean> = NativeArgument("XNO_OBJC_GENERICS")

    /**
     * Explicit list of Clang options.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XOVERRIDE_CLANG_OPTIONS: NativeArgument<Array<String>?> =
        NativeArgument("XOVERRIDE_CLANG_OPTIONS")

    /**
     * Allocator used at runtime.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XALLOCATOR: NativeArgument<String?> = NativeArgument("XALLOCATOR")

    /**
     * Save a klib that only contains the public ABI to the given path.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XHEADER_KLIB_PATH: NativeArgument<String?> = NativeArgument("XHEADER_KLIB_PATH")

    /**
     * Remap file source directory paths in debug info.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XDEBUG_PREFIX_MAP: NativeArgument<Array<String>?> =
        NativeArgument("XDEBUG_PREFIX_MAP")

    /**
     * Perform caches pre-linking.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XPRE_LINK_CACHES: NativeArgument<String?> = NativeArgument("XPRE_LINK_CACHES")

    /**
     * Override values from 'konan.properties' with the given ones.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XOVERRIDE_KONAN_PROPERTIES: NativeArgument<Array<String>?> =
        NativeArgument("XOVERRIDE_KONAN_PROPERTIES")

    /**
     * When to destroy the runtime – 'legacy' and 'on-shutdown' are currently supported. Note that 'legacy' mode is deprecated and will be removed.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XDESTROY_RUNTIME_MODE: NativeArgument<String?> =
        NativeArgument("XDESTROY_RUNTIME_MODE")

    /**
     * GC to use – 'noop', 'stms', and 'cms' are currently supported. This works only with '-memory-model experimental'.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XGC: NativeArgument<String?> = NativeArgument("XGC")

    /**
     * Initialize top level properties lazily per file.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XIR_PROPERTY_LAZY_INITIALIZATION: NativeArgument<String?> =
        NativeArgument("XIR_PROPERTY_LAZY_INITIALIZATION")

    /**
     * Unhandled exception processing in 'Worker.executeAfter'. Possible values: 'legacy' and 'use-hook'. The default value is 'legacy' and for '-memory-model experimental', the default value is 'use-hook'.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XWORKER_EXCEPTION_HANDLING: NativeArgument<String?> =
        NativeArgument("XWORKER_EXCEPTION_HANDLING")

    /**
     * Choose the LLVM distribution that will be used during compilation.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XLLVM_VARIANT: NativeArgument<String?> = NativeArgument("XLLVM_VARIANT")

    /**
     * Specify a binary option.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XBINARY: NativeArgument<Array<String>?> = NativeArgument("XBINARY")

    /**
     * Enable logging of Native runtime internals.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XRUNTIME_LOGS: NativeArgument<String?> = NativeArgument("XRUNTIME_LOGS")

    /**
     * Path to a file for dumping the list of all available tests.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XDUMP_TESTS_TO: NativeArgument<String?> = NativeArgument("XDUMP_TESTS_TO")

    /**
     * Use lazy IR for cached libraries.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XLAZY_IR_FOR_CACHES: NativeArgument<String?> = NativeArgument("XLAZY_IR_FOR_CACHES")

    /**
     * Omit binary when compiling the framework.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XOMIT_FRAMEWORK_BINARY: NativeArgument<Boolean> =
        NativeArgument("XOMIT_FRAMEWORK_BINARY")

    /**
     * Continue compilation from the given bitcode file.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XCOMPILE_FROM_BITCODE: NativeArgument<String?> =
        NativeArgument("XCOMPILE_FROM_BITCODE")

    /**
     * Serialized dependencies to use for linking.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XREAD_DEPENDENCIES_FROM: NativeArgument<String?> =
        NativeArgument("XREAD_DEPENDENCIES_FROM")

    /**
     * Path for writing backend dependencies.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XWRITE_DEPENDENCIES_TO: NativeArgument<String?> =
        NativeArgument("XWRITE_DEPENDENCIES_TO")

    /**
     * Directory that should contain the results of '-Xsave-llvm-ir-after=<phase>'.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XSAVE_LLVM_IR_DIRECTORY: NativeArgument<String?> =
        NativeArgument("XSAVE_LLVM_IR_DIRECTORY")

    /**
     * Custom path to the location of konan distributions.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XKONAN_DATA_DIR: NativeArgument<String?> = NativeArgument("XKONAN_DATA_DIR")

    /**
     * Custom set of LLVM passes to run as the ModuleOptimizationPipeline.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XLLVM_MODULE_PASSES: NativeArgument<String?> = NativeArgument("XLLVM_MODULE_PASSES")

    /**
     * Custom set of LLVM passes to run as the LTOOptimizationPipeline.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XLLVM_LTO_PASSES: NativeArgument<String?> = NativeArgument("XLLVM_LTO_PASSES")

    /**
     * Comma-separated list that will be written as the value of 'native_targets' property in the .klib manifest. Unknown values are discarded.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XMANIFEST_NATIVE_TARGETS: NativeArgument<Array<String>?> =
        NativeArgument("XMANIFEST_NATIVE_TARGETS")

    /**
     * Path to a directory to dump synthetic accessors and their use sites.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val XDUMP_SYNTHETIC_ACCESSORS_TO: NativeArgument<String?> =
        NativeArgument("XDUMP_SYNTHETIC_ACCESSORS_TO")
  }
}
