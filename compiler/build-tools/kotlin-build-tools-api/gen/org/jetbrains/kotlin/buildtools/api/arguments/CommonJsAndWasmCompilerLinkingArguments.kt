// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import java.nio.`file`.Path
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsIrDiagnosticMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsMainCallMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.SourceMapEmbedSources
import org.jetbrains.kotlin.buildtools.api.arguments.enums.SourceMapNamesPolicy

/**
 * @since 2.4.20
 */
public interface CommonJsAndWasmCompilerLinkingArguments : CommonJsAndWasmArguments,
    CommonKlibBasedArgumentsLinkingArguments {
  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: CommonJsAndWasmCompilerLinkingArgument<V>): V

  /**
   * An option for configuring [CommonJsAndWasmCompilerLinkingArguments].
   *
   * @see get
   * @see set    
   */
  public class CommonJsAndWasmCompilerLinkingArgument<V>(
    public val id: String,
    public val availableSinceVersion: KotlinReleaseVersion,
  )

  /**
   * A builder for [CommonJsAndWasmCompilerLinkingArguments].
   */
  public interface Builder : CommonJsAndWasmArguments.Builder,
      CommonKlibBasedArgumentsLinkingArguments.Builder {
    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> `get`(key: CommonJsAndWasmCompilerLinkingArgument<V>): V

    /**
     * Set the [value] for option specified by [key], overriding any previous value for that option.
     */
    public operator fun <V> `set`(key: CommonJsAndWasmCompilerLinkingArgument<V>, `value`: V)
  }

  public companion object {
    /**
     * Generate a JS file using the IR backend.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_PRODUCE_JS: CommonJsAndWasmCompilerLinkingArgument<Boolean> =
        CommonJsAndWasmCompilerLinkingArgument("X_IR_PRODUCE_JS", KotlinReleaseVersion(1, 3, 70))

    /**
     * Generate a source map.
     */
    @JvmField
    public val SOURCE_MAP: CommonJsAndWasmCompilerLinkingArgument<Boolean> =
        CommonJsAndWasmCompilerLinkingArgument("SOURCE_MAP", KotlinReleaseVersion(1, 0, 0))

    /**
     * Path to an intermediate library that should be processed in the same manner as source files.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_INCLUDE: CommonJsAndWasmCompilerLinkingArgument<Path?> =
        CommonJsAndWasmCompilerLinkingArgument("X_INCLUDE", KotlinReleaseVersion(1, 4, 0))

    /**
     * Destination for generated files.
     */
    @JvmField
    public val IR_OUTPUT_DIR: CommonJsAndWasmCompilerLinkingArgument<String?> =
        CommonJsAndWasmCompilerLinkingArgument("IR_OUTPUT_DIR", KotlinReleaseVersion(1, 8, 20))

    /**
     * Base name of generated files.
     */
    @JvmField
    public val IR_OUTPUT_NAME: CommonJsAndWasmCompilerLinkingArgument<String?> =
        CommonJsAndWasmCompilerLinkingArgument("IR_OUTPUT_NAME", KotlinReleaseVersion(1, 8, 20))

    /**
     * Specify the name of the compilation module for the IR backend.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_MODULE_NAME: CommonJsAndWasmCompilerLinkingArgument<String?> =
        CommonJsAndWasmCompilerLinkingArgument("X_IR_MODULE_NAME", KotlinReleaseVersion(1, 4, 0))

    /**
     * Don't pack the library into a klib file.
     */
    @JvmField
    public val NOPACK: CommonJsAndWasmCompilerLinkingArgument<Boolean> =
        CommonJsAndWasmCompilerLinkingArgument("NOPACK", KotlinReleaseVersion(2, 4, 20))

    /**
     * Paths to Kotlin libraries with .meta.js and .kjsm files, separated by the system path separator.
     */
    @JvmField
    public val LIBRARIES: CommonJsAndWasmCompilerLinkingArgument<List<Path>?> =
        CommonJsAndWasmCompilerLinkingArgument("LIBRARIES", KotlinReleaseVersion(1, 1, 0))

    /**
     * Paths to friend modules.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_FRIEND_MODULES: CommonJsAndWasmCompilerLinkingArgument<List<Path>?> =
        CommonJsAndWasmCompilerLinkingArgument("X_FRIEND_MODULES", KotlinReleaseVersion(1, 1, 3))

    /**
     * Path to the cache directory.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_CACHE_DIRECTORY: CommonJsAndWasmCompilerLinkingArgument<Path?> =
        CommonJsAndWasmCompilerLinkingArgument("X_CACHE_DIRECTORY", KotlinReleaseVersion(1, 8, 20))

    /**
     * Perform lazy initialization for properties.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_PROPERTY_LAZY_INITIALIZATION: CommonJsAndWasmCompilerLinkingArgument<Boolean> =
        CommonJsAndWasmCompilerLinkingArgument("X_IR_PROPERTY_LAZY_INITIALIZATION", KotlinReleaseVersion(1, 4, 30))

    /**
     * Perform experimental dead code elimination.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_DCE: CommonJsAndWasmCompilerLinkingArgument<Boolean> =
        CommonJsAndWasmCompilerLinkingArgument("X_IR_DCE", KotlinReleaseVersion(1, 3, 70))

    /**
     * Specify whether the 'main' function should be called upon execution.
     */
    @JvmField
    public val MAIN: CommonJsAndWasmCompilerLinkingArgument<JsMainCallMode?> =
        CommonJsAndWasmCompilerLinkingArgument("MAIN", KotlinReleaseVersion(1, 0, 0))

    /**
     * Add the specified prefix to the paths in the source map.
     */
    @JvmField
    public val SOURCE_MAP_PREFIX: CommonJsAndWasmCompilerLinkingArgument<String?> =
        CommonJsAndWasmCompilerLinkingArgument("SOURCE_MAP_PREFIX", KotlinReleaseVersion(1, 1, 4))

    /**
     * Base directories for calculating relative paths to source files in the source map.
     */
    @JvmField
    public val SOURCE_MAP_BASE_DIRS: CommonJsAndWasmCompilerLinkingArgument<String?> =
        CommonJsAndWasmCompilerLinkingArgument("SOURCE_MAP_BASE_DIRS", KotlinReleaseVersion(1, 1, 60))

    /**
     * Embed source files into the source map.
     */
    @JvmField
    public val SOURCE_MAP_EMBED_SOURCES:
        CommonJsAndWasmCompilerLinkingArgument<SourceMapEmbedSources?> =
        CommonJsAndWasmCompilerLinkingArgument("SOURCE_MAP_EMBED_SOURCES", KotlinReleaseVersion(1, 1, 4))

    /**
     * Mode for mapping generated names to original names.
     */
    @JvmField
    public val SOURCE_MAP_NAMES_POLICY:
        CommonJsAndWasmCompilerLinkingArgument<SourceMapNamesPolicy?> =
        CommonJsAndWasmCompilerLinkingArgument("SOURCE_MAP_NAMES_POLICY", KotlinReleaseVersion(1, 8, 20))

    /**
     * Disable internal declaration export.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_FRIEND_MODULES_DISABLED: CommonJsAndWasmCompilerLinkingArgument<Boolean> =
        CommonJsAndWasmCompilerLinkingArgument("X_FRIEND_MODULES_DISABLED", KotlinReleaseVersion(1, 1, 3))

    /**
     * Print reachability information about declarations to 'stdout' while performing DCE.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_DCE_PRINT_REACHABILITY_INFO: CommonJsAndWasmCompilerLinkingArgument<Boolean> =
        CommonJsAndWasmCompilerLinkingArgument("X_IR_DCE_PRINT_REACHABILITY_INFO", KotlinReleaseVersion(1, 4, 0))

    /**
     * Enable the IR fake override validator.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_FAKE_OVERRIDE_VALIDATOR: CommonJsAndWasmCompilerLinkingArgument<Boolean> =
        CommonJsAndWasmCompilerLinkingArgument("X_FAKE_OVERRIDE_VALIDATOR", KotlinReleaseVersion(1, 4, 30))

    /**
     * Enable runtime diagnostics instead of removing declarations when performing DCE.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_DCE_RUNTIME_DIAGNOSTIC:
        CommonJsAndWasmCompilerLinkingArgument<JsIrDiagnosticMode?> =
        CommonJsAndWasmCompilerLinkingArgument("X_IR_DCE_RUNTIME_DIAGNOSTIC", KotlinReleaseVersion(1, 5, 0))

    /**
     * Generate a TypeScript declaration .d.ts file alongside the JS file.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_GENERATE_DTS: CommonJsAndWasmCompilerLinkingArgument<Boolean> =
        CommonJsAndWasmCompilerLinkingArgument("X_GENERATE_DTS", KotlinReleaseVersion(1, 3, 70))

    /**
     * Generate strict types for implicitly exported entities inside d.ts files.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_STRICT_IMPLICIT_EXPORT_TYPES: CommonJsAndWasmCompilerLinkingArgument<Boolean> =
        CommonJsAndWasmCompilerLinkingArgument("X_STRICT_IMPLICIT_EXPORT_TYPES", KotlinReleaseVersion(1, 8, 0))
  }
}
