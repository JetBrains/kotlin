// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Array
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.DeprecatedCompilerArgument
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.RemovedCompilerArgument

/**
 * @since 2.4.20
 */
public interface NativeCompilerLinkingArguments : NativeCompilerArguments,
    CommonKlibBasedArgumentsLinkingArguments {
  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: NativeCompilerLinkingArgument<V>): V

  /**
   * An option for configuring [NativeCompilerLinkingArguments].
   *
   * @see get
   * @see set    
   */
  public class NativeCompilerLinkingArgument<V>(
    public val id: String,
    public val availableSinceVersion: KotlinReleaseVersion,
  )

  /**
   * A builder for [NativeCompilerLinkingArguments].
   */
  public interface Builder : NativeCompilerArguments.Builder,
      CommonKlibBasedArgumentsLinkingArguments.Builder {
    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> `get`(key: NativeCompilerLinkingArgument<V>): V

    /**
     * Set the [value] for option specified by [key], overriding any previous value for that option.
     */
    public operator fun <V> `set`(key: NativeCompilerLinkingArgument<V>, `value`: V)

    /**
     * Constructs a new immutable [NativeCompilerLinkingArguments] instance with the options set in this builder.
     */
    override fun build(): NativeCompilerLinkingArguments
  }

  public companion object {
    /**
     * Path to a library to be added to the cache.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ADD_CACHE: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_ADD_CACHE", KotlinReleaseVersion(1, 5, 20))

    /**
     * Add light debug information for optimized builds. This option is skipped in debug builds.
     * It's enabled by default on Darwin platforms where collected debug information is stored in a .dSYM file.
     * Currently this option is disabled by default on other platforms.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ADD_LIGHT_DEBUG: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_ADD_LIGHT_DEBUG", KotlinReleaseVersion(1, 5, 20))

    /**
     * Allocator used at runtime.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ALLOCATOR: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_ALLOCATOR", KotlinReleaseVersion(1, 5, 20))

    /**
     * Path to the directory where caches for auto-cacheable dependencies should be put.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_AUTO_CACHE_DIR: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_AUTO_CACHE_DIR", KotlinReleaseVersion(1, 8, 20))

    /**
     * Path to the root directory from which dependencies are to be cached automatically.
     * By default caches will be placed into the kotlin-native system cache directory.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_AUTO_CACHE_FROM: NativeCompilerLinkingArgument<Array<String>?> =
        NativeCompilerLinkingArgument("X_AUTO_CACHE_FROM", KotlinReleaseVersion(1, 8, 20))

    /**
     * Run codegen by file in N parallel threads.
     * 0 means use one thread per processor core.
     * The default value is 1.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_BACKEND_THREADS: NativeCompilerLinkingArgument<Int> =
        NativeCompilerLinkingArgument("X_BACKEND_THREADS", KotlinReleaseVersion(1, 9, 0))

    /**
     * Specify a binary option.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_BINARY: NativeCompilerLinkingArgument<Array<String>?> =
        NativeCompilerLinkingArgument("X_BINARY", KotlinReleaseVersion(1, 6, 0))

    /**
     * Bundle ID to be set in the Info.plist file of the produced framework.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     *
     * Deprecated in Kotlin version 1.7.20.
     *
     * Removed in Kotlin version 2.5.0.
     */
    @JvmField
    @ExperimentalCompilerArgument
    @RemovedCompilerArgument
    public val X_BUNDLE_ID: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_BUNDLE_ID", KotlinReleaseVersion(1, 6, 20))

    /**
     * Path to the directory containing caches.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_CACHE_DIRECTORY: NativeCompilerLinkingArgument<Array<String>?> =
        NativeCompilerLinkingArgument("X_CACHE_DIRECTORY", KotlinReleaseVersion(1, 5, 20))

    /**
     * Paths to a library and its cache, separated by a comma.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_CACHED_LIBRARY: NativeCompilerLinkingArgument<Array<String>?> =
        NativeCompilerLinkingArgument("X_CACHED_LIBRARY", KotlinReleaseVersion(1, 5, 20))

    /**
     * Ensure that all calls of possibly long external functions are done in the native thread state.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_CHECK_STATE_AT_EXTERNAL_CALLS: NativeCompilerLinkingArgument<Boolean> =
        NativeCompilerLinkingArgument("X_CHECK_STATE_AT_EXTERNAL_CALLS", KotlinReleaseVersion(1, 6, 0))

    /**
     * Continue compilation from the given bitcode file.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_COMPILE_FROM_BITCODE: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_COMPILE_FROM_BITCODE", KotlinReleaseVersion(1, 9, 0))

    /**
     * Generate debug info of the given version (1, 2).
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DEBUG_INFO_VERSION: NativeCompilerLinkingArgument<Int> =
        NativeCompilerLinkingArgument("X_DEBUG_INFO_VERSION", KotlinReleaseVersion(1, 5, 20))

    /**
     * Remap file source directory paths in debug info.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DEBUG_PREFIX_MAP: NativeCompilerLinkingArgument<Array<String>?> =
        NativeCompilerLinkingArgument("X_DEBUG_PREFIX_MAP", KotlinReleaseVersion(1, 5, 20))

    /**
     * When to destroy the runtime.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     *
     * Removed in Kotlin version 2.5.0.
     */
    @JvmField
    @ExperimentalCompilerArgument
    @RemovedCompilerArgument
    public val X_DESTROY_RUNTIME_MODE: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_DESTROY_RUNTIME_MODE", KotlinReleaseVersion(1, 5, 20))

    /**
     * Path to a file where the list of all cache archives produced by this build should be written.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DUMP_BUILT_CACHES_TO: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_DUMP_BUILT_CACHES_TO", KotlinReleaseVersion(2, 4, 20))

    /**
     * Path to a file for dumping the list of all available tests.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DUMP_TESTS_TO: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_DUMP_TESTS_TO", KotlinReleaseVersion(1, 7, 0))

    /**
     *
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_EMIT_LAZY_OBJC_HEADER: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_EMIT_LAZY_OBJC_HEADER", KotlinReleaseVersion(1, 5, 20))

    /**
     * A library to be included in the produced framework API.
     * This library must be one of the ones passed with '-library'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_EXPORT_LIBRARY: NativeCompilerLinkingArgument<Array<String>?> =
        NativeCompilerLinkingArgument("X_EXPORT_LIBRARY", KotlinReleaseVersion(1, 5, 20))

    /**
     * Path to the file containing external dependencies.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_EXTERNAL_DEPENDENCIES: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_EXTERNAL_DEPENDENCIES", KotlinReleaseVersion(2, 0, 0))

    /**
     * Path to the file to cache.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_FILE_TO_CACHE: NativeCompilerLinkingArgument<Array<String>?> =
        NativeCompilerLinkingArgument("X_FILE_TO_CACHE", KotlinReleaseVersion(1, 8, 0))

    /**
     * Add an additional header import to the framework header.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_FRAMEWORK_IMPORT_HEADER: NativeCompilerLinkingArgument<Array<String>?> =
        NativeCompilerLinkingArgument("X_FRAMEWORK_IMPORT_HEADER", KotlinReleaseVersion(1, 5, 20))

    /**
     * Generate trampolines to make debugger breakpoint resolution more accurate (inlines, 'when', etc.).
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_G_GENERATE_DEBUG_TRAMPOLINE: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_G_GENERATE_DEBUG_TRAMPOLINE", KotlinReleaseVersion(1, 5, 30))

    /**
     * Add light debug information.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     *
     * Deprecated in Kotlin version 1.5.20.
     *
     * Removed in Kotlin version 2.4.20.
     */
    @JvmField
    @ExperimentalCompilerArgument
    @RemovedCompilerArgument
    public val X_G0: NativeCompilerLinkingArgument<Boolean> =
        NativeCompilerLinkingArgument("X_G0", KotlinReleaseVersion(1, 5, 20))

    /**
     * GC to use – 'noop', 'stms', and 'cms' are currently supported. This works only with '-memory-model experimental'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_GC: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_GC", KotlinReleaseVersion(1, 5, 30))

    /**
     * Path to the directory where incremental build caches should be put.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IC_CACHE_DIR: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_IC_CACHE_DIR", KotlinReleaseVersion(1, 9, 20))

    /**
     * A path to an intermediate library that should be processed in the same manner as source files.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_INCLUDE: NativeCompilerLinkingArgument<Array<String>?> =
        NativeCompilerLinkingArgument("X_INCLUDE", KotlinReleaseVersion(1, 5, 20))

    /**
     * Initialize top level properties lazily per file.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_PROPERTY_LAZY_INITIALIZATION: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_IR_PROPERTY_LAZY_INITIALIZATION", KotlinReleaseVersion(1, 6, 0))

    /**
     * Custom set of LLVM passes to run as the LTOOptimizationPipeline.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_LLVM_LTO_PASSES: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_LLVM_LTO_PASSES", KotlinReleaseVersion(2, 1, 0))

    /**
     * Custom set of LLVM passes to run as the ModuleOptimizationPipeline.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_LLVM_MODULE_PASSES: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_LLVM_MODULE_PASSES", KotlinReleaseVersion(2, 1, 0))

    /**
     * Choose the LLVM distribution that will be used during compilation.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_LLVM_VARIANT: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_LLVM_VARIANT", KotlinReleaseVersion(1, 6, 0))

    /**
     * Force the compiler to produce per-file caches.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_MAKE_PER_FILE_CACHE: NativeCompilerLinkingArgument<Boolean> =
        NativeCompilerLinkingArgument("X_MAKE_PER_FILE_CACHE", KotlinReleaseVersion(1, 8, 0))

    /**
     * Disable generics support for framework header.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_NO_OBJC_GENERICS: NativeCompilerLinkingArgument<Boolean> =
        NativeCompilerLinkingArgument("X_NO_OBJC_GENERICS", KotlinReleaseVersion(1, 5, 20))

    /**
     * Omit binary when compiling the framework.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_OMIT_FRAMEWORK_BINARY: NativeCompilerLinkingArgument<Boolean> =
        NativeCompilerLinkingArgument("X_OMIT_FRAMEWORK_BINARY", KotlinReleaseVersion(1, 8, 0))

    /**
     * Explicit list of Clang options.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_OVERRIDE_CLANG_OPTIONS: NativeCompilerLinkingArgument<Array<String>?> =
        NativeCompilerLinkingArgument("X_OVERRIDE_CLANG_OPTIONS", KotlinReleaseVersion(1, 5, 20))

    /**
     * Override values from 'konan.properties' with the given ones.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_OVERRIDE_KONAN_PROPERTIES: NativeCompilerLinkingArgument<Array<String>?> =
        NativeCompilerLinkingArgument("X_OVERRIDE_KONAN_PROPERTIES", KotlinReleaseVersion(1, 5, 20))

    /**
     * Perform caches pre-linking.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PRE_LINK_CACHES: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_PRE_LINK_CACHES", KotlinReleaseVersion(1, 5, 20))

    /**
     * Print LLVM bitcode.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PRINT_BITCODE: NativeCompilerLinkingArgument<Boolean> =
        NativeCompilerLinkingArgument("X_PRINT_BITCODE", KotlinReleaseVersion(1, 5, 20))

    /**
     * Don't link unused libraries even if explicitly specified.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     *
     * Removed in Kotlin version 2.5.0.
     */
    @JvmField
    @ExperimentalCompilerArgument
    @RemovedCompilerArgument
    public val X_PURGE_USER_LIBS: NativeCompilerLinkingArgument<Boolean> =
        NativeCompilerLinkingArgument("X_PURGE_USER_LIBS", KotlinReleaseVersion(1, 5, 20))

    /**
     * Serialized dependencies to use for linking.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_READ_DEPENDENCIES_FROM: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_READ_DEPENDENCIES_FROM", KotlinReleaseVersion(1, 9, 0))

    /**
     * Override the standard 'runtime.bc' location.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_RUNTIME: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_RUNTIME", KotlinReleaseVersion(1, 5, 20))

    /**
     * Enable logging of Native runtime internals.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_RUNTIME_LOGS: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_RUNTIME_LOGS", KotlinReleaseVersion(1, 6, 0))

    /**
     * Save the result of the Kotlin IR to LLVM IR translation to '-Xsave-llvm-ir-directory'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SAVE_LLVM_IR_AFTER: NativeCompilerLinkingArgument<Array<String>?> =
        NativeCompilerLinkingArgument("X_SAVE_LLVM_IR_AFTER", KotlinReleaseVersion(1, 7, 0))

    /**
     * Directory that should contain the results of '-Xsave-llvm-ir-after=<phase>'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SAVE_LLVM_IR_DIRECTORY: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_SAVE_LLVM_IR_DIRECTORY", KotlinReleaseVersion(1, 9, 0))

    /**
     * Create a framework with a static library instead of a dynamic one.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_STATIC_FRAMEWORK: NativeCompilerLinkingArgument<Boolean> =
        NativeCompilerLinkingArgument("X_STATIC_FRAMEWORK", KotlinReleaseVersion(1, 5, 20))

    /**
     * Save temporary files to the given directory.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_TEMPORARY_FILES_DIR: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_TEMPORARY_FILES_DIR", KotlinReleaseVersion(1, 5, 20))

    /**
     * Verify LLVM bitcode after each method.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_VERIFY_BITCODE: NativeCompilerLinkingArgument<Boolean> =
        NativeCompilerLinkingArgument("X_VERIFY_BITCODE", KotlinReleaseVersion(1, 5, 20))

    /**
     * Verify the compiler.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_VERIFY_COMPILER: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_VERIFY_COMPILER", KotlinReleaseVersion(1, 5, 20))

    /**
     * Unhandled exception processing in 'Worker.executeAfter'. Possible values: 'legacy' and 'use-hook'. The default value is 'legacy' and for '-memory-model experimental', the default value is 'use-hook'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     *
     * Deprecated in Kotlin version 2.4.20.
     *
     * Removed in Kotlin version 2.5.0.
     */
    @JvmField
    @ExperimentalCompilerArgument
    @RemovedCompilerArgument
    public val X_WORKER_EXCEPTION_HANDLING: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_WORKER_EXCEPTION_HANDLING", KotlinReleaseVersion(1, 6, 0))

    /**
     * Path for writing backend dependencies.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WRITE_DEPENDENCIES_TO: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("X_WRITE_DEPENDENCIES_TO", KotlinReleaseVersion(1, 9, 0))

    /**
     * Enable runtime assertions in generated code.
     */
    @JvmField
    public val ENABLE_ASSERTIONS: NativeCompilerLinkingArgument<Boolean> =
        NativeCompilerLinkingArgument("ENABLE_ASSERTIONS", KotlinReleaseVersion(1, 5, 20))

    /**
     * Qualified entry point name.
     */
    @JvmField
    public val ENTRY: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("ENTRY", KotlinReleaseVersion(1, 5, 20))

    /**
     * Enable the emission of debug information.
     */
    @JvmField
    public val G: NativeCompilerLinkingArgument<Boolean> =
        NativeCompilerLinkingArgument("G", KotlinReleaseVersion(1, 5, 20))

    /**
     * Produce a runner for unit tests that doesn't force an exit.
     */
    @JvmField
    public val GENERATE_NO_EXIT_TEST_RUNNER: NativeCompilerLinkingArgument<Boolean> =
        NativeCompilerLinkingArgument("GENERATE_NO_EXIT_TEST_RUNNER", KotlinReleaseVersion(1, 5, 20))

    /**
     * Produce a runner for unit tests.
     */
    @JvmField
    public val GENERATE_TEST_RUNNER: NativeCompilerLinkingArgument<Boolean> =
        NativeCompilerLinkingArgument("GENERATE_TEST_RUNNER", KotlinReleaseVersion(1, 5, 20))

    /**
     * Produce a worker runner for unit tests.
     *
     * Deprecated in Kotlin version 2.5.0.
     */
    @JvmField
    @DeprecatedCompilerArgument
    public val GENERATE_WORKER_TEST_RUNNER: NativeCompilerLinkingArgument<Boolean> =
        NativeCompilerLinkingArgument("GENERATE_WORKER_TEST_RUNNER", KotlinReleaseVersion(1, 5, 20))

    /**
     * Pass the given argument to the linker.
     */
    @JvmField
    public val LINKER_OPTION: NativeCompilerLinkingArgument<Array<String>?> =
        NativeCompilerLinkingArgument("LINKER_OPTION", KotlinReleaseVersion(1, 5, 20))

    /**
     * Pass arguments to the linker.
     */
    @JvmField
    public val LINKER_OPTIONS: NativeCompilerLinkingArgument<Array<String>?> =
        NativeCompilerLinkingArgument("LINKER_OPTIONS", KotlinReleaseVersion(1, 5, 20))

    /**
     * Choose the memory model to be used – 'strict' and 'experimental' are currently supported.
     */
    @JvmField
    public val MEMORY_MODEL: NativeCompilerLinkingArgument<String?> =
        NativeCompilerLinkingArgument("MEMORY_MODEL", KotlinReleaseVersion(1, 5, 20))

    /**
     * Assume the 'main' entry point will be provided by external libraries.
     */
    @JvmField
    public val NOMAIN: NativeCompilerLinkingArgument<Boolean> =
        NativeCompilerLinkingArgument("NOMAIN", KotlinReleaseVersion(1, 5, 20))

    /**
     * Enable optimizations during compilation.
     */
    @JvmField
    public val OPT: NativeCompilerLinkingArgument<Boolean> =
        NativeCompilerLinkingArgument("OPT", KotlinReleaseVersion(1, 5, 20))
  }
}
