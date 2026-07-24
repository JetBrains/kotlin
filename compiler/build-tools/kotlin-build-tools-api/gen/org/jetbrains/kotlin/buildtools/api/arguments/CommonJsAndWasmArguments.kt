// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import java.nio.`file`.Path
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion

/**
 * @since 2.4.20
 */
public interface CommonJsAndWasmArguments : CommonKlibBasedArguments {
  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: CommonJsAndWasmArgument<V>): V

  /**
   * An option for configuring [CommonJsAndWasmArguments].
   *
   * @see get
   * @see set    
   */
  public class CommonJsAndWasmArgument<V>(
    public val id: String,
    public val availableSinceVersion: KotlinReleaseVersion,
  )

  /**
   * A builder for [CommonJsAndWasmArguments].
   */
  public interface Builder : CommonKlibBasedArguments.Builder {
    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> `get`(key: CommonJsAndWasmArgument<V>): V

    /**
     * Set the [value] for option specified by [key], overriding any previous value for that option.
     */
    public operator fun <V> `set`(key: CommonJsAndWasmArgument<V>, `value`: V)

    /**
     * Constructs a new immutable [CommonJsAndWasmArguments] instance with the options set in this builder.
     *
     * @since 2.4.20
     */
    override fun build(): CommonJsAndWasmArguments
  }

  public companion object {
    /**
     * Enable the IR fake override validator.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_FAKE_OVERRIDE_VALIDATOR: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_FAKE_OVERRIDE_VALIDATOR", KotlinReleaseVersion(1, 4, 30))

    /**
     * Paths to friend modules.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_FRIEND_MODULES: CommonJsAndWasmArgument<List<Path>?> =
        CommonJsAndWasmArgument("X_FRIEND_MODULES", KotlinReleaseVersion(1, 1, 3))

    /**
     * Disable internal declaration export.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_FRIEND_MODULES_DISABLED: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_FRIEND_MODULES_DISABLED", KotlinReleaseVersion(1, 1, 3))

    /**
     * Specify the name of the compilation module for the IR backend.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_MODULE_NAME: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("X_IR_MODULE_NAME", KotlinReleaseVersion(1, 4, 0))

    /**
     * Destination for generated files.
     */
    @JvmField
    public val IR_OUTPUT_DIR: CommonJsAndWasmArgument<Path?> =
        CommonJsAndWasmArgument("IR_OUTPUT_DIR", KotlinReleaseVersion(1, 8, 20))

    /**
     * Base name of generated files.
     */
    @JvmField
    public val IR_OUTPUT_NAME: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("IR_OUTPUT_NAME", KotlinReleaseVersion(1, 8, 20))

    /**
     * Paths to Kotlin libraries with .meta.js and .kjsm files, separated by the system path separator.
     */
    @JvmField
    public val LIBRARIES: CommonJsAndWasmArgument<List<Path>?> =
        CommonJsAndWasmArgument("LIBRARIES", KotlinReleaseVersion(1, 1, 0))

    /**
     * Don't pack the library into a klib file.
     */
    @JvmField
    public val NOPACK: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("NOPACK", KotlinReleaseVersion(2, 4, 20))
  }
}
