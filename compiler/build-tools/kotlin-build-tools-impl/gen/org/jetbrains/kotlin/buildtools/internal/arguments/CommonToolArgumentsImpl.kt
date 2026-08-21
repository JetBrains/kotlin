// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import java.lang.IllegalStateException
import kotlin.Any
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.mutableSetOf
import kotlin.collections.toMutableList
import kotlin.collections.toMutableSet
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments as ArgumentsCommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments as CommonToolArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal abstract class CommonToolArgumentsImpl(
  protected open val compilerArguments: CommonToolArguments,
  protected open val optionsMap: MutableMap<String, Any?>,
  argumentValidationErrors: Set<String> = emptySet(),
  restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
  internal val argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
) : ArgumentsCommonToolArguments,
    ArgumentsCommonToolArguments.Builder {
  protected val _restrictedArgViolations: MutableList<RestrictedArgViolation> =
      restrictedArgViolations.toMutableList()

  internal val restrictedArgViolations: List<RestrictedArgViolation>
    get() = _restrictedArgViolations

  protected val _argumentValidationErrors: MutableSet<String> =
      argumentValidationErrors.toMutableSet()

  internal val argumentValidationErrors: Set<String>
    get() = _argumentValidationErrors

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: CommonToolArgument<V>): V = getOption(key.id) as V

  private operator fun <V> `set`(key: CommonToolArgument<V>, `value`: V) {
    setOption(key.id, value)
  }

  public operator fun contains(key: CommonToolArgument<*>): Boolean = isArgumentKnown(key.id) 

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: ArgumentsCommonToolArguments.CommonToolArgument<V>): V = getOption(key.id) as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: ArgumentsCommonToolArguments.CommonToolArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    setOption(key.id, value)
  }

  @Deprecated(
    message = "This method is no longer useful when compiling with Kotlin compiler 2.3.20 and above, as the arguments instance now contains default values for all arguments.",
    level = DeprecationLevel.ERROR,
  )
  override operator fun contains(key: ArgumentsCommonToolArguments.CommonToolArgument<*>): Boolean = key.id in optionsMap

  @Suppress(
    "UNCHECKED_CAST",
    "DEPRECATION",
  )
  private fun getOption(keyId: String): Any? = when (keyId) {
    "WERROR" -> {
    this.compilerArguments.allWarningsAsErrors
    }
    "WEXTRA" -> {
    this.compilerArguments.extraWarnings
    }
    "X" -> {
    this.compilerArguments.extraHelp
    }
    "HELP" -> {
    this.compilerArguments.help
    }
    "NOWARN" -> {
    this.compilerArguments.suppressWarnings
    }
    "VERBOSE" -> {
    this.compilerArguments.verbose
    }
    "VERSION" -> {
    this.compilerArguments.version
    }
    else -> {
      check(keyId in optionsMap) { "Argument ${keyId} is not set and has no default value" }
      optionsMap[keyId]
    }
  }

  @Suppress(
    "UNCHECKED_CAST",
    "DEPRECATION",
  )
  private fun setOption(keyId: String, `value`: Any?) {
    when (keyId) {
      "WERROR" -> {
      this.compilerArguments.allWarningsAsErrors = (value as Boolean)
      }
      "WEXTRA" -> {
      this.compilerArguments.extraWarnings = (value as Boolean)
      }
      "X" -> {
      this.compilerArguments.extraHelp = (value as Boolean)
      }
      "HELP" -> {
      this.compilerArguments.help = (value as Boolean)
      }
      "NOWARN" -> {
      this.compilerArguments.suppressWarnings = (value as Boolean)
      }
      "VERBOSE" -> {
      this.compilerArguments.verbose = (value as Boolean)
      }
      "VERSION" -> {
      this.compilerArguments.version = (value as Boolean)
      }
      else -> optionsMap[keyId] = value
    }
  }

  abstract override fun build(): CommonToolArgumentsImpl

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: CommonToolArguments): CommonToolArguments = arguments

  protected fun applyCompilerArguments(arguments: CommonToolArguments) {
  }

  protected open fun isArgumentKnown(name: String): Boolean = name in knownArguments

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: CommonToolArguments): CommonToolArguments {
    arguments.allWarningsAsErrors = this.compilerArguments.allWarningsAsErrors
    arguments.extraWarnings = this.compilerArguments.extraWarnings
    return arguments
  }

  internal open fun collectRestrictedArgViolations(compilerArgs: CommonToolArguments, defaultArgs: CommonToolArguments) {
    _restrictedArgViolations.clear()
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
