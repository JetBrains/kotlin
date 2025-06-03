package org.jetbrains.kotlin.build.tools.api

import kotlin.Any
import kotlin.Boolean
import kotlin.String
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import kotlin.jvm.JvmField

public open class CommonToolArguments {
  private val optionsMap: MutableMap<CommonToolArgument<*>, Any?> = mutableMapOf()

  public operator fun <V> `get`(key: CommonToolArgument<V>): V? = optionsMap[key] as V?

  public operator fun <V> `set`(key: CommonToolArgument<V>, `value`: V) {
    optionsMap[key] = `value`
  }

  public class CommonToolArgument<V>(
    public val id: String,
  )

  public companion object {
    /**
     * Print a synopsis of standard options.
     */
    @JvmField
    public val HELP: CommonToolArgument<Boolean> = CommonToolArgument("HELP")

    /**
     * Print a synopsis of advanced options.
     */
    @JvmField
    public val X: CommonToolArgument<Boolean> = CommonToolArgument("X")

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
