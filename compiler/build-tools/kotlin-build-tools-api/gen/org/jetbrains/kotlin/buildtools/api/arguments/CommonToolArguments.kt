package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Boolean
import kotlin.String
import kotlin.Suppress
import kotlin.jvm.JvmField

public interface CommonToolArguments {
  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: CommonToolArgument<V>): V

  public operator fun <V> `set`(key: CommonToolArgument<V>, `value`: V)

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
