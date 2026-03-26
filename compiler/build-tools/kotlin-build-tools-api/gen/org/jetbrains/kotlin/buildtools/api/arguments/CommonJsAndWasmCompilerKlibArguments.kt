// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import java.nio.`file`.Path
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List
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
  }

  public companion object {
    /**
     * Destination for generated files.
     */
    @JvmField
    public val IR_OUTPUT_DIR: CommonJsAndWasmCompilerKlibArgument<String?> =
        CommonJsAndWasmCompilerKlibArgument("IR_OUTPUT_DIR", KotlinReleaseVersion(1, 8, 20))

    /**
     * Base name of generated files.
     */
    @JvmField
    public val IR_OUTPUT_NAME: CommonJsAndWasmCompilerKlibArgument<String?> =
        CommonJsAndWasmCompilerKlibArgument("IR_OUTPUT_NAME", KotlinReleaseVersion(1, 8, 20))

    /**
     * Specify the name of the compilation module for the IR backend.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_MODULE_NAME: CommonJsAndWasmCompilerKlibArgument<String?> =
        CommonJsAndWasmCompilerKlibArgument("X_IR_MODULE_NAME", KotlinReleaseVersion(1, 4, 0))

    /**
     * Don't pack the library into a klib file.
     */
    @JvmField
    public val NOPACK: CommonJsAndWasmCompilerKlibArgument<Boolean> =
        CommonJsAndWasmCompilerKlibArgument("NOPACK", KotlinReleaseVersion(2, 4, 20))

    /**
     * Generate a packed klib into the directory specified by '-ir-output-dir'.
     *
     * This argument is deprecated. Producing a packed klib is now the default behavior. 
     *
     * The '-nopack' argument can be used instead to determine if a packed klib file will be produced.
     * Setting this argument to something other than `null` overrides the value from '-nopack'.
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

    /**
     * Paths to Kotlin libraries with .meta.js and .kjsm files, separated by the system path separator.
     */
    @JvmField
    public val LIBRARIES: CommonJsAndWasmCompilerKlibArgument<List<Path>?> =
        CommonJsAndWasmCompilerKlibArgument("LIBRARIES", KotlinReleaseVersion(1, 1, 0))

    /**
     * Paths to friend modules.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_FRIEND_MODULES: CommonJsAndWasmCompilerKlibArgument<List<Path>?> =
        CommonJsAndWasmCompilerKlibArgument("X_FRIEND_MODULES", KotlinReleaseVersion(1, 1, 3))

    /**
     * Generate an unpacked klib into the directory specified by '-ir-output-dir'.
     *
     * This argument is deprecated.
     *  
     * The '-nopack' argument should be used to determine if a packed klib file will be produced.
     * Setting this argument to something other than `null` overrides the value from '-nopack'.
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
     * Add a custom output name to the split .js files.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_PER_MODULE_OUTPUT_NAME: CommonJsAndWasmCompilerKlibArgument<String?> =
        CommonJsAndWasmCompilerKlibArgument("X_IR_PER_MODULE_OUTPUT_NAME", KotlinReleaseVersion(1, 5, 30))

    /**
     * Disable internal declaration export.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_FRIEND_MODULES_DISABLED: CommonJsAndWasmCompilerKlibArgument<Boolean> =
        CommonJsAndWasmCompilerKlibArgument("X_FRIEND_MODULES_DISABLED", KotlinReleaseVersion(1, 1, 3))

    /**
     * Enable the IR fake override validator.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_FAKE_OVERRIDE_VALIDATOR: CommonJsAndWasmCompilerKlibArgument<Boolean> =
        CommonJsAndWasmCompilerKlibArgument("X_FAKE_OVERRIDE_VALIDATOR", KotlinReleaseVersion(1, 4, 30))
  }
}
