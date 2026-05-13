// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Boolean
import kotlin.String
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.RemovedCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsEcmaVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsIrDiagnosticMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsModuleKind

/**
 * @since 2.4.20
 */
@ExperimentalCompilerArgument
public interface JsCompilerArguments : CommonJsAndWasmArguments {
  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: JsCompilerArgument<V>): V

  /**
   * An option for configuring [JsCompilerArguments].
   *
   * @see get
   * @see set    
   */
  public class JsCompilerArgument<V>(
    public val id: String,
    public val availableSinceVersion: KotlinReleaseVersion,
  )

  /**
   * A builder for [JsCompilerArguments].
   */
  public interface Builder : CommonJsAndWasmArguments.Builder {
    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> `get`(key: JsCompilerArgument<V>): V

    /**
     * Set the [value] for option specified by [key], overriding any previous value for that option.
     */
    public operator fun <V> `set`(key: JsCompilerArgument<V>, `value`: V)

    /**
     * Constructs a new immutable [JsCompilerArguments] instance with the options set in this builder.
     */
    public fun build(): JsCompilerArguments
  }

  public companion object {
    /**
     * Enable extension function members in external interfaces.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS: JsCompilerArgument<Boolean> =
        JsCompilerArgument("X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS", KotlinReleaseVersion(1, 5, 32))

    /**
     * Enable exporting of Kotlin interfaces to implement them from JavaScript/TypeScript.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT: JsCompilerArgument<Boolean> =
        JsCompilerArgument("X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT", KotlinReleaseVersion(2, 3, 20))

    /**
     * Enable exporting suspend functions to JavaScript/TypeScript.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ENABLE_SUSPEND_FUNCTION_EXPORTING: JsCompilerArgument<Boolean> =
        JsCompilerArgument("X_ENABLE_SUSPEND_FUNCTION_EXPORTING", KotlinReleaseVersion(2, 3, 0))

    /**
     * Use ES2015 arrow functions in the JavaScript code generated for Kotlin lambdas. Enabled by default in case of ES2015 target usage
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ES_ARROW_FUNCTIONS: JsCompilerArgument<Boolean?> =
        JsCompilerArgument("X_ES_ARROW_FUNCTIONS", KotlinReleaseVersion(2, 1, 0))

    /**
     * Let generated JavaScript code use ES2015 classes. Enabled by default in case of ES2015 target usage
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ES_CLASSES: JsCompilerArgument<Boolean?> =
        JsCompilerArgument("X_ES_CLASSES", KotlinReleaseVersion(1, 8, 20))

    /**
     * Enable ES2015 generator functions usage inside the compiled code. Enabled by default in case of ES2015 target usage
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ES_GENERATORS: JsCompilerArgument<Boolean?> =
        JsCompilerArgument("X_ES_GENERATORS", KotlinReleaseVersion(2, 0, 0))

    /**
     * Compile Long values as ES2020 bigint instead of object.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ES_LONG_AS_BIGINT: JsCompilerArgument<Boolean?> =
        JsCompilerArgument("X_ES_LONG_AS_BIGINT", KotlinReleaseVersion(2, 2, 20))

    /**
     * Generate polyfills for features from the ES6+ standards.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_GENERATE_POLYFILLS: JsCompilerArgument<Boolean> =
        JsCompilerArgument("X_GENERATE_POLYFILLS", KotlinReleaseVersion(1, 8, 20))

    /**
     * Use the compiler to build the cache.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_BUILD_CACHE: JsCompilerArgument<Boolean> =
        JsCompilerArgument("X_IR_BUILD_CACHE", KotlinReleaseVersion(1, 5, 30))

    /**
     * Lambda expressions that capture values are translated into in-line anonymous JavaScript functions.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS: JsCompilerArgument<Boolean> =
        JsCompilerArgument("X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS", KotlinReleaseVersion(1, 7, 20))

    /**
     * Comma-separated list of fully qualified names not to be eliminated by DCE (if it can be reached), and for which to keep non-minified names.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_KEEP: JsCompilerArgument<String?> =
        JsCompilerArgument("X_IR_KEEP", KotlinReleaseVersion(1, 8, 20))

    /**
     * Minimize the names of members.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_MINIMIZED_MEMBER_NAMES: JsCompilerArgument<Boolean> =
        JsCompilerArgument("X_IR_MINIMIZED_MEMBER_NAMES", KotlinReleaseVersion(1, 7, 0))

    /**
     * Generate one .js file per source file.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_PER_FILE: JsCompilerArgument<Boolean> =
        JsCompilerArgument("X_IR_PER_FILE", KotlinReleaseVersion(1, 6, 20))

    /**
     * Generate one .js file per module.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_PER_MODULE: JsCompilerArgument<Boolean> =
        JsCompilerArgument("X_IR_PER_MODULE", KotlinReleaseVersion(1, 4, 20))

    /**
     * Wrap access to external 'Boolean' properties with an explicit conversion to 'Boolean'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_SAFE_EXTERNAL_BOOLEAN: JsCompilerArgument<Boolean> =
        JsCompilerArgument("X_IR_SAFE_EXTERNAL_BOOLEAN", KotlinReleaseVersion(1, 5, 30))

    /**
     * Enable runtime diagnostics when accessing external 'Boolean' properties.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC: JsCompilerArgument<JsIrDiagnosticMode?> =
        JsCompilerArgument("X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC", KotlinReleaseVersion(1, 5, 30))

    /**
     * Perform additional optimizations on the generated JS code.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_OPTIMIZE_GENERATED_JS: JsCompilerArgument<Boolean> =
        JsCompilerArgument("X_OPTIMIZE_GENERATED_JS", KotlinReleaseVersion(1, 9, 0))

    /**
     * JS expression that will be executed in runtime and be put as an Array<String> parameter of the main function
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION: JsCompilerArgument<String?> =
        JsCompilerArgument("X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION", KotlinReleaseVersion(2, 0, 0))

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
    public val X_TYPED_ARRAYS: JsCompilerArgument<Boolean> =
        JsCompilerArgument("X_TYPED_ARRAYS", KotlinReleaseVersion(1, 1, 3))

    /**
     * The kind of JS module generated by the compiler. ES modules are enabled by default in case of ES2015 target usage
     */
    @JvmField
    public val MODULE_KIND: JsCompilerArgument<JsModuleKind?> =
        JsCompilerArgument("MODULE_KIND", KotlinReleaseVersion(1, 0, 4))

    /**
     *
     *
     * Deprecated in Kotlin version 2.1.0.
     *
     * Removed in Kotlin version 2.2.0.
     */
    @JvmField
    @RemovedCompilerArgument
    public val OUTPUT: JsCompilerArgument<String?> =
        JsCompilerArgument("OUTPUT", KotlinReleaseVersion(1, 0, 0))

    /**
     * Generate JS files for the specified ECMA version.
     */
    @JvmField
    public val TARGET: JsCompilerArgument<JsEcmaVersion?> =
        JsCompilerArgument("TARGET", KotlinReleaseVersion(1, 0, 0))
  }
}
