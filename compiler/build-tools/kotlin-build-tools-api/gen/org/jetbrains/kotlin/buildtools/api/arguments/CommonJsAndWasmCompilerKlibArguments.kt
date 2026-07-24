// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Boolean
import kotlin.String
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.DeprecatedCompilerArgument
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion

/**
 * @since 2.4.20
 */
public interface CommonJsAndWasmCompilerKlibArguments : CommonJsAndWasmArguments,
    CommonKlibBasedArgumentsKlibArguments {
  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: CommonJsAndWasmCompilerKlibArgument<V>): V

  /**
   * An option for configuring [CommonJsAndWasmCompilerKlibArguments].
   *
   * @see get
   * @see set    
   */
  public class CommonJsAndWasmCompilerKlibArgument<V>(
    public val id: String,
    public val availableSinceVersion: KotlinReleaseVersion,
  )

  /**
   * A builder for [CommonJsAndWasmCompilerKlibArguments].
   */
  public interface Builder : CommonJsAndWasmArguments.Builder,
      CommonKlibBasedArgumentsKlibArguments.Builder {
    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> `get`(key: CommonJsAndWasmCompilerKlibArgument<V>): V

    /**
     * Set the [value] for option specified by [key], overriding any previous value for that option.
     */
    public operator fun <V> `set`(key: CommonJsAndWasmCompilerKlibArgument<V>, `value`: V)

    /**
     * Constructs a new immutable [CommonJsAndWasmCompilerKlibArguments] instance with the options set in this builder.
     *
     * @since 2.4.20
     */
    override fun build(): CommonJsAndWasmCompilerKlibArguments
  }

  public companion object {
    /**
     * Add a custom output name to the split .js files.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_PER_MODULE_OUTPUT_NAME: CommonJsAndWasmCompilerKlibArgument<String?> =
        CommonJsAndWasmCompilerKlibArgument("X_IR_PER_MODULE_OUTPUT_NAME", KotlinReleaseVersion(1, 5, 30))

    /**
     * Generate an unpacked klib into the parent directory of the output JS file.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     *
     * Deprecated in Kotlin version 2.4.20.
     */
    @JvmField
    @ExperimentalCompilerArgument
    @DeprecatedCompilerArgument
    public val X_IR_PRODUCE_KLIB_DIR: CommonJsAndWasmCompilerKlibArgument<Boolean?> =
        CommonJsAndWasmCompilerKlibArgument("X_IR_PRODUCE_KLIB_DIR", KotlinReleaseVersion(1, 3, 70))

    /**
     * Generate a packed klib into the directory specified by '-ir-output-dir'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     *
     * Deprecated in Kotlin version 2.4.20.
     */
    @JvmField
    @ExperimentalCompilerArgument
    @DeprecatedCompilerArgument
    public val X_IR_PRODUCE_KLIB_FILE: CommonJsAndWasmCompilerKlibArgument<Boolean?> =
        CommonJsAndWasmCompilerKlibArgument("X_IR_PRODUCE_KLIB_FILE", KotlinReleaseVersion(1, 3, 70))
  }
}
