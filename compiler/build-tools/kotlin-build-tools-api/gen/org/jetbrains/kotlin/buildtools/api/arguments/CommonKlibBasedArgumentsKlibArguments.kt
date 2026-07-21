// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import java.nio.`file`.Path
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.RemovedCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KlibIrInlinerMode

/**
 * @since 2.4.20
 */
public interface CommonKlibBasedArgumentsKlibArguments : CommonKlibBasedArguments {
  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: CommonKlibBasedArgumentsKlibArgument<V>): V

  /**
   * An option for configuring [CommonKlibBasedArgumentsKlibArguments].
   *
   * @see get
   * @see set    
   */
  public class CommonKlibBasedArgumentsKlibArgument<V>(
    public val id: String,
    public val availableSinceVersion: KotlinReleaseVersion,
  )

  /**
   * A builder for [CommonKlibBasedArgumentsKlibArguments].
   */
  public interface Builder : CommonKlibBasedArguments.Builder {
    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> `get`(key: CommonKlibBasedArgumentsKlibArgument<V>): V

    /**
     * Set the [value] for option specified by [key], overriding any previous value for that option.
     */
    public operator fun <V> `set`(key: CommonKlibBasedArgumentsKlibArgument<V>, `value`: V)

    /**
     * Constructs a new immutable [CommonKlibBasedArgumentsKlibArguments] instance with the options set in this builder.
     *
     * @since 2.4.20
     */
    override fun build(): CommonKlibBasedArgumentsKlibArguments
  }

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
    public val X_KLIB_ABI_VERSION: CommonKlibBasedArgumentsKlibArgument<String?> =
        CommonKlibBasedArgumentsKlibArgument("X_KLIB_ABI_VERSION", KotlinReleaseVersion(2, 2, 0))

    /**
     * Enable signature uniqueness checks.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS: CommonKlibBasedArgumentsKlibArgument<Boolean> =
        CommonKlibBasedArgumentsKlibArgument("X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS", KotlinReleaseVersion(2, 0, 20))

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
    public val X_KLIB_IR_INLINER: CommonKlibBasedArgumentsKlibArgument<KlibIrInlinerMode> =
        CommonKlibBasedArgumentsKlibArgument("X_KLIB_IR_INLINER", KotlinReleaseVersion(2, 1, 20))

    /**
     * Normalize absolute paths in klibs.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     *
     * Deprecated in Kotlin version 2.4.20.
     *
     * Removed in Kotlin version 2.5.0.
     */
    @JvmField
    @ExperimentalCompilerArgument
    @RemovedCompilerArgument
    public val X_KLIB_NORMALIZE_ABSOLUTE_PATH: CommonKlibBasedArgumentsKlibArgument<Boolean> =
        CommonKlibBasedArgumentsKlibArgument("X_KLIB_NORMALIZE_ABSOLUTE_PATH", KotlinReleaseVersion(2, 0, 20))

    /**
     * Relativize all the paths stored in a klib using the given path prefixes.
     * The supplied prefixes should be absolute paths to the directories containing the source code files.
     * Note: The prefixes are applied in the same order as they are passed in this CLI argument.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_KLIB_RELATIVE_PATH_BASE: CommonKlibBasedArgumentsKlibArgument<List<Path>> =
        CommonKlibBasedArgumentsKlibArgument("X_KLIB_RELATIVE_PATH_BASE", KotlinReleaseVersion(2, 0, 20))

    /**
     * Skip library compatibility checks for stdlib and kotlin.test library.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SKIP_LIBRARY_SPECIAL_COMPATIBILITY_CHECKS:
        CommonKlibBasedArgumentsKlibArgument<Boolean> =
        CommonKlibBasedArgumentsKlibArgument("X_SKIP_LIBRARY_SPECIAL_COMPATIBILITY_CHECKS", KotlinReleaseVersion(2, 4, 0))
  }
}
