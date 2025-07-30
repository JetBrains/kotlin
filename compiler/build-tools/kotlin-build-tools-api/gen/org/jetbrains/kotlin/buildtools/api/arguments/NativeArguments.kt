// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Array
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.jvm.JvmField

/**
 * @since 2.3.0
 */
@ExperimentalCompilerArgument
public interface NativeArguments : CommonKlibBasedArguments {
  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: NativeArgument<V>): V

  /**
   * Set the [value] for option specified by [key], overriding any previous value for that option.
   */
  public operator fun <V> `set`(key: NativeArgument<V>, `value`: V)

  public operator fun contains(key: NativeArgument<*>): Boolean

  /**
   * Base class for [NativeArguments] options.
   *
   * @see get
   * @see set    
   */
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
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_BUNDLE_ID: NativeArgument<String?> = NativeArgument("X_BUNDLE_ID")

    /**
     * Path to the directory containing caches.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_CACHE_DIRECTORY: NativeArgument<Array<String>?> =
        NativeArgument("X_CACHE_DIRECTORY")

    /**
     * Paths to a library and its cache, separated by a comma.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_CACHED_LIBRARY: NativeArgument<Array<String>?> = NativeArgument("X_CACHED_LIBRARY")

    /**
     * Path to the root directory from which dependencies are to be cached automatically.
     * By default caches will be placed into the kotlin-native system cache directory.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_AUTO_CACHE_FROM: NativeArgument<Array<String>?> =
        NativeArgument("X_AUTO_CACHE_FROM")

    /**
     * Path to the directory where caches for auto-cacheable dependencies should be put.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_AUTO_CACHE_DIR: NativeArgument<String?> = NativeArgument("X_AUTO_CACHE_DIR")

    /**
     * Path to the directory where incremental build caches should be put.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IC_CACHE_DIR: NativeArgument<String?> = NativeArgument("X_IC_CACHE_DIR")

    /**
     * Check dependencies and download the missing ones.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_CHECK_DEPENDENCIES: NativeArgument<Boolean> =
        NativeArgument("X_CHECK_DEPENDENCIES")

    /**
     *
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_EMIT_LAZY_OBJC_HEADER: NativeArgument<String?> =
        NativeArgument("X_EMIT_LAZY_OBJC_HEADER")

    /**
     * A library to be included in the produced framework API.
     * This library must be one of the ones passed with '-library'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_EXPORT_LIBRARY: NativeArgument<Array<String>?> = NativeArgument("X_EXPORT_LIBRARY")

    /**
     * Path to the file containing external dependencies.
     * External dependencies are required for verbose output in the event of IR linker errors,
     * but they do not affect compilation at all.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_EXTERNAL_DEPENDENCIES: NativeArgument<String?> =
        NativeArgument("X_EXTERNAL_DEPENDENCIES")

    /**
     * Enable the IR fake override validator.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_FAKE_OVERRIDE_VALIDATOR: NativeArgument<Boolean> =
        NativeArgument("X_FAKE_OVERRIDE_VALIDATOR")

    /**
     * Add an additional header import to the framework header.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_FRAMEWORK_IMPORT_HEADER: NativeArgument<Array<String>?> =
        NativeArgument("X_FRAMEWORK_IMPORT_HEADER")

    /**
     * Add light debug information for optimized builds. This option is skipped in debug builds.
     * It's enabled by default on Darwin platforms where collected debug information is stored in a .dSYM file.
     * Currently this option is disabled by default on other platforms.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ADD_LIGHT_DEBUG: NativeArgument<String?> = NativeArgument("X_ADD_LIGHT_DEBUG")

    /**
     * Add light debug information. This option has been deprecated. Please use '-Xadd-light-debug=enable' instead.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_G0: NativeArgument<Boolean> = NativeArgument("X_G0")

    /**
     * Generate trampolines to make debugger breakpoint resolution more accurate (inlines, 'when', etc.).
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_G_GENERATE_DEBUG_TRAMPOLINE: NativeArgument<String?> =
        NativeArgument("X_G_GENERATE_DEBUG_TRAMPOLINE")

    /**
     * Path to a library to be added to the cache.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ADD_CACHE: NativeArgument<String?> = NativeArgument("X_ADD_CACHE")

    /**
     * Path to the file to cache.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_FILE_TO_CACHE: NativeArgument<Array<String>?> = NativeArgument("X_FILE_TO_CACHE")

    /**
     * Force the compiler to produce per-file caches.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_MAKE_PER_FILE_CACHE: NativeArgument<Boolean> =
        NativeArgument("X_MAKE_PER_FILE_CACHE")

    /**
     * Run codegen by file in N parallel threads.
     * 0 means use one thread per processor core.
     * The default value is 1.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_BACKEND_THREADS: NativeArgument<Int> = NativeArgument("X_BACKEND_THREADS")

    /**
     * Export KDoc entries in the framework header.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_EXPORT_KDOC: NativeArgument<Boolean> = NativeArgument("X_EXPORT_KDOC")

    /**
     * Print LLVM bitcode.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PRINT_BITCODE: NativeArgument<Boolean> = NativeArgument("X_PRINT_BITCODE")

    /**
     * Ensure that all calls of possibly long external functions are done in the native thread state.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_CHECK_STATE_AT_EXTERNAL_CALLS: NativeArgument<Boolean> =
        NativeArgument("X_CHECK_STATE_AT_EXTERNAL_CALLS")

    /**
     * Print IR.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PRINT_IR: NativeArgument<Boolean> = NativeArgument("X_PRINT_IR")

    /**
     * Print files.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PRINT_FILES: NativeArgument<Boolean> = NativeArgument("X_PRINT_FILES")

    /**
     * Don't link unused libraries even if explicitly specified.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PURGE_USER_LIBS: NativeArgument<Boolean> = NativeArgument("X_PURGE_USER_LIBS")

    /**
     * Write file containing the paths of dependencies used during klib compilation to the provided path
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO: NativeArgument<String?> =
        NativeArgument("X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO")

    /**
     * Override the standard 'runtime.bc' location.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_RUNTIME: NativeArgument<String?> = NativeArgument("X_RUNTIME")

    /**
     * A path to an intermediate library that should be processed in the same manner as source files.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_INCLUDE: NativeArgument<Array<String>?> = NativeArgument("X_INCLUDE")

    /**
     * A short name used to denote this library in the IDE and in a generated Objective-C header.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SHORT_MODULE_NAME: NativeArgument<String?> = NativeArgument("X_SHORT_MODULE_NAME")

    /**
     * Create a framework with a static library instead of a dynamic one.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_STATIC_FRAMEWORK: NativeArgument<Boolean> = NativeArgument("X_STATIC_FRAMEWORK")

    /**
     * Save temporary files to the given directory.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_TEMPORARY_FILES_DIR: NativeArgument<String?> =
        NativeArgument("X_TEMPORARY_FILES_DIR")

    /**
     * Save the result of the Kotlin IR to LLVM IR translation to '-Xsave-llvm-ir-directory'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SAVE_LLVM_IR_AFTER: NativeArgument<Array<String>?> =
        NativeArgument("X_SAVE_LLVM_IR_AFTER")

    /**
     * Verify LLVM bitcode after each method.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_VERIFY_BITCODE: NativeArgument<Boolean> = NativeArgument("X_VERIFY_BITCODE")

    /**
     * Verify the compiler.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_VERIFY_COMPILER: NativeArgument<String?> = NativeArgument("X_VERIFY_COMPILER")

    /**
     * Paths to friend modules.
     */
    @JvmField
    public val FRIEND_MODULES: NativeArgument<String?> = NativeArgument("FRIEND_MODULES")

    /**
     * Paths to output directories for refined modules (modules whose 'expect' declarations this module can actualize).
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_REFINES_PATHS: NativeArgument<Array<String>?> = NativeArgument("X_REFINES_PATHS")

    /**
     * Generate debug info of the given version (1, 2).
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DEBUG_INFO_VERSION: NativeArgument<Int> = NativeArgument("X_DEBUG_INFO_VERSION")

    /**
     * Disable generics support for framework header.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_NO_OBJC_GENERICS: NativeArgument<Boolean> = NativeArgument("X_NO_OBJC_GENERICS")

    /**
     * Explicit list of Clang options.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_OVERRIDE_CLANG_OPTIONS: NativeArgument<Array<String>?> =
        NativeArgument("X_OVERRIDE_CLANG_OPTIONS")

    /**
     * Allocator used at runtime.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ALLOCATOR: NativeArgument<String?> = NativeArgument("X_ALLOCATOR")

    /**
     * Save a klib that only contains the public ABI to the given path.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_HEADER_KLIB_PATH: NativeArgument<String?> = NativeArgument("X_HEADER_KLIB_PATH")

    /**
     * Remap file source directory paths in debug info.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DEBUG_PREFIX_MAP: NativeArgument<Array<String>?> =
        NativeArgument("X_DEBUG_PREFIX_MAP")

    /**
     * Perform caches pre-linking.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PRE_LINK_CACHES: NativeArgument<String?> = NativeArgument("X_PRE_LINK_CACHES")

    /**
     * Override values from 'konan.properties' with the given ones.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_OVERRIDE_KONAN_PROPERTIES: NativeArgument<Array<String>?> =
        NativeArgument("X_OVERRIDE_KONAN_PROPERTIES")

    /**
     * When to destroy the runtime – 'legacy' and 'on-shutdown' are currently supported. Note that 'legacy' mode is deprecated and will be removed.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DESTROY_RUNTIME_MODE: NativeArgument<String?> =
        NativeArgument("X_DESTROY_RUNTIME_MODE")

    /**
     * GC to use – 'noop', 'stms', and 'cms' are currently supported. This works only with '-memory-model experimental'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_GC: NativeArgument<String?> = NativeArgument("X_GC")

    /**
     * Initialize top level properties lazily per file.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_PROPERTY_LAZY_INITIALIZATION: NativeArgument<String?> =
        NativeArgument("X_IR_PROPERTY_LAZY_INITIALIZATION")

    /**
     * Unhandled exception processing in 'Worker.executeAfter'. Possible values: 'legacy' and 'use-hook'. The default value is 'legacy' and for '-memory-model experimental', the default value is 'use-hook'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WORKER_EXCEPTION_HANDLING: NativeArgument<String?> =
        NativeArgument("X_WORKER_EXCEPTION_HANDLING")

    /**
     * Choose the LLVM distribution that will be used during compilation.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_LLVM_VARIANT: NativeArgument<String?> = NativeArgument("X_LLVM_VARIANT")

    /**
     * Specify a binary option.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_BINARY: NativeArgument<Array<String>?> = NativeArgument("X_BINARY")

    /**
     * Enable logging of Native runtime internals.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_RUNTIME_LOGS: NativeArgument<String?> = NativeArgument("X_RUNTIME_LOGS")

    /**
     * Path to a file for dumping the list of all available tests.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DUMP_TESTS_TO: NativeArgument<String?> = NativeArgument("X_DUMP_TESTS_TO")

    /**
     * Use lazy IR for cached libraries.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_LAZY_IR_FOR_CACHES: NativeArgument<String?> =
        NativeArgument("X_LAZY_IR_FOR_CACHES")

    /**
     * Omit binary when compiling the framework.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_OMIT_FRAMEWORK_BINARY: NativeArgument<Boolean> =
        NativeArgument("X_OMIT_FRAMEWORK_BINARY")

    /**
     * Continue compilation from the given bitcode file.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_COMPILE_FROM_BITCODE: NativeArgument<String?> =
        NativeArgument("X_COMPILE_FROM_BITCODE")

    /**
     * Serialized dependencies to use for linking.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_READ_DEPENDENCIES_FROM: NativeArgument<String?> =
        NativeArgument("X_READ_DEPENDENCIES_FROM")

    /**
     * Path for writing backend dependencies.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WRITE_DEPENDENCIES_TO: NativeArgument<String?> =
        NativeArgument("X_WRITE_DEPENDENCIES_TO")

    /**
     * Directory that should contain the results of '-Xsave-llvm-ir-after=<phase>'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SAVE_LLVM_IR_DIRECTORY: NativeArgument<String?> =
        NativeArgument("X_SAVE_LLVM_IR_DIRECTORY")

    /**
     * Custom path to the location of konan distributions.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_KONAN_DATA_DIR: NativeArgument<String?> = NativeArgument("X_KONAN_DATA_DIR")

    /**
     * Custom set of LLVM passes to run as the ModuleOptimizationPipeline.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_LLVM_MODULE_PASSES: NativeArgument<String?> =
        NativeArgument("X_LLVM_MODULE_PASSES")

    /**
     * Custom set of LLVM passes to run as the LTOOptimizationPipeline.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_LLVM_LTO_PASSES: NativeArgument<String?> = NativeArgument("X_LLVM_LTO_PASSES")

    /**
     * Comma-separated list that will be written as the value of 'native_targets' property in the .klib manifest. Unknown values are discarded.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_MANIFEST_NATIVE_TARGETS: NativeArgument<Array<String>?> =
        NativeArgument("X_MANIFEST_NATIVE_TARGETS")
  }
}
