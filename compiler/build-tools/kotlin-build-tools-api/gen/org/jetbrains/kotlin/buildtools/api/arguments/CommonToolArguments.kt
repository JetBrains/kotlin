// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Boolean
import kotlin.String
import kotlin.collections.List
import kotlin.jvm.JvmField

/**
 * @since 2.3.0
 */
public interface CommonToolArguments {
  public fun toArgumentStrings(): List<String>

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
   * Base class for [CommonToolArguments] options.
   *
   * @see get
   * @see set    
   */
  public class CommonToolArgument<V>(
    public val id: String,
  )

  public companion object {
    /**
     * Display the compiler version.
     */
    @JvmField
    public val VERSION: CommonToolArgument<Boolean> = CommonToolArgument("VERSION")

    /**
     * Enable verbose logging output.
     */
    @JvmField
    public val VERBOSE: CommonToolArgument<Boolean> = CommonToolArgument("VERBOSE")

    /**
     * Don't generate any warnings.
     */
    @JvmField
    public val NOWARN: CommonToolArgument<Boolean> = CommonToolArgument("NOWARN")

    /**
     * Report an error if there are any warnings.
     */
    @JvmField
    public val WERROR: CommonToolArgument<Boolean> = CommonToolArgument("WERROR")

    /**
     * Enable extra checkers for K2.
     */
    @JvmField
    public val WEXTRA: CommonToolArgument<Boolean> = CommonToolArgument("WEXTRA")
  }
}
