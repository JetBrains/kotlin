// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.String
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.DeprecatedCompilerArgument
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
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

    /**
     * Constructs a new immutable [CommonKlibBasedArgumentsLinkingArguments] instance with the options set in this builder.
     *
     * @since 2.4.20
     */
    override fun build(): CommonKlibBasedArgumentsLinkingArguments
  }

  public companion object {
    /**
     * Enables partial linkage mode.
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
  }
}
