// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Array
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KlibIrInlinerMode

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

  /**
   * Check if an option specified by [key] has a value set.
   *
   * Note: trying to read an option (by using [get]) that has not been set will result in an exception.
   *
   * @return true if the option has a value set, false otherwise
   */
  public operator fun contains(key: CommonKlibBasedArgument<*>): Boolean

  /**
   * Base class for [CommonKlibBasedArguments] options.
   *
   * @see get
   * @see set    
   */
  public class CommonKlibBasedArgument<V>(
    public val id: String,
    public val availableSinceVersion: KotlinReleaseVersion,
  )

  public companion object {
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
        CommonKlibBasedArgument("X_KLIB_ABI_VERSION", KotlinReleaseVersion(2, 2, 0))

    /**
     * Klib dependencies usage strategy when multiple KLIBs has same `unique_name` property value.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY: CommonKlibBasedArgument<String?> =
        CommonKlibBasedArgument("X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY", KotlinReleaseVersion(2, 1, 0))

    /**
     * Enable signature uniqueness checks.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS: CommonKlibBasedArgument<Boolean> =
        CommonKlibBasedArgument("X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS", KotlinReleaseVersion(2, 0, 20))

    /**
     * Set the mode of the experimental IR inliner on the first compilation stage.
     * - `intra-module` mode enforces inlining of the functions only from the compiled module
     * - `full` mode enforces inlining of all functions (from the compiled module and from all dependencies)
     *    Warning: This mode will trigger setting the `pre-release` flag for the compiled library.
     * - `disabled` mode completely disables the IR inliner
     * - `default` mode lets the IR inliner run in `intra-module`, `full` or `disabled` mode based on the current language version
     *         
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_KLIB_IR_INLINER: CommonKlibBasedArgument<KlibIrInlinerMode> =
        CommonKlibBasedArgument("X_KLIB_IR_INLINER", KotlinReleaseVersion(2, 1, 20))

    /**
     * Normalize absolute paths in klibs.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_KLIB_NORMALIZE_ABSOLUTE_PATH: CommonKlibBasedArgument<Boolean> =
        CommonKlibBasedArgument("X_KLIB_NORMALIZE_ABSOLUTE_PATH", KotlinReleaseVersion(2, 0, 20))

    /**
     * Provide a base path to compute the source's relative paths in klib (default is empty).
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_KLIB_RELATIVE_PATH_BASE: CommonKlibBasedArgument<Array<String>?> =
        CommonKlibBasedArgument("X_KLIB_RELATIVE_PATH_BASE", KotlinReleaseVersion(2, 0, 20))

    /**
     * Maximum number of klibs that can be cached during compilation. Default is 64.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT: CommonKlibBasedArgument<Int> =
        CommonKlibBasedArgument("X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT", KotlinReleaseVersion(2, 3, 0))

    /**
     * Use partial linkage mode.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PARTIAL_LINKAGE: CommonKlibBasedArgument<String?> =
        CommonKlibBasedArgument("X_PARTIAL_LINKAGE", KotlinReleaseVersion(2, 0, 20))

    /**
     * Define the compile-time log level for partial linkage.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PARTIAL_LINKAGE_LOGLEVEL: CommonKlibBasedArgument<String?> =
        CommonKlibBasedArgument("X_PARTIAL_LINKAGE_LOGLEVEL", KotlinReleaseVersion(2, 0, 20))
  }
}
