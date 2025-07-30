// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Array
import kotlin.Boolean
import kotlin.String
import kotlin.jvm.JvmField

/**
 * @since 2.3.0
 */
@ExperimentalCompilerArgument
public interface CommonKlibBasedArguments : CommonCompilerArguments {
  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: CommonKlibBasedArgument<V>): V

  /**
   * Set the [value] for option specified by [key], overriding any previous value for that option.
   */
  public operator fun <V> `set`(key: CommonKlibBasedArgument<V>, `value`: V)

  public operator fun contains(key: CommonKlibBasedArgument<*>): Boolean

  /**
   * Base class for [CommonKlibBasedArguments] options.
   *
   * @see get
   * @see set    
   */
  public class CommonKlibBasedArgument<V>(
    public val id: String,
  )

  public companion object {
    /**
     * Provide a base path to compute the source's relative paths in klib (default is empty).
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_KLIB_RELATIVE_PATH_BASE: CommonKlibBasedArgument<Array<String>?> =
        CommonKlibBasedArgument("X_KLIB_RELATIVE_PATH_BASE")

    /**
     * Normalize absolute paths in klibs.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_KLIB_NORMALIZE_ABSOLUTE_PATH: CommonKlibBasedArgument<Boolean> =
        CommonKlibBasedArgument("X_KLIB_NORMALIZE_ABSOLUTE_PATH")

    /**
     * Enable signature uniqueness checks.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS: CommonKlibBasedArgument<Boolean> =
        CommonKlibBasedArgument("X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS")

    /**
     * Use partial linkage mode.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PARTIAL_LINKAGE: CommonKlibBasedArgument<String?> =
        CommonKlibBasedArgument("X_PARTIAL_LINKAGE")

    /**
     * Define the compile-time log level for partial linkage.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PARTIAL_LINKAGE_LOGLEVEL: CommonKlibBasedArgument<String?> =
        CommonKlibBasedArgument("X_PARTIAL_LINKAGE_LOGLEVEL")

    /**
     * Klib dependencies usage strategy when multiple KLIBs has same `unique_name` property value.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY: CommonKlibBasedArgument<String?> =
        CommonKlibBasedArgument("X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY")

    /**
     * Enable experimental support to invoke IR Inliner before Klib serialization.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_KLIB_IR_INLINER: CommonKlibBasedArgument<Boolean> =
        CommonKlibBasedArgument("X_KLIB_IR_INLINER")

    /**
     * Specify the custom ABI version to be written in KLIB. This option is intended only for tests.
     * Warning: This option does not affect KLIB ABI. Neither allows it making a KLIB backward-compatible with older ABI versions.
     * The only observable effect is that a custom ABI version is written to KLIB manifest file.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_KLIB_ABI_VERSION: CommonKlibBasedArgument<String?> =
        CommonKlibBasedArgument("X_KLIB_ABI_VERSION")
  }
}
