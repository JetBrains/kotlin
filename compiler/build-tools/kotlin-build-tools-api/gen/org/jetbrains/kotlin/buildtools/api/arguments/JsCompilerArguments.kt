// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Boolean
import kotlin.String
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.RemovedCompilerArgument

/**
 * @since 2.4.20
 */
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
     *
     * @since 2.4.20
     */
    override fun build(): JsCompilerArguments
  }

  public companion object {
    /**
     * Enable exporting suspend lambdas to JavaScript/TypeScript.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SUSPEND_LAMBDA_EXPORTING: JsCompilerArgument<Boolean> =
        JsCompilerArgument("X_SUSPEND_LAMBDA_EXPORTING", KotlinReleaseVersion(2, 4, 20))

    /**
     * This option does nothing and is left for compatibility with the legacy backend.
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
  }
}
