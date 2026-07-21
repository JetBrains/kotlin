// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Boolean
import kotlin.String
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsEcmaVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsIrDiagnosticMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsModuleKind

/**
 * @since 2.4.20
 */
public interface JsCompilerLinkingArguments : JsCompilerArguments,
    CommonJsAndWasmCompilerLinkingArguments {
  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: JsCompilerLinkingArgument<V>): V

  /**
   * An option for configuring [JsCompilerLinkingArguments].
   *
   * @see get
   * @see set    
   */
  public class JsCompilerLinkingArgument<V>(
    public val id: String,
    public val availableSinceVersion: KotlinReleaseVersion,
  )

  /**
   * A builder for [JsCompilerLinkingArguments].
   */
  public interface Builder : JsCompilerArguments.Builder,
      CommonJsAndWasmCompilerLinkingArguments.Builder {
    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> `get`(key: JsCompilerLinkingArgument<V>): V

    /**
     * Set the [value] for option specified by [key], overriding any previous value for that option.
     */
    public operator fun <V> `set`(key: JsCompilerLinkingArgument<V>, `value`: V)

    /**
     * Constructs a new immutable [JsCompilerLinkingArguments] instance with the options set in this builder.
     */
    override fun build(): JsCompilerLinkingArguments
  }

  public companion object {
    /**
     * Use ES2015 arrow functions in the JavaScript code generated for Kotlin lambdas. Enabled by default in case of ES2015 target usage
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ES_ARROW_FUNCTIONS: JsCompilerLinkingArgument<Boolean?> =
        JsCompilerLinkingArgument("X_ES_ARROW_FUNCTIONS", KotlinReleaseVersion(2, 1, 0))

    /**
     * Let generated JavaScript code use ES2015 classes. Enabled by default in case of ES2015 target usage
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ES_CLASSES: JsCompilerLinkingArgument<Boolean?> =
        JsCompilerLinkingArgument("X_ES_CLASSES", KotlinReleaseVersion(1, 8, 20))

    /**
     * Enable ES2015 generator functions usage inside the compiled code. Enabled by default in case of ES2015 target usage
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ES_GENERATORS: JsCompilerLinkingArgument<Boolean?> =
        JsCompilerLinkingArgument("X_ES_GENERATORS", KotlinReleaseVersion(2, 0, 0))

    /**
     * Compile Long values as ES2020 bigint instead of object.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ES_LONG_AS_BIGINT: JsCompilerLinkingArgument<Boolean?> =
        JsCompilerLinkingArgument("X_ES_LONG_AS_BIGINT", KotlinReleaseVersion(2, 2, 20))

    /**
     * Generate polyfills for features from the ES6+ standards.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_GENERATE_POLYFILLS: JsCompilerLinkingArgument<Boolean> =
        JsCompilerLinkingArgument("X_GENERATE_POLYFILLS", KotlinReleaseVersion(1, 8, 20))

    /**
     * Use the compiler to build the cache.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_BUILD_CACHE: JsCompilerLinkingArgument<Boolean> =
        JsCompilerLinkingArgument("X_IR_BUILD_CACHE", KotlinReleaseVersion(1, 5, 30))

    /**
     * Lambda expressions that capture values are translated into in-line anonymous JavaScript functions.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS: JsCompilerLinkingArgument<Boolean> =
        JsCompilerLinkingArgument("X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS", KotlinReleaseVersion(1, 7, 20))

    /**
     * Comma-separated list of fully qualified names not to be eliminated by DCE (if it can be reached), and for which to keep non-minified names.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_KEEP: JsCompilerLinkingArgument<String?> =
        JsCompilerLinkingArgument("X_IR_KEEP", KotlinReleaseVersion(1, 8, 20))

    /**
     * Minimize the names of members.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_MINIMIZED_MEMBER_NAMES: JsCompilerLinkingArgument<Boolean> =
        JsCompilerLinkingArgument("X_IR_MINIMIZED_MEMBER_NAMES", KotlinReleaseVersion(1, 7, 0))

    /**
     * Generate one .js file per source file.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_PER_FILE: JsCompilerLinkingArgument<Boolean> =
        JsCompilerLinkingArgument("X_IR_PER_FILE", KotlinReleaseVersion(1, 6, 20))

    /**
     * Generate one .js file per module.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_PER_MODULE: JsCompilerLinkingArgument<Boolean> =
        JsCompilerLinkingArgument("X_IR_PER_MODULE", KotlinReleaseVersion(1, 4, 20))

    /**
     * Wrap access to external 'Boolean' properties with an explicit conversion to 'Boolean'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_SAFE_EXTERNAL_BOOLEAN: JsCompilerLinkingArgument<Boolean> =
        JsCompilerLinkingArgument("X_IR_SAFE_EXTERNAL_BOOLEAN", KotlinReleaseVersion(1, 5, 30))

    /**
     * Enable runtime diagnostics when accessing external 'Boolean' properties.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC: JsCompilerLinkingArgument<JsIrDiagnosticMode?>
        =
        JsCompilerLinkingArgument("X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC", KotlinReleaseVersion(1, 5, 30))

    /**
     * Perform additional optimizations on the generated JS code.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_OPTIMIZE_GENERATED_JS: JsCompilerLinkingArgument<Boolean> =
        JsCompilerLinkingArgument("X_OPTIMIZE_GENERATED_JS", KotlinReleaseVersion(1, 9, 0))

    /**
     * JS expression that will be executed in runtime and be put as an Array<String> parameter of the main function
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION: JsCompilerLinkingArgument<String?> =
        JsCompilerLinkingArgument("X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION", KotlinReleaseVersion(2, 0, 0))

    /**
     * Export 'dynamic' and 'Any' Kotlin types as 'unknown' TypeScript type.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_TS_EXPORT_UNTYPED_AS_UNKNOWN: JsCompilerLinkingArgument<Boolean> =
        JsCompilerLinkingArgument("X_TS_EXPORT_UNTYPED_AS_UNKNOWN", KotlinReleaseVersion(2, 5, 0))

    /**
     * The kind of JS module generated by the compiler. ES modules are enabled by default in case of ES2015 target usage
     */
    @JvmField
    public val MODULE_KIND: JsCompilerLinkingArgument<JsModuleKind?> =
        JsCompilerLinkingArgument("MODULE_KIND", KotlinReleaseVersion(1, 0, 4))

    /**
     * Generate JS files for the specified ECMA version.
     */
    @JvmField
    public val TARGET: JsCompilerLinkingArgument<JsEcmaVersion?> =
        JsCompilerLinkingArgument("TARGET", KotlinReleaseVersion(1, 0, 0))
  }
}
