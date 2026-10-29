// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:UseSerializers(PathAsStringSerializer::class)
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
import kotlin.collections.listOf
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments as ArgumentsCommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments as CommonToolArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

@Serializable
internal abstract class CommonToolArgumentsImpl() : ArgumentsCommonToolArguments,
    ArgumentsCommonToolArguments.Builder {
  protected val internalArguments: MutableSet<String> = mutableSetOf()

  @Transient
  private val defaultArguments: CommonToolArguments = createDefaultArguments()

  @Transient
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @SerialName("WERROR")
  internal var allWarningsAsErrors: Boolean = defaultArguments.allWarningsAsErrors

  @SerialName("WEXTRA")
  internal var extraWarnings: Boolean = defaultArguments.extraWarnings

  @SerialName("X")
  internal var extraHelp: Boolean = defaultArguments.extraHelp

  @SerialName("HELP")
  internal var help: Boolean = defaultArguments.help

  @SerialName("NOWARN")
  internal var suppressWarnings: Boolean = defaultArguments.suppressWarnings

  @SerialName("VERBOSE")
  internal var verbose: Boolean = defaultArguments.verbose

  @SerialName("VERSION")
  internal var version: Boolean = defaultArguments.version

  @Transient
  protected val _restrictedArgViolations: MutableList<RestrictedArgViolation> = mutableListOf()

  internal val restrictedArgViolations: List<RestrictedArgViolation>
    get() = _restrictedArgViolations

  protected val _argumentValidationErrors: MutableSet<String> = mutableSetOf()

  internal val argumentValidationErrors: Set<String>
    get() = _argumentValidationErrors

  @Transient
  internal val argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics()

  public constructor(
    argumentValidationErrors: Set<String> = emptySet(),
    restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
    argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
  ) : this() {
    _argumentValidationErrors += argumentValidationErrors
    _restrictedArgViolations += restrictedArgViolations
    this.argumentParseDiagnostics += argumentParseDiagnostics
  }

  public abstract fun createDefaultArguments(): CommonToolArguments

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: ArgumentsCommonToolArguments.CommonToolArgument<V>): V = CommonToolArgumentValueAdapter.toApi(when (key.id) {
    "WERROR" -> allWarningsAsErrors
    "WEXTRA" -> extraWarnings
    "X" -> extraHelp
    "HELP" -> help
    "NOWARN" -> suppressWarnings
    "VERBOSE" -> verbose
    "VERSION" -> version
    else -> {
            check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
            optionsMap[key.id]
        }
  }
  )

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: ArgumentsCommonToolArguments.CommonToolArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    when (key.id) {
      "WERROR" -> allWarningsAsErrors = CommonToolArgumentValueAdapter.toImpl(value)
      "WEXTRA" -> extraWarnings = CommonToolArgumentValueAdapter.toImpl(value)
      "X" -> extraHelp = CommonToolArgumentValueAdapter.toImpl(value)
      "HELP" -> help = CommonToolArgumentValueAdapter.toImpl(value)
      "NOWARN" -> suppressWarnings = CommonToolArgumentValueAdapter.toImpl(value)
      "VERBOSE" -> verbose = CommonToolArgumentValueAdapter.toImpl(value)
      "VERSION" -> version = CommonToolArgumentValueAdapter.toImpl(value)
      else -> {
              optionsMap[key.id] = CommonToolArgumentValueAdapter.toImpl(value)
          }
    }
  }

  @Deprecated(
    message = "This method is no longer useful when compiling with Kotlin compiler 2.3.20 and above, as the arguments instance now contains default values for all arguments.",
    level = DeprecationLevel.ERROR,
  )
  override operator fun contains(key: ArgumentsCommonToolArguments.CommonToolArgument<*>): Boolean = key.id in knownOptionIds

  abstract override fun build(): CommonToolArgumentsImpl

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: CommonToolArguments): CommonToolArguments {
    val unknownArgs = optionsMap.keys
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    arguments.allWarningsAsErrors = allWarningsAsErrors
    arguments.extraWarnings = extraWarnings
    arguments.extraHelp = extraHelp
    arguments.help = help
    arguments.suppressWarnings = suppressWarnings
    arguments.verbose = verbose
    arguments.version = version
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: CommonToolArguments) {
    allWarningsAsErrors = arguments.allWarningsAsErrors
    extraWarnings = arguments.extraWarnings
    extraHelp = arguments.extraHelp
    help = arguments.help
    suppressWarnings = arguments.suppressWarnings
    verbose = arguments.verbose
    version = arguments.version
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: CommonToolArguments): CommonToolArguments {
    arguments.allWarningsAsErrors = allWarningsAsErrors
    arguments.extraWarnings = extraWarnings
    return arguments
  }

  internal open fun collectRestrictedArgViolations(compilerArgs: CommonToolArguments, defaultArgs: CommonToolArguments) {
    _restrictedArgViolations.clear()
  }

  public companion object {
    private val knownOptionIds: List<String> =
        listOf("WERROR","WEXTRA","X","HELP","NOWARN","VERBOSE","VERSION",)
  }
}
