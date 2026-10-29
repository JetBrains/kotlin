// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:UseSerializers(PathAsStringSerializer::class)
@file:OptIn(ExperimentalCompilerArgument::class)
@file:Suppress("EnumValuesSoftDeprecate")

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import java.io.File
import java.lang.IllegalStateException
import java.nio.`file`.Path
import kotlin.Any
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.joinToString
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.toTypedArray
import kotlin.text.split
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.`internal`.DeepCopyable
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.MetadataTargetPlatform
import org.jetbrains.kotlin.buildtools.`internal`.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArgumentsAllErrors
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

@Serializable
internal class MetadataArgumentsImpl() : CommonCompilerArgumentsImpl(), MetadataArguments,
    MetadataArguments.Builder, DeepCopyable<MetadataArgumentsImpl> {
  @Transient
  private val defaultArguments: K2MetadataCompilerArguments = createDefaultArguments()

  @Transient
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @SerialName("X_FRIEND_PATHS")
  internal var friendPaths: List<Path> =
      defaultArguments.friendPaths.mapOrEmpty { kotlin.io.path.Path(it) }

  @SerialName("X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT")
  internal var klibZipFileAccessorCacheLimit: Int =
      defaultArguments.klibZipFileAccessorCacheLimit.let { it.toInt() }

  @SerialName("X_LEGACY_METADATA_JAR_K2")
  internal var legacyMetadataJar: Boolean = defaultArguments.legacyMetadataJar

  @SerialName("X_REFINES_PATHS")
  internal var refinesPaths: List<Path> =
      defaultArguments.refinesPaths.mapOrEmpty { kotlin.io.path.Path(it) }

  @SerialName("X_TARGET_PLATFORM")
  internal var targetPlatform: List<MetadataTargetPlatform> =
      defaultArguments.targetPlatform.map { MetadataTargetPlatform.values().firstOrNull { entry -> entry.stringValue == it } ?: throw CompilerArgumentsParseException("Unknown -Xtarget-platform value: $it") }

  @SerialName("CLASSPATH")
  internal var classpath: List<Path>? =
      defaultArguments.classpath?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) }

  @SerialName("D")
  internal var destination: String? = defaultArguments.destination

  @SerialName("MODULE_NAME")
  internal var moduleName: String? = defaultArguments.moduleName

  public constructor(
    argumentValidationErrors: Set<String> = emptySet(),
    restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
    argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
  ) : this() {
    _argumentValidationErrors += argumentValidationErrors
    _restrictedArgViolations += restrictedArgViolations
    this.argumentParseDiagnostics += argumentParseDiagnostics
  }

  override fun createDefaultArguments(): K2MetadataCompilerArguments = K2MetadataCompilerArguments()

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: MetadataArguments.MetadataArgument<V>): V = MetadataArgumentValueAdapter.toApi(when (key.id) {
    "X_FRIEND_PATHS" -> friendPaths
    "X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT" -> klibZipFileAccessorCacheLimit
    "X_LEGACY_METADATA_JAR_K2" -> legacyMetadataJar
    "X_REFINES_PATHS" -> refinesPaths
    "X_TARGET_PLATFORM" -> targetPlatform
    "CLASSPATH" -> classpath
    "D" -> destination
    "MODULE_NAME" -> moduleName
    else -> {
            check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
            optionsMap[key.id]
        }
  }
  )

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: MetadataArguments.MetadataArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    when (key.id) {
      "X_FRIEND_PATHS" -> friendPaths = MetadataArgumentValueAdapter.toImpl(value)
      "X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT" -> klibZipFileAccessorCacheLimit = MetadataArgumentValueAdapter.toImpl(value)
      "X_LEGACY_METADATA_JAR_K2" -> legacyMetadataJar = MetadataArgumentValueAdapter.toImpl(value)
      "X_REFINES_PATHS" -> refinesPaths = MetadataArgumentValueAdapter.toImpl(value)
      "X_TARGET_PLATFORM" -> targetPlatform = MetadataArgumentValueAdapter.toImpl(value)
      "CLASSPATH" -> classpath = MetadataArgumentValueAdapter.toImpl(value)
      "D" -> destination = MetadataArgumentValueAdapter.toImpl(value)
      "MODULE_NAME" -> moduleName = MetadataArgumentValueAdapter.toImpl(value)
      else -> {
              optionsMap[key.id] = MetadataArgumentValueAdapter.toImpl(value)
          }
    }
  }

  override fun deepCopy(): MetadataArgumentsImpl = MetadataArgumentsImpl(argumentValidationErrors = argumentValidationErrors.toSet(), restrictedArgViolations = restrictedArgViolations.toList(), argumentParseDiagnostics = argumentParseDiagnostics.copy()).also { newArgs -> newArgs.applyCompilerArguments(toCompilerArguments()) }

  override fun build(): MetadataArgumentsImpl = deepCopy()

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(): K2MetadataCompilerArguments {
    val arguments = K2MetadataCompilerArguments()
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    arguments.friendPaths = friendPaths.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.klibZipFileAccessorCacheLimit = klibZipFileAccessorCacheLimit.toString()
    arguments.legacyMetadataJar = legacyMetadataJar
    arguments.refinesPaths = refinesPaths.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.targetPlatform = targetPlatform.map { it.stringValue }.toTypedArray()
    arguments.classpath = classpath?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.destination = destination
    arguments.moduleName = moduleName
    arguments.internalArguments = parseCommandLineArguments<K2MetadataCompilerArguments>(internalArguments.toList()).internalArguments
    populateExplicitArguments(arguments)
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: K2MetadataCompilerArguments) {
    super.applyCompilerArguments(arguments)
    friendPaths = arguments.friendPaths.mapOrEmpty { kotlin.io.path.Path(it) }
    klibZipFileAccessorCacheLimit = arguments.klibZipFileAccessorCacheLimit.let { it.toInt() }
    legacyMetadataJar = arguments.legacyMetadataJar
    refinesPaths = arguments.refinesPaths.mapOrEmpty { kotlin.io.path.Path(it) }
    try { targetPlatform = arguments.targetPlatform.map { MetadataTargetPlatform.values().firstOrNull { entry -> entry.stringValue == it } ?: throw CompilerArgumentsParseException("Unknown -Xtarget-platform value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    classpath = arguments.classpath?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) }
    destination = arguments.destination
    moduleName = arguments.moduleName
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: K2MetadataCompilerArguments = K2MetadataCompilerArguments()): K2MetadataCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    arguments.friendPaths = friendPaths.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.klibZipFileAccessorCacheLimit = klibZipFileAccessorCacheLimit.toString()
    arguments.legacyMetadataJar = legacyMetadataJar
    arguments.refinesPaths = refinesPaths.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.targetPlatform = targetPlatform.map { it.stringValue }.toTypedArray()
    arguments.classpath = classpath?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.destination = destination
    arguments.moduleName = moduleName
    return arguments
  }

  @Deprecated(
    message = "This method is deprecated. Use applyCommandLineArguments instead.",
    level = DeprecationLevel.WARNING,
  )
  override fun applyArgumentStrings(arguments: List<String>) {
    val compilerArgs: K2MetadataCompilerArguments = parseCommandLineArguments(arguments)
    collectRestrictedArgViolations(compilerArgs, K2MetadataCompilerArguments())
    validateArgumentsAllErrors(compilerArgs.errors).forEach { _argumentValidationErrors.add(it) }
    argumentParseDiagnostics.record(compilerArgs, arguments) { toCompilerArguments() }
    applyCompilerArguments(compilerArgs)
  }

  @DelicateBuildToolsApi
  override fun applyCommandLineArguments(arguments: List<String>) {
    val compilerArgs = toCompilerArguments()
    parseCommandLineArguments(arguments, compilerArgs, false)
    handleCustomPluginArguments(this, compilerArgs)
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

  public companion object {
    private val knownOptionIds: List<String> =
        listOf("X_FRIEND_PATHS","X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT","X_LEGACY_METADATA_JAR_K2","X_REFINES_PATHS","X_TARGET_PLATFORM","CLASSPATH","D","MODULE_NAME",)
  }
}
