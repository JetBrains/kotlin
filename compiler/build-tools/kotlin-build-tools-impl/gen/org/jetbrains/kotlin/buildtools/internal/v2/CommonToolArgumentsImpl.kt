package org.jetbrains.kotlin.buildtools.`internal`.v2

import kotlin.Any
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import org.jetbrains.kotlin.buildtools.api.v2.CommonToolArguments.Companion.HELP
import org.jetbrains.kotlin.buildtools.api.v2.CommonToolArguments.Companion.NOWARN
import org.jetbrains.kotlin.buildtools.api.v2.CommonToolArguments.Companion.VERBOSE
import org.jetbrains.kotlin.buildtools.api.v2.CommonToolArguments.Companion.VERSION
import org.jetbrains.kotlin.buildtools.api.v2.CommonToolArguments.Companion.WERROR
import org.jetbrains.kotlin.buildtools.api.v2.CommonToolArguments.Companion.WEXTRA
import org.jetbrains.kotlin.buildtools.api.v2.CommonToolArguments.Companion.X
import org.jetbrains.kotlin.buildtools.api.v2.CommonToolArguments as V2CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments as CommonToolArguments

public open class CommonToolArgumentsImpl : V2CommonToolArguments {
  private val optionsMap: MutableMap<V2CommonToolArguments.CommonToolArgument<*>, Any?> =
      mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  override operator fun <V> `get`(key: V2CommonToolArguments.CommonToolArgument<V>): V = optionsMap[key] as V

  override operator fun <V> `set`(key: V2CommonToolArguments.CommonToolArgument<V>, `value`: V) {
    optionsMap[key] = `value`
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: CommonToolArguments): CommonToolArguments {
    if (HELP in optionsMap) { arguments.help = get(HELP) }
    if (X in optionsMap) { arguments.extraHelp = get(X) }
    if (VERSION in optionsMap) { arguments.version = get(VERSION) }
    if (VERBOSE in optionsMap) { arguments.verbose = get(VERBOSE) }
    if (NOWARN in optionsMap) { arguments.suppressWarnings = get(NOWARN) }
    if (WERROR in optionsMap) { arguments.allWarningsAsErrors = get(WERROR) }
    if (WEXTRA in optionsMap) { arguments.extraWarnings = get(WEXTRA) }
    return arguments
  }
}
