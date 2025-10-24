// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Boolean
import kotlin.String
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.RemovedCompilerArgument

/**
 * @since 2.3.0
 */
@ExperimentalCompilerArgument
public interface JsArguments : WasmArguments {
  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: JsArgument<V>): V

  /**
   * Set the [value] for option specified by [key], overriding any previous value for that option.
   */
  public operator fun <V> `set`(key: JsArgument<V>, `value`: V)

  /**
   * Check if an option specified by [key] has a value set.
   *
   * Note: trying to read an option (by using [get]) that has not been set will result in an exception.
   *
   * @return true if the option has a value set, false otherwise
   */
  public operator fun contains(key: JsArgument<*>): Boolean

  /**
   * Base class for [JsArguments] options.
   *
   * @see get
   * @see set    
   */
  public class JsArgument<V>(
    public val id: String,
    public val availableSinceVersion: KotlinReleaseVersion,
  )

  public companion object {
    /**
     * Path to the cache directory.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_CACHE_DIRECTORY: JsArgument<String?> =
        JsArgument("X_CACHE_DIRECTORY", KotlinReleaseVersion(1, 8, 20))

    /**
     * Enable extension function members in external interfaces.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS: JsArgument<Boolean> =
        JsArgument("X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS", KotlinReleaseVersion(1, 5, 32))

    /**
     * Use ES2015 arrow functions in the JavaScript code generated for Kotlin lambdas. Enabled by default in case of ES2015 target usage
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ES_ARROW_FUNCTIONS: JsArgument<Boolean?> =
        JsArgument("X_ES_ARROW_FUNCTIONS", KotlinReleaseVersion(2, 1, 0))

    /**
     * Let generated JavaScript code use ES2015 classes. Enabled by default in case of ES2015 target usage
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ES_CLASSES: JsArgument<Boolean?> =
        JsArgument("X_ES_CLASSES", KotlinReleaseVersion(1, 8, 20))

    /**
     * Enable ES2015 generator functions usage inside the compiled code. Enabled by default in case of ES2015 target usage
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ES_GENERATORS: JsArgument<Boolean?> =
        JsArgument("X_ES_GENERATORS", KotlinReleaseVersion(2, 0, 0))

    /**
     * Compile Long values as ES2020 bigint instead of object.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ES_LONG_AS_BIGINT: JsArgument<Boolean?> =
        JsArgument("X_ES_LONG_AS_BIGINT", KotlinReleaseVersion(2, 2, 20))

    /**
     * Enable the IR fake override validator.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_FAKE_OVERRIDE_VALIDATOR: JsArgument<Boolean> =
        JsArgument("X_FAKE_OVERRIDE_VALIDATOR", KotlinReleaseVersion(1, 4, 30))

    /**
     * Paths to friend modules.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_FRIEND_MODULES: JsArgument<String?> =
        JsArgument("X_FRIEND_MODULES", KotlinReleaseVersion(1, 1, 3))

    /**
     * Disable internal declaration export.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_FRIEND_MODULES_DISABLED: JsArgument<Boolean> =
        JsArgument("X_FRIEND_MODULES_DISABLED", KotlinReleaseVersion(1, 1, 3))

    /**
     * Generate a TypeScript declaration .d.ts file alongside the JS file.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_GENERATE_DTS: JsArgument<Boolean> =
        JsArgument("X_GENERATE_DTS", KotlinReleaseVersion(1, 3, 70))

    /**
     * Generate polyfills for features from the ES6+ standards.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_GENERATE_POLYFILLS: JsArgument<Boolean> =
        JsArgument("X_GENERATE_POLYFILLS", KotlinReleaseVersion(1, 8, 20))

    /**
     * Path to an intermediate library that should be processed in the same manner as source files.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_INCLUDE: JsArgument<String?> =
        JsArgument("X_INCLUDE", KotlinReleaseVersion(1, 4, 0))

    /**
     * Use the compiler to build the cache.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_BUILD_CACHE: JsArgument<Boolean> =
        JsArgument("X_IR_BUILD_CACHE", KotlinReleaseVersion(1, 5, 30))

    /**
     * Perform experimental dead code elimination.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_DCE: JsArgument<Boolean> =
        JsArgument("X_IR_DCE", KotlinReleaseVersion(1, 3, 70))

    /**
     * Print reachability information about declarations to 'stdout' while performing DCE.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_DCE_PRINT_REACHABILITY_INFO: JsArgument<Boolean> =
        JsArgument("X_IR_DCE_PRINT_REACHABILITY_INFO", KotlinReleaseVersion(1, 4, 0))

    /**
     * Enable runtime diagnostics instead of removing declarations when performing DCE.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_DCE_RUNTIME_DIAGNOSTIC: JsArgument<String?> =
        JsArgument("X_IR_DCE_RUNTIME_DIAGNOSTIC", KotlinReleaseVersion(1, 5, 0))

    /**
     * Lambda expressions that capture values are translated into in-line anonymous JavaScript functions.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS: JsArgument<Boolean> =
        JsArgument("X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS", KotlinReleaseVersion(1, 7, 20))

    /**
     * Comma-separated list of fully qualified names not to be eliminated by DCE (if it can be reached), and for which to keep non-minified names.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_KEEP: JsArgument<String?> =
        JsArgument("X_IR_KEEP", KotlinReleaseVersion(1, 8, 20))

    /**
     * Minimize the names of members.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_MINIMIZED_MEMBER_NAMES: JsArgument<Boolean> =
        JsArgument("X_IR_MINIMIZED_MEMBER_NAMES", KotlinReleaseVersion(1, 7, 0))

    /**
     * Specify the name of the compilation module for the IR backend.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_MODULE_NAME: JsArgument<String?> =
        JsArgument("X_IR_MODULE_NAME", KotlinReleaseVersion(1, 4, 0))

    /**
     * Generate one .js file per source file.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_PER_FILE: JsArgument<Boolean> =
        JsArgument("X_IR_PER_FILE", KotlinReleaseVersion(1, 6, 20))

    /**
     * Generate one .js file per module.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_PER_MODULE: JsArgument<Boolean> =
        JsArgument("X_IR_PER_MODULE", KotlinReleaseVersion(1, 4, 20))

    /**
     * Add a custom output name to the split .js files.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_PER_MODULE_OUTPUT_NAME: JsArgument<String?> =
        JsArgument("X_IR_PER_MODULE_OUTPUT_NAME", KotlinReleaseVersion(1, 5, 30))

    /**
     * Generate a JS file using the IR backend.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_PRODUCE_JS: JsArgument<Boolean> =
        JsArgument("X_IR_PRODUCE_JS", KotlinReleaseVersion(1, 3, 70))

    /**
     * Generate an unpacked klib into the parent directory of the output JS file.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_PRODUCE_KLIB_DIR: JsArgument<Boolean> =
        JsArgument("X_IR_PRODUCE_KLIB_DIR", KotlinReleaseVersion(1, 3, 70))

    /**
     * Generate a packed klib into the directory specified by '-ir-output-dir'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_PRODUCE_KLIB_FILE: JsArgument<Boolean> =
        JsArgument("X_IR_PRODUCE_KLIB_FILE", KotlinReleaseVersion(1, 3, 70))

    /**
     * Perform lazy initialization for properties.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_PROPERTY_LAZY_INITIALIZATION: JsArgument<Boolean> =
        JsArgument("X_IR_PROPERTY_LAZY_INITIALIZATION", KotlinReleaseVersion(1, 4, 30))

    /**
     * Wrap access to external 'Boolean' properties with an explicit conversion to 'Boolean'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_SAFE_EXTERNAL_BOOLEAN: JsArgument<Boolean> =
        JsArgument("X_IR_SAFE_EXTERNAL_BOOLEAN", KotlinReleaseVersion(1, 5, 30))

    /**
     * Enable runtime diagnostics when accessing external 'Boolean' properties.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC: JsArgument<String?> =
        JsArgument("X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC", KotlinReleaseVersion(1, 5, 30))

    /**
     * Perform additional optimizations on the generated JS code.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_OPTIMIZE_GENERATED_JS: JsArgument<Boolean> =
        JsArgument("X_OPTIMIZE_GENERATED_JS", KotlinReleaseVersion(1, 9, 0))

    /**
     * JS expression that will be executed in runtime and be put as an Array<String> parameter of the main function
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION: JsArgument<String?> =
        JsArgument("X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION", KotlinReleaseVersion(2, 0, 0))

    /**
     * Generate strict types for implicitly exported entities inside d.ts files.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_STRICT_IMPLICIT_EXPORT_TYPES: JsArgument<Boolean> =
        JsArgument("X_STRICT_IMPLICIT_EXPORT_TYPES", KotlinReleaseVersion(1, 8, 0))

    /**
     * This option does nothing and is left for compatibility with the legacy backend.
     * It is deprecated and will be removed in a future release.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     *
     * Deprecated in Kotlin version 2.1.0.
     *
     * Removed in Kotlin version 2.3.0.
     */
    @JvmField
    @ExperimentalCompilerArgument
    @RemovedCompilerArgument
    public val X_TYPED_ARRAYS: JsArgument<Boolean> =
        JsArgument("X_TYPED_ARRAYS", KotlinReleaseVersion(1, 1, 3))

    /**
     * Destination for generated files.
     */
    @JvmField
    public val IR_OUTPUT_DIR: JsArgument<String?> =
        JsArgument("IR_OUTPUT_DIR", KotlinReleaseVersion(1, 8, 20))

    /**
     * Base name of generated files.
     */
    @JvmField
    public val IR_OUTPUT_NAME: JsArgument<String?> =
        JsArgument("IR_OUTPUT_NAME", KotlinReleaseVersion(1, 8, 20))

    /**
     * Paths to Kotlin libraries with .meta.js and .kjsm files, separated by the system path separator.
     */
    @JvmField
    public val LIBRARIES: JsArgument<String?> =
        JsArgument("LIBRARIES", KotlinReleaseVersion(1, 1, 0))

    /**
     * Specify whether the 'main' function should be called upon execution.
     */
    @JvmField
    public val MAIN: JsArgument<String?> = JsArgument("MAIN", KotlinReleaseVersion(1, 0, 0))

    /**
     * The kind of JS module generated by the compiler. ES modules are enabled by default in case of ES2015 target usage
     */
    @JvmField
    public val MODULE_KIND: JsArgument<String?> =
        JsArgument("MODULE_KIND", KotlinReleaseVersion(1, 0, 4))

    /**
     *
     *
     * Deprecated in Kotlin version 2.1.0.
     *
     * Removed in Kotlin version 2.2.0.
     */
    @JvmField
    @RemovedCompilerArgument
    public val OUTPUT: JsArgument<String?> = JsArgument("OUTPUT", KotlinReleaseVersion(1, 0, 0))

    /**
     * Generate a source map.
     */
    @JvmField
    public val SOURCE_MAP: JsArgument<Boolean> =
        JsArgument("SOURCE_MAP", KotlinReleaseVersion(1, 0, 0))

    /**
     * Base directories for calculating relative paths to source files in the source map.
     */
    @JvmField
    public val SOURCE_MAP_BASE_DIRS: JsArgument<String?> =
        JsArgument("SOURCE_MAP_BASE_DIRS", KotlinReleaseVersion(1, 1, 60))

    /**
     * Embed source files into the source map.
     */
    @JvmField
    public val SOURCE_MAP_EMBED_SOURCES: JsArgument<String?> =
        JsArgument("SOURCE_MAP_EMBED_SOURCES", KotlinReleaseVersion(1, 1, 4))

    /**
     * Mode for mapping generated names to original names.
     */
    @JvmField
    public val SOURCE_MAP_NAMES_POLICY: JsArgument<String?> =
        JsArgument("SOURCE_MAP_NAMES_POLICY", KotlinReleaseVersion(1, 8, 20))

    /**
     * Add the specified prefix to the paths in the source map.
     */
    @JvmField
    public val SOURCE_MAP_PREFIX: JsArgument<String?> =
        JsArgument("SOURCE_MAP_PREFIX", KotlinReleaseVersion(1, 1, 4))

    /**
     * Generate JS files for the specified ECMA version.
     */
    @JvmField
    public val TARGET: JsArgument<String?> = JsArgument("TARGET", KotlinReleaseVersion(1, 0, 0))
  }
}
