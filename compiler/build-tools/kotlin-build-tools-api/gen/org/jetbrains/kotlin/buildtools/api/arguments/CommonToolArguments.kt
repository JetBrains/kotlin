// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.String
import kotlin.collections.List
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion

/**
 * @since 2.3.0
 */
public interface CommonToolArguments {
  /**
   * Converts the options to a list of string arguments recognized by the Kotlin CLI compiler.
   */
  public fun toArgumentStrings(): List<String>

  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: CommonToolArgument<V>): V

  /**
   * Check if an option specified by [key] has a value set.
   *
   * Note: trying to read an option (by using [get]) that has not been set will result in an exception.
   *
   * @return true if the option has a value set, false otherwise
   */
  @Deprecated(
    message = "This method is no longer useful when compiling with Kotlin compiler 2.3.20 and above, as the arguments instance now contains default values for all arguments.",
    level = DeprecationLevel.WARNING,
  )
  public operator fun contains(key: CommonToolArgument<*>): Boolean

  /**
   * An option for configuring [CommonToolArguments].
   *
   * @see get
   * @see set    
   */
  public class CommonToolArgument<V>(
    public val id: String,
    public val availableSinceVersion: KotlinReleaseVersion,
  )

  /**
   * A builder for [CommonToolArguments].
   *
   * @since 2.3.20
   */
  public interface Builder {
    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> `get`(key: CommonToolArgument<V>): V

    /**
     * Set the [value] for option specified by [key], overriding any previous value for that option.
     */
    public operator fun <V> `set`(key: CommonToolArgument<V>, `value`: V)

    /**
     * Check if an option specified by [key] has a value set.
     *
     * Note: trying to read an option (by using [get]) that has not been set will result in an exception.
     *
     * @return true if the option has a value set, false otherwise
     */
    @Deprecated(
      message = "This method is no longer useful when compiling with Kotlin compiler 2.3.20 and above, as the arguments instance now contains default values for all arguments.",
      level = DeprecationLevel.WARNING,
    )
    public operator fun contains(key: CommonToolArgument<*>): Boolean

    /**
     * Constructs a new immutable [CommonToolArguments] instance with the options set in this builder.
     *
     * @since 2.4.20
     */
    public fun build(): CommonToolArguments

    /**
     * Takes a list of string arguments in the format recognized by the Kotlin CLI compiler and applies the options parsed from them into this instance.
     *
     * @throws org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException when the `arguments` contain errors and cannot be parsed
     *
     * @param arguments a list of arguments for the Kotlin CLI compiler
     */
    public fun applyArgumentStrings(arguments: List<String>)
  }

  public companion object {
    /**
     * Report an error if there are any warnings.
     */
    @JvmField
    public val WERROR: CommonToolArgument<Boolean> =
        CommonToolArgument("WERROR", KotlinReleaseVersion(1, 2, 0))

    /**
     * Enable extra checkers for K2.
     */
    @JvmField
    public val WEXTRA: CommonToolArgument<Boolean> =
        CommonToolArgument("WEXTRA", KotlinReleaseVersion(2, 1, 0))

    /**
     * Don't generate any warnings.
     */
    @JvmField
    public val NOWARN: CommonToolArgument<Boolean> =
        CommonToolArgument("NOWARN", KotlinReleaseVersion(1, 0, 0))

    /**
     * Enable verbose logging output.
     */
    @JvmField
    public val VERBOSE: CommonToolArgument<Boolean> =
        CommonToolArgument("VERBOSE", KotlinReleaseVersion(1, 0, 0))

    /**
     * Display the compiler version.
     */
    @JvmField
    public val VERSION: CommonToolArgument<Boolean> =
        CommonToolArgument("VERSION", KotlinReleaseVersion(1, 0, 0))
  }
}
