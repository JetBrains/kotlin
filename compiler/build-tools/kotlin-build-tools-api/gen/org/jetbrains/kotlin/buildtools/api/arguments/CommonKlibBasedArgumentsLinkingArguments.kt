// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Int
import kotlin.String
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.DeprecatedCompilerArgument
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.buildtools.api.arguments.enums.PartialLinkageLogLevel
import org.jetbrains.kotlin.buildtools.api.arguments.enums.PartialLinkageMode

/**
 * @since 2.4.20
 */
public interface CommonKlibBasedArgumentsLinkingArguments : CommonKlibBasedArguments {
  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: CommonKlibBasedArgumentsLinkingArgument<V>): V

  /**
   * An option for configuring [CommonKlibBasedArgumentsLinkingArguments].
   *
   * @see get
   * @see set    
   */
  public class CommonKlibBasedArgumentsLinkingArgument<V>(
    public val id: String,
    public val availableSinceVersion: KotlinReleaseVersion,
  )

  /**
   * A builder for [CommonKlibBasedArgumentsLinkingArguments].
   */
  public interface Builder : CommonKlibBasedArguments.Builder {
    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> `get`(key: CommonKlibBasedArgumentsLinkingArgument<V>): V

    /**
     * Set the [value] for option specified by [key], overriding any previous value for that option.
     */
    public operator fun <V> `set`(key: CommonKlibBasedArgumentsLinkingArgument<V>, `value`: V)
  }

  public companion object {
    /**
     * This option is deprecated and will be deleted in future versions.
     * The partial linkage engine is always turned on.
     * If you would like to adjust the compile-time log level for partial linkage, use -Xpartial-linkage-loglevel.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     *
     * Deprecated in Kotlin version 2.4.0.
     */
    @JvmField
    @ExperimentalCompilerArgument
    @DeprecatedCompilerArgument
    public val X_PARTIAL_LINKAGE: CommonKlibBasedArgumentsLinkingArgument<PartialLinkageMode?> =
        CommonKlibBasedArgumentsLinkingArgument("X_PARTIAL_LINKAGE", KotlinReleaseVersion(2, 0, 20))

    /**
     * Define the compile-time log level for partial linkage.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PARTIAL_LINKAGE_LOGLEVEL:
        CommonKlibBasedArgumentsLinkingArgument<PartialLinkageLogLevel?> =
        CommonKlibBasedArgumentsLinkingArgument("X_PARTIAL_LINKAGE_LOGLEVEL", KotlinReleaseVersion(2, 0, 20))

    /**
     * Klib dependencies usage strategy when multiple KLIBs has same `unique_name` property value.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY:
        CommonKlibBasedArgumentsLinkingArgument<DuplicatedUniqueNameStrategy?> =
        CommonKlibBasedArgumentsLinkingArgument("X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY", KotlinReleaseVersion(2, 1, 0))

    /**
     * Maximum number of klibs that can be cached during compilation. Default is 64.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT: CommonKlibBasedArgumentsLinkingArgument<Int> =
        CommonKlibBasedArgumentsLinkingArgument("X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT", KotlinReleaseVersion(2, 3, 0))
  }
}
