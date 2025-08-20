// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import kotlin.Any
import kotlin.Boolean
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonToolArgumentsImpl.Companion.HELP
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonToolArgumentsImpl.Companion.NOWARN
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonToolArgumentsImpl.Companion.VERBOSE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonToolArgumentsImpl.Companion.VERSION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonToolArgumentsImpl.Companion.WERROR
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonToolArgumentsImpl.Companion.WEXTRA
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonToolArgumentsImpl.Companion.X
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments as ArgumentsCommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments as CommonToolArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal abstract class CommonToolArgumentsImpl : ArgumentsCommonToolArguments {
  protected val internalArguments: MutableSet<String> = mutableSetOf()

  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: ArgumentsCommonToolArguments.CommonToolArgument<V>): V = optionsMap[key.id] as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: ArgumentsCommonToolArguments.CommonToolArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  override operator fun contains(key: ArgumentsCommonToolArguments.CommonToolArgument<*>): Boolean = key.id in optionsMap

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: CommonToolArgument<V>): V = optionsMap[key.id] as V

  public operator fun <V> `set`(key: CommonToolArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: CommonToolArgument<*>): Boolean = key.id in optionsMap

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: CommonToolArguments): CommonToolArguments {
    if (HELP in this) { arguments.help = get(HELP)}
    if (X in this) { arguments.extraHelp = get(X)}
    if (VERSION in this) { arguments.version = get(VERSION)}
    if (VERBOSE in this) { arguments.verbose = get(VERBOSE)}
    if (NOWARN in this) { arguments.suppressWarnings = get(NOWARN)}
    if (WERROR in this) { arguments.allWarningsAsErrors = get(WERROR)}
    try { if (WEXTRA in this) { arguments.extraWarnings = get(WEXTRA)} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: WEXTRA. Current compiler version is: $KC_VERSION}, but argument was introduced in 2.1.0""").initCause(e) }
    return arguments
  }

  @Suppress("DEPRECATION")
  public fun applyCompilerArguments(arguments: CommonToolArguments) {
    try { this[HELP] = arguments.help } catch (_: NoSuchMethodError) {  }
    try { this[X] = arguments.extraHelp } catch (_: NoSuchMethodError) {  }
    try { this[VERSION] = arguments.version } catch (_: NoSuchMethodError) {  }
    try { this[VERBOSE] = arguments.verbose } catch (_: NoSuchMethodError) {  }
    try { this[NOWARN] = arguments.suppressWarnings } catch (_: NoSuchMethodError) {  }
    try { this[WERROR] = arguments.allWarningsAsErrors } catch (_: NoSuchMethodError) {  }
    try { this[WEXTRA] = arguments.extraWarnings } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  public class CommonToolArgument<V>(
    public val id: String,
  )

  public companion object {
    public val HELP: CommonToolArgument<Boolean> = CommonToolArgument("HELP")

    public val X: CommonToolArgument<Boolean> = CommonToolArgument("X")

    public val VERSION: CommonToolArgument<Boolean> = CommonToolArgument("VERSION")

    public val VERBOSE: CommonToolArgument<Boolean> = CommonToolArgument("VERBOSE")

    public val NOWARN: CommonToolArgument<Boolean> = CommonToolArgument("NOWARN")

    public val WERROR: CommonToolArgument<Boolean> = CommonToolArgument("WERROR")

    public val WEXTRA: CommonToolArgument<Boolean> = CommonToolArgument("WEXTRA")
  }
}
