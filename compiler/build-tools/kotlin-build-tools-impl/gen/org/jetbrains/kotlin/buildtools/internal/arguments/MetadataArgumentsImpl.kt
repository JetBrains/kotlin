// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

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
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.toTypedArray
import kotlin.text.split
import kotlinx.serialization.SerialName
import org.jetbrains.kotlin.buildtools.`internal`.DeepCopyable
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.MetadataTargetPlatform
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

internal class MetadataArgumentsImpl(
  defaultArguments: K2MetadataCompilerArguments = K2MetadataCompilerArguments(),
  argumentValidationErrors: Set<String> = emptySet(),
  restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
  argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
) : CommonCompilerArgumentsImpl(defaultArguments, argumentValidationErrors, restrictedArgViolations, argumentParseDiagnostics),
    MetadataArguments,
    MetadataArguments.Builder,
    DeepCopyable<MetadataArgumentsImpl> {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @SerialName("X_FRIEND_PATHS")
  protected var `Xfriend-paths`: List<Path> =
      defaultArguments.friendPaths.mapOrEmpty { kotlin.io.path.Path(it) }

  @SerialName("X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT")
  protected var `Xklib-zip-file-accessor-cache-limit`: Int =
      defaultArguments.klibZipFileAccessorCacheLimit.let { it.toInt() }

  @SerialName("X_LEGACY_METADATA_JAR_K2")
  protected var `Xlegacy-metadata-jar-k2`: Boolean = defaultArguments.legacyMetadataJar

  @SerialName("X_REFINES_PATHS")
  protected var `Xrefines-paths`: List<Path> =
      defaultArguments.refinesPaths.mapOrEmpty { kotlin.io.path.Path(it) }

  @SerialName("X_TARGET_PLATFORM")
  protected var `Xtarget-platform`: List<MetadataTargetPlatform> =
      defaultArguments.targetPlatform.map { MetadataTargetPlatform.entries.firstOrNull { entry -> entry.stringValue == it } ?: throw CompilerArgumentsParseException("Unknown -Xtarget-platform value: $it") }

  @SerialName("CLASSPATH")
  protected var classpath: List<Path>? =
      defaultArguments.classpath?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) }

  @SerialName("D")
  protected var d: String? = defaultArguments.destination

  @SerialName("MODULE_NAME")
  protected var `module-name`: String? = defaultArguments.moduleName
  init {
    applyCompilerArguments(K2MetadataCompilerArguments())
  }

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: MetadataArgument<V>): V = optionsMap[key.id] as V

  public operator fun <V> `set`(key: MetadataArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: MetadataArgument<*>): Boolean = key.id in optionsMap

  private operator fun `get`(key: String): Any? = MetadataArgumentValueAdapter.toApi(optionsMap[key])

  private operator fun `set`(key: String, `value`: Any?) {
    optionsMap[key] = MetadataArgumentValueAdapter.toImpl(`value`)
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: MetadataArguments.MetadataArgument<V>): V {
    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
    return this[key.id] as V
  }

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: MetadataArguments.MetadataArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    this[key.id] = `value`
  }

  override fun deepCopy(): MetadataArgumentsImpl = MetadataArgumentsImpl(argumentValidationErrors = argumentValidationErrors.toSet(), restrictedArgViolations = restrictedArgViolations.toList(), argumentParseDiagnostics = argumentParseDiagnostics.copy()).also { newArgs -> newArgs.applyCompilerArguments(toCompilerArguments()) }

  override fun build(): MetadataArgumentsImpl = deepCopy()

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(): K2MetadataCompilerArguments {
    val arguments = K2MetadataCompilerArguments()
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys.filter { it !in knownArguments }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    arguments.friendPaths = `Xfriend-paths`.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.klibZipFileAccessorCacheLimit = `Xklib-zip-file-accessor-cache-limit`.toString()
    arguments.legacyMetadataJar = `Xlegacy-metadata-jar-k2`
    arguments.refinesPaths = `Xrefines-paths`.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.targetPlatform = `Xtarget-platform`.map { it.stringValue }.toTypedArray()
    arguments.classpath = classpath?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.destination = d
    arguments.moduleName = `module-name`
    arguments.internalArguments = parseCommandLineArguments<K2MetadataCompilerArguments>(internalArguments.toList()).internalArguments
    populateExplicitArguments(arguments)
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: K2MetadataCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { `Xfriend-paths` = arguments.friendPaths.mapOrEmpty { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `Xklib-zip-file-accessor-cache-limit` = arguments.klibZipFileAccessorCacheLimit.let { it.toInt() } } catch (_: NoSuchMethodError) {  }
    try { `Xlegacy-metadata-jar-k2` = arguments.legacyMetadataJar } catch (_: NoSuchMethodError) {  }
    try { `Xrefines-paths` = arguments.refinesPaths.mapOrEmpty { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `Xtarget-platform` = arguments.targetPlatform.map { MetadataTargetPlatform.entries.firstOrNull { entry -> entry.stringValue == it } ?: throw CompilerArgumentsParseException("Unknown -Xtarget-platform value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { classpath = arguments.classpath?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { d = arguments.destination } catch (_: NoSuchMethodError) {  }
    try { `module-name` = arguments.moduleName } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: K2MetadataCompilerArguments = K2MetadataCompilerArguments()): K2MetadataCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    arguments.friendPaths = `Xfriend-paths`.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.klibZipFileAccessorCacheLimit = `Xklib-zip-file-accessor-cache-limit`.toString()
    arguments.legacyMetadataJar = `Xlegacy-metadata-jar-k2`
    arguments.refinesPaths = `Xrefines-paths`.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.targetPlatform = `Xtarget-platform`.map { it.stringValue }.toTypedArray()
    arguments.classpath = classpath?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.destination = d
    arguments.moduleName = `module-name`
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

  public class MetadataArgument<V>(
    public val id: String,
  ) {
    init {
      knownArguments.add(id)}
  }

  public companion object {
    private val knownArguments: MutableSet<String> = mutableSetOf()

    public val X_FRIEND_PATHS: MetadataArgument<List<Path>> = MetadataArgument("X_FRIEND_PATHS")

    public val X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT: MetadataArgument<Int> =
        MetadataArgument("X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT")

    public val X_LEGACY_METADATA_JAR_K2: MetadataArgument<Boolean> =
        MetadataArgument("X_LEGACY_METADATA_JAR_K2")

    public val X_REFINES_PATHS: MetadataArgument<List<Path>> = MetadataArgument("X_REFINES_PATHS")

    public val X_TARGET_PLATFORM: MetadataArgument<List<MetadataTargetPlatform>> =
        MetadataArgument("X_TARGET_PLATFORM")

    public val CLASSPATH: MetadataArgument<List<Path>?> = MetadataArgument("CLASSPATH")

    public val D: MetadataArgument<String?> = MetadataArgument("D")

    public val MODULE_NAME: MetadataArgument<String?> = MetadataArgument("MODULE_NAME")
  }
}
