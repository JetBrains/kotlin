// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.compat.arguments

import java.lang.IllegalStateException
import kotlin.Any
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlinx.serialization.SerialName
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments as ArgumentsCommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments as CommonToolArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal abstract class CommonToolArgumentsImpl() : ArgumentsCommonToolArguments,
    ArgumentsCommonToolArguments.Builder {
  protected val internalArguments: MutableSet<String> = mutableSetOf()

  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @SerialName("WERROR")
  protected var Werror: Boolean = defaultArguments.allWarningsAsErrors

  @SerialName("WEXTRA")
  protected var Wextra: Boolean = defaultArguments.extraWarnings

  @SerialName("X")
  protected var X: Boolean = defaultArguments.extraHelp

  @SerialName("HELP")
  protected var help: Boolean = defaultArguments.help

  @SerialName("NOWARN")
  protected var nowarn: Boolean = defaultArguments.suppressWarnings

  @SerialName("VERBOSE")
  protected var verbose: Boolean = defaultArguments.verbose

  @SerialName("VERSION")
  protected var version: Boolean = defaultArguments.version

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: CommonToolArgument<V>): V = optionsMap[key.id] as V

  public operator fun <V> `set`(key: CommonToolArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: CommonToolArgument<*>): Boolean = key.id in optionsMap

  private operator fun `get`(key: String): Any? = CommonToolArgumentValueAdapter.toApi(optionsMap[key])

  private operator fun `set`(key: String, `value`: Any?) {
    optionsMap[key] = CommonToolArgumentValueAdapter.toImpl(`value`)
  }

  @Suppress("UNCHECKED_CAST")
  override operator fun <V> `get`(key: ArgumentsCommonToolArguments.CommonToolArgument<V>): V {
    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
    return this[key.id] as V
  }

  override operator fun <V> `set`(key: ArgumentsCommonToolArguments.CommonToolArgument<V>, `value`: V) {
    val currentKotlinVersion = KotlinToolingVersion(KC_VERSION)
    if (key.availableSinceVersion > KotlinReleaseVersion(currentKotlinVersion.major, currentKotlinVersion.minor, currentKotlinVersion.patch)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    this[key.id] = `value`
  }

  @Deprecated(
    message = "This method is no longer useful when compiling with Kotlin compiler 2.3.20 and above, as the arguments instance now contains default values for all arguments.",
    level = DeprecationLevel.ERROR,
  )
  override operator fun contains(key: ArgumentsCommonToolArguments.CommonToolArgument<*>): Boolean = key.id in optionsMap

  abstract override fun build(): CommonToolArgumentsImpl

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: CommonToolArguments): CommonToolArguments {
    val unknownArgs = optionsMap.keys.filter { it !in knownArguments }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    arguments.allWarningsAsErrors = Werror
    try { arguments.extraWarnings = Wextra } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: WEXTRA. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.0""").initCause(e) }
    arguments.extraHelp = X
    arguments.help = help
    arguments.suppressWarnings = nowarn
    arguments.verbose = verbose
    arguments.version = version
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: CommonToolArguments) {
    try { Werror = arguments.allWarningsAsErrors } catch (_: NoSuchMethodError) {  }
    try { Wextra = arguments.extraWarnings } catch (_: NoSuchMethodError) {  }
    try { X = arguments.extraHelp } catch (_: NoSuchMethodError) {  }
    try { help = arguments.help } catch (_: NoSuchMethodError) {  }
    try { nowarn = arguments.suppressWarnings } catch (_: NoSuchMethodError) {  }
    try { verbose = arguments.verbose } catch (_: NoSuchMethodError) {  }
    try { version = arguments.version } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  public class CommonToolArgument<V>(
    public val id: String,
  ) {
    init {
      knownArguments.add(id)}
  }

  public companion object {
    private val knownArguments: MutableSet<String> = mutableSetOf()

    public val WERROR: CommonToolArgument<Boolean> = CommonToolArgument("WERROR")

    public val WEXTRA: CommonToolArgument<Boolean> = CommonToolArgument("WEXTRA")

    public val X: CommonToolArgument<Boolean> = CommonToolArgument("X")

    public val HELP: CommonToolArgument<Boolean> = CommonToolArgument("HELP")

    public val NOWARN: CommonToolArgument<Boolean> = CommonToolArgument("NOWARN")

    public val VERBOSE: CommonToolArgument<Boolean> = CommonToolArgument("VERBOSE")

    public val VERSION: CommonToolArgument<Boolean> = CommonToolArgument("VERSION")
  }
}
