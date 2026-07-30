// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.ReplaceWith
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
    level = DeprecationLevel.ERROR,
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
      level = DeprecationLevel.ERROR,
    )
    public operator fun contains(key: CommonToolArgument<*>): Boolean

    /**
     * Constructs a new immutable [CommonToolArguments] instance with the options set in this builder.
     *
     * @since 2.4.20
     */
    public fun build(): CommonToolArguments

    /**
     * Deprecated. Use applyCommandLineArguments instead. This will become an error in Kotlin 2.6.0, and will be removed in 2.7.0.
     *
     * This method is unsafe to use - it wipes all options previously set on this instance to defaults before applying the passed [arguments].
     *
     * Takes a list of string arguments in the format recognized by the Kotlin CLI compiler and applies the options parsed from them into this instance.
     *
     * When compiling with Kotlin compiler 2.4.20 and above, parsing errors are collected on this instance and reported as compilation errors when the compilation is executed.
     * @throws org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException when compiling with Kotlin compiler below 2.4.20 and the `arguments` contain errors and cannot be parsed
     *
     * @param arguments a list of arguments for the Kotlin CLI compiler
     */
    @Deprecated(
      message = "This method is deprecated. Use applyCommandLineArguments instead. This will become an error in Kotlin 2.6.0, and will be removed in 2.7.0.",
      level = DeprecationLevel.WARNING,
      replaceWith = ReplaceWith("applyCommandLineArguments(arguments)"),
    )
    public fun applyArgumentStrings(arguments: List<String>)

    /**
     * Takes a list of string arguments in the format recognized by the Kotlin CLI compiler and applies the options parsed from them into this instance.
     *
     * In general, using this method should be avoided if possible, and the type-safe [set] method should be used instead. It is provided 
     * only to make migration from previous non-BTA integrations to BTA easier. 
     *
     * Please note that after calling this method, some BTA type-safe
     * synthetic arguments might be lost (though their values will still be accessible through string arguments). 
     * Currently, [CommonCompilerArguments.COMPILER_PLUGINS] is one such example where its contents may be translated to related string arguments (-P, -Xplugin and
     *  -Xcompiler-plugin-order) and the original `COMPILER_PLUGINS` value will be cleared.
     *
     * When compiling with Kotlin compiler 2.4.20 and above, parsing errors are collected on this instance and reported as compilation errors when the compilation is executed.
     *
     * Even though this method was introduced in Build Tools API 2.5.0, it's usable when compiling with all supported Kotlin compiler versions.
     *
     * @throws org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException when compiling with Kotlin compiler below 2.4.20 and the `arguments` contain errors and cannot be parsed
     * @since 2.5.0
     *
     * @param arguments a list of arguments for the Kotlin CLI compiler
     */
    public fun applyCommandLineArguments(arguments: List<String>)
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
