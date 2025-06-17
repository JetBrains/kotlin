// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.api.`internal`.compat.arguments

import kotlin.Any
import kotlin.Boolean
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments.Companion.NOWARN
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments.Companion.VERBOSE
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments.Companion.VERSION
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments.Companion.WERROR
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments.Companion.WEXTRA
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument

internal open class CommonToolArgumentsImpl : CommonToolArguments {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  override operator fun <V> `get`(key: CommonToolArguments.CommonToolArgument<V>): V = optionsMap[key.id] as V

  override operator fun <V> `set`(key: CommonToolArguments.CommonToolArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: CommonToolArgument<V>): V = optionsMap[key.id] as V

  public operator fun <V> `set`(key: CommonToolArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: CommonToolArgument<*>): Boolean = key.id in optionsMap

  @Suppress("DEPRECATION")
  @OptIn(ExperimentalCompilerArgument::class)
  open override fun toArgumentStrings(): List<String> {
    val arguments = mutableListOf<String>()
    if ("VERSION" in optionsMap) { arguments.add("-version=" + get(VERSION)) }
    if ("VERBOSE" in optionsMap) { arguments.add("-verbose=" + get(VERBOSE)) }
    if ("NOWARN" in optionsMap) { arguments.add("-nowarn=" + get(NOWARN)) }
    if ("WERROR" in optionsMap) { arguments.add("-Werror=" + get(WERROR)) }
    if ("WEXTRA" in optionsMap) { arguments.add("-Wextra=" + get(WEXTRA)) }
    return arguments
  }

  public class CommonToolArgument<V>(
    public val id: String,
  )

  public companion object {
    public val VERSION: CommonToolArgument<Boolean> = CommonToolArgument("VERSION")

    public val VERBOSE: CommonToolArgument<Boolean> = CommonToolArgument("VERBOSE")

    public val NOWARN: CommonToolArgument<Boolean> = CommonToolArgument("NOWARN")

    public val WERROR: CommonToolArgument<Boolean> = CommonToolArgument("WERROR")

    public val WEXTRA: CommonToolArgument<Boolean> = CommonToolArgument("WEXTRA")
  }
}
