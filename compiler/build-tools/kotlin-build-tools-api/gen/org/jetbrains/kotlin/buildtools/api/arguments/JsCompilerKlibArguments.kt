// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Boolean
import kotlin.String
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion

/**
 * @since 2.4.20
 */
public interface JsCompilerKlibArguments : JsCompilerArguments,
    CommonJsAndWasmCompilerKlibArguments {
  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: JsCompilerKlibArgument<V>): V

  /**
   * An option for configuring [JsCompilerKlibArguments].
   *
   * @see get
   * @see set    
   */
  public class JsCompilerKlibArgument<V>(
    public val id: String,
    public val availableSinceVersion: KotlinReleaseVersion,
  )

  /**
   * A builder for [JsCompilerKlibArguments].
   */
  public interface Builder : JsCompilerArguments.Builder,
      CommonJsAndWasmCompilerKlibArguments.Builder {
    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> `get`(key: JsCompilerKlibArgument<V>): V

    /**
     * Set the [value] for option specified by [key], overriding any previous value for that option.
     */
    public operator fun <V> `set`(key: JsCompilerKlibArgument<V>, `value`: V)

    /**
     * Constructs a new immutable [JsCompilerKlibArguments] instance with the options set in this builder.
     */
    public fun build(): JsCompilerKlibArguments
  }

  public companion object {
    /**
     * Enable extension function members in external interfaces.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS: JsCompilerKlibArgument<Boolean> =
        JsCompilerKlibArgument("X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS", KotlinReleaseVersion(1, 5, 32))

    /**
     * Enable exporting suspend functions to JavaScript/TypeScript.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ENABLE_SUSPEND_FUNCTION_EXPORTING: JsCompilerKlibArgument<Boolean> =
        JsCompilerKlibArgument("X_ENABLE_SUSPEND_FUNCTION_EXPORTING", KotlinReleaseVersion(2, 3, 0))

    /**
     * Enable exporting of Kotlin interfaces to implement them from JavaScript/TypeScript.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT: JsCompilerKlibArgument<Boolean> =
        JsCompilerKlibArgument("X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT", KotlinReleaseVersion(2, 3, 20))
  }
}
