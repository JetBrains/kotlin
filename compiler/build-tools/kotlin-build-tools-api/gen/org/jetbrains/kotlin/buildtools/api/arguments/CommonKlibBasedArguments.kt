// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Int
import kotlin.String
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.DuplicatedUniqueNameStrategy

/**
 * @since 2.4.20
 */
public interface CommonKlibBasedArguments : CommonCompilerArguments {
  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: CommonKlibBasedArgument<V>): V

  /**
   * An option for configuring [CommonKlibBasedArguments].
   *
   * @see get
   * @see set    
   */
  public class CommonKlibBasedArgument<V>(
    public val id: String,
    public val availableSinceVersion: KotlinReleaseVersion,
  )

  /**
   * A builder for [CommonKlibBasedArguments].
   */
  public interface Builder : CommonCompilerArguments.Builder {
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
  }

  public companion object {
    /**
     * Klib dependencies usage strategy when multiple KLIBs has same `unique_name` property value.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY:
        CommonKlibBasedArgument<DuplicatedUniqueNameStrategy?> =
        CommonKlibBasedArgument("X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY", KotlinReleaseVersion(2, 1, 0))

    /**
     * Maximum number of klibs that can be cached during compilation. Default is 64.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT: CommonKlibBasedArgument<Int> =
        CommonKlibBasedArgument("X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT", KotlinReleaseVersion(2, 3, 0))
  }
}
