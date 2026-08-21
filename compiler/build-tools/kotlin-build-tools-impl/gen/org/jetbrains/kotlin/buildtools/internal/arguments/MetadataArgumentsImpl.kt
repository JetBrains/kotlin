// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import java.io.File
import java.lang.IllegalStateException
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.toTypedArray
import kotlin.io.path.Path
import kotlin.text.split
import org.jetbrains.kotlin.buildtools.`internal`.DeepCopyable
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments
import org.jetbrains.kotlin.buildtools.api.arguments.enums.MetadataTargetPlatform
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.copyK2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArgumentsAllErrors
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal class MetadataArgumentsImpl(
  protected override val compilerArguments:
      K2MetadataCompilerArguments = K2MetadataCompilerArguments(),
  protected override val optionsMap: MutableMap<String, Any?> = mutableMapOf(),
  argumentValidationErrors: Set<String> = emptySet(),
  restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
  argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
) : CommonCompilerArgumentsImpl(compilerArguments, optionsMap, argumentValidationErrors, restrictedArgViolations, argumentParseDiagnostics),
    MetadataArguments,
    MetadataArguments.Builder,
    DeepCopyable<MetadataArgumentsImpl> {
  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: MetadataArgument<V>): V = getOption(key.id) as V

  private operator fun <V> `set`(key: MetadataArgument<V>, `value`: V) {
    setOption(key.id, value)
  }

  public operator fun contains(key: MetadataArgument<*>): Boolean = isArgumentKnown(key.id) 

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: MetadataArguments.MetadataArgument<V>): V = getOption(key.id) as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: MetadataArguments.MetadataArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    setOption(key.id, value)
  }

  @Suppress(
    "UNCHECKED_CAST",
    "DEPRECATION",
  )
  private fun getOption(keyId: String): Any? = when (keyId) {
    "X_FRIEND_PATHS" -> {
    this.compilerArguments.friendPaths.mapOrEmpty { Path(it) }
    }
    "X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT" -> {
    try {
    this.compilerArguments.klibZipFileAccessorCacheLimit.let { it.toInt() }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_LEGACY_METADATA_JAR_K2" -> {
    this.compilerArguments.legacyMetadataJar
    }
    "X_REFINES_PATHS" -> {
    this.compilerArguments.refinesPaths.mapOrEmpty { Path(it) }
    }
    "X_TARGET_PLATFORM" -> {
    try {
    this.compilerArguments.targetPlatform.map { MetadataTargetPlatform.entries.firstOrNull { entry -> entry.stringValue == it } ?: throw CompilerArgumentsParseException("Unknown -Xtarget-platform value: $it") }
    } catch (_: NoSuchMethodError) { null }
    }
    "CLASSPATH" -> {
    this.compilerArguments.classpath?.split(File.pathSeparator)?.map { Path(it) }
    }
    "D" -> {
    this.compilerArguments.destination
    }
    "MODULE_NAME" -> {
    this.compilerArguments.moduleName
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
      "X_FRIEND_PATHS" -> {
      this.compilerArguments.friendPaths = (value as List<java.nio.`file`.Path>).map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
      }
      "X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT" -> {
      try {
      this.compilerArguments.klibZipFileAccessorCacheLimit = (value as Int).toString()
      } catch (_: NoSuchMethodError) { }
      }
      "X_LEGACY_METADATA_JAR_K2" -> {
      this.compilerArguments.legacyMetadataJar = (value as Boolean)
      }
      "X_REFINES_PATHS" -> {
      this.compilerArguments.refinesPaths = (value as List<java.nio.`file`.Path>).map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
      }
      "X_TARGET_PLATFORM" -> {
      try {
      this.compilerArguments.targetPlatform = (value as List<MetadataTargetPlatform>).map { it.stringValue }.toTypedArray()
      } catch (_: NoSuchMethodError) { }
      }
      "CLASSPATH" -> {
      this.compilerArguments.classpath = (value as List<java.nio.`file`.Path>?)?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
      }
      "D" -> {
      this.compilerArguments.destination = (value as String?)
      }
      "MODULE_NAME" -> {
      this.compilerArguments.moduleName = (value as String?)
      }
      else -> optionsMap[keyId] = value
    }
  }

  override fun deepCopy(): MetadataArgumentsImpl = MetadataArgumentsImpl(org.jetbrains.kotlin.cli.common.arguments.copyK2MetadataCompilerArguments(this.compilerArguments, org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments()).also { newArgs -> newArgs.errors = this.compilerArguments.errors } , optionsMap.toMutableMap(), _argumentValidationErrors.toMutableSet(), restrictedArgViolations.toList(),  argumentParseDiagnostics.copy())

  override fun build(): MetadataArgumentsImpl = deepCopy()

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(): K2MetadataCompilerArguments {
    val arguments = copyK2MetadataCompilerArguments(compilerArguments, K2MetadataCompilerArguments()).also { newArgs -> newArgs.errors = compilerArguments.errors } 
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys.filterNot { isArgumentKnown(it) }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    populateExplicitArguments(arguments)
    return arguments
  }

  protected fun applyCompilerArguments(arguments: K2MetadataCompilerArguments) {
    copyK2MetadataCompilerArguments(arguments, this.compilerArguments).also { newArgs -> newArgs.errors = arguments.errors } 
    super.applyCompilerArguments(arguments)
  }

  protected override fun isArgumentKnown(name: String): Boolean = name in knownArguments || super.isArgumentKnown(name)

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: K2MetadataCompilerArguments = K2MetadataCompilerArguments()): K2MetadataCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    arguments.friendPaths = this.compilerArguments.friendPaths
    arguments.klibZipFileAccessorCacheLimit = this.compilerArguments.klibZipFileAccessorCacheLimit
    arguments.legacyMetadataJar = this.compilerArguments.legacyMetadataJar
    arguments.refinesPaths = this.compilerArguments.refinesPaths
    arguments.targetPlatform = this.compilerArguments.targetPlatform
    arguments.classpath = this.compilerArguments.classpath
    arguments.destination = this.compilerArguments.destination
    arguments.moduleName = this.compilerArguments.moduleName
    return arguments
  }

  override fun applyArgumentStrings(arguments: List<String>) {
    val compilerArgs: K2MetadataCompilerArguments = parseCommandLineArguments(arguments)
    collectRestrictedArgViolations(compilerArgs, K2MetadataCompilerArguments())
    validateArgumentsAllErrors(compilerArgs.errors).forEach { _argumentValidationErrors.add(it) }
    argumentParseDiagnostics.record(compilerArgs, arguments) { toCompilerArguments() }
    applyCompilerArguments(compilerArgs)
  }

  override fun toArgumentStrings(): List<String> {
    val arguments = toCompilerArguments().compilerToArgumentStrings(allowArgFileInValues = false)
    return arguments
  }

  @Suppress("DEPRECATION")
  internal override fun collectRestrictedArgViolations(compilerArgs: CommonToolArguments, defaultArgs: CommonToolArguments) {
    super.collectRestrictedArgViolations(compilerArgs, defaultArgs)
    val args = compilerArgs as K2MetadataCompilerArguments
    val castedDefaults = defaultArgs as K2MetadataCompilerArguments
    if (args.destination != castedDefaults.destination) _restrictedArgViolations.add(RestrictedArgViolation.Error("Argument '-d' is not supported in the Build Tools API. The destination is configured via the destination parameter of metadataKlibCompilationOperationBuilder."))
    if (args.legacyMetadataJar != castedDefaults.legacyMetadataJar) _restrictedArgViolations.add(RestrictedArgViolation.Warning("Argument '-Xlegacy-metadata-jar-k2' is not supported in the Build Tools API. This warning will become an error starting from Kotlin 2.6.0."))
  }

  /**
   * Returns a sorted list of compiler argument strings representing only the arguments
   * that affect the compilation outcome (i.e. those with [affectsCompilationOutcome][org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument.affectsCompilationOutcome] set to true).
   * Arguments with default values are omitted from the output, because [toCompilerArgumentsAffectingOutcome]
   * only sets arguments that have been explicitly assigned, and [compilerToArgumentStrings][org.jetbrains.kotlin.compilerRunner.toArgumentStrings]
   * skips properties whose value matches the default.
   */
  public fun toCompilationInputs(): List<String> = toCompilerArgumentsAffectingOutcome().compilerToArgumentStrings(allowArgFileInValues = false).sorted()

  public class MetadataArgument<V>(
    public val id: String,
  ) {
    init {
      knownArguments.add(id)}
  }

  public companion object {
    private val knownArguments: MutableSet<String> = mutableSetOf()

    public val X_FRIEND_PATHS: MetadataArgument<List<java.nio.`file`.Path>> =
        MetadataArgument("X_FRIEND_PATHS")

    public val X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT: MetadataArgument<Int> =
        MetadataArgument("X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT")

    public val X_LEGACY_METADATA_JAR_K2: MetadataArgument<Boolean> =
        MetadataArgument("X_LEGACY_METADATA_JAR_K2")

    public val X_REFINES_PATHS: MetadataArgument<List<java.nio.`file`.Path>> =
        MetadataArgument("X_REFINES_PATHS")

    public val X_TARGET_PLATFORM: MetadataArgument<List<MetadataTargetPlatform>> =
        MetadataArgument("X_TARGET_PLATFORM")

    public val CLASSPATH: MetadataArgument<List<java.nio.`file`.Path>?> =
        MetadataArgument("CLASSPATH")

    public val D: MetadataArgument<String?> = MetadataArgument("D")

    public val MODULE_NAME: MetadataArgument<String?> = MetadataArgument("MODULE_NAME")
  }
}
