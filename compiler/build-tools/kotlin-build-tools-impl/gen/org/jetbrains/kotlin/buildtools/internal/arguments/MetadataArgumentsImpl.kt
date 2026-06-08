// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import java.lang.IllegalStateException
import kotlin.Any
import kotlin.Array
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
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import org.jetbrains.kotlin.buildtools.`internal`.DeepCopyable
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.MetadataArgumentsImpl.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.`internal`.arguments.MetadataArgumentsImpl.Companion.D
import org.jetbrains.kotlin.buildtools.`internal`.arguments.MetadataArgumentsImpl.Companion.MODULE_NAME
import org.jetbrains.kotlin.buildtools.`internal`.arguments.MetadataArgumentsImpl.Companion.X_FRIEND_PATHS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.MetadataArgumentsImpl.Companion.X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT
import org.jetbrains.kotlin.buildtools.`internal`.arguments.MetadataArgumentsImpl.Companion.X_LEGACY_METADATA_JAR_K2
import org.jetbrains.kotlin.buildtools.`internal`.arguments.MetadataArgumentsImpl.Companion.X_REFINES_PATHS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.MetadataArgumentsImpl.Companion.X_TARGET_PLATFORM
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
  private val adapter: MetadataArgumentValueAdapter? = null,
  argumentValidationErrors: Set<String> = emptySet(),
  restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
) : CommonCompilerArgumentsImpl(adapter, argumentValidationErrors, restrictedArgViolations),
    MetadataArguments,
    MetadataArguments.Builder,
    DeepCopyable<MetadataArgumentsImpl> {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()
  init {
    applyCompilerArguments(K2MetadataCompilerArguments())
  }

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: MetadataArgument<V>): V = optionsMap[key.id] as V

  private operator fun <V> `set`(key: MetadataArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: MetadataArgument<*>): Boolean = key.id in optionsMap

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: MetadataArguments.MetadataArgument<V>): V {
    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
    return adapter?.mapFrom(optionsMap[key.id], key) ?: optionsMap[key.id] as V
  }

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: MetadataArguments.MetadataArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 4, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    optionsMap[key.id] = adapter?.mapTo(`value`, key) ?: `value`
  }

  override fun deepCopy(): MetadataArgumentsImpl = MetadataArgumentsImpl(adapter, argumentValidationErrors.toSet(), restrictedArgViolations.toList()).also { newArgs -> newArgs.applyCompilerArguments(toCompilerArguments()) }

  override fun build(): MetadataArgumentsImpl = deepCopy()

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(): K2MetadataCompilerArguments {
    val arguments = K2MetadataCompilerArguments()
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys.filter { it !in knownArguments }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    if (X_FRIEND_PATHS in this) { arguments.friendPaths = get(X_FRIEND_PATHS) ?: emptyArray()}
    if (X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT in this) { arguments.klibZipFileAccessorCacheLimit = get(X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT).toString()}
    if (X_LEGACY_METADATA_JAR_K2 in this) { arguments.legacyMetadataJar = get(X_LEGACY_METADATA_JAR_K2)}
    if (X_REFINES_PATHS in this) { arguments.refinesPaths = get(X_REFINES_PATHS) ?: emptyArray()}
    if (X_TARGET_PLATFORM in this) { arguments.targetPlatform = get(X_TARGET_PLATFORM) ?: emptyArray()}
    if (CLASSPATH in this) { arguments.classpath = get(CLASSPATH)}
    if (D in this) { arguments.destination = get(D)}
    if (MODULE_NAME in this) { arguments.moduleName = get(MODULE_NAME)}
    arguments.internalArguments = parseCommandLineArguments<K2MetadataCompilerArguments>(internalArguments.toList()).internalArguments
    populateExplicitArguments(arguments)
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: K2MetadataCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { this[X_FRIEND_PATHS] = arguments.friendPaths } catch (_: NoSuchMethodError) {  }
    try { this[X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT] = arguments.klibZipFileAccessorCacheLimit.let { it.toInt() } } catch (_: NoSuchMethodError) {  }
    try { this[X_LEGACY_METADATA_JAR_K2] = arguments.legacyMetadataJar } catch (_: NoSuchMethodError) {  }
    try { this[X_REFINES_PATHS] = arguments.refinesPaths } catch (_: NoSuchMethodError) {  }
    try { this[X_TARGET_PLATFORM] = arguments.targetPlatform } catch (_: NoSuchMethodError) {  }
    try { this[CLASSPATH] = arguments.classpath } catch (_: NoSuchMethodError) {  }
    try { this[D] = arguments.destination } catch (_: NoSuchMethodError) {  }
    try { this[MODULE_NAME] = arguments.moduleName } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: K2MetadataCompilerArguments = K2MetadataCompilerArguments()): K2MetadataCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    if (X_FRIEND_PATHS in this) { arguments.friendPaths = get(X_FRIEND_PATHS) ?: emptyArray()}
    if (X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT in this) { arguments.klibZipFileAccessorCacheLimit = get(X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT).toString()}
    if (X_LEGACY_METADATA_JAR_K2 in this) { arguments.legacyMetadataJar = get(X_LEGACY_METADATA_JAR_K2)}
    if (X_REFINES_PATHS in this) { arguments.refinesPaths = get(X_REFINES_PATHS) ?: emptyArray()}
    if (X_TARGET_PLATFORM in this) { arguments.targetPlatform = get(X_TARGET_PLATFORM) ?: emptyArray()}
    if (CLASSPATH in this) { arguments.classpath = get(CLASSPATH)}
    if (D in this) { arguments.destination = get(D)}
    if (MODULE_NAME in this) { arguments.moduleName = get(MODULE_NAME)}
    return arguments
  }

  override fun applyArgumentStrings(arguments: List<String>) {
    val compilerArgs: K2MetadataCompilerArguments = parseCommandLineArguments(arguments)
    collectRestrictedArgViolations(compilerArgs, K2MetadataCompilerArguments())
    validateArgumentsAllErrors(compilerArgs.errors).forEach { _argumentValidationErrors.add(it) }
    applyCompilerArguments(compilerArgs)
  }

  override fun toArgumentStrings(): List<String> {
    val arguments = toCompilerArguments().compilerToArgumentStrings(allowArgFileInValues = false)
    return arguments
  }

  internal override fun collectRestrictedArgViolations(compilerArgs: CommonToolArguments, defaultArgs: CommonToolArguments) {
    super.collectRestrictedArgViolations(compilerArgs, defaultArgs)
    val args = compilerArgs as K2MetadataCompilerArguments
    val castedDefaults = defaultArgs as K2MetadataCompilerArguments
    if (args.destination != castedDefaults.destination) _restrictedArgViolations.add(RestrictedArgViolation.Warning("Argument '-d' is not supported in the Build Tools API. The destination is configured via the destinationDirectory parameter of jvmCompilationOperationBuilder. This warning will become an error starting from Kotlin 2.5.0."))
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

    public val X_FRIEND_PATHS: MetadataArgument<Array<String>?> = MetadataArgument("X_FRIEND_PATHS")

    public val X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT: MetadataArgument<Int> =
        MetadataArgument("X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT")

    public val X_LEGACY_METADATA_JAR_K2: MetadataArgument<Boolean> =
        MetadataArgument("X_LEGACY_METADATA_JAR_K2")

    public val X_REFINES_PATHS: MetadataArgument<Array<String>?> =
        MetadataArgument("X_REFINES_PATHS")

    public val X_TARGET_PLATFORM: MetadataArgument<Array<String>?> =
        MetadataArgument("X_TARGET_PLATFORM")

    public val CLASSPATH: MetadataArgument<String?> = MetadataArgument("CLASSPATH")

    public val D: MetadataArgument<String?> = MetadataArgument("D")

    public val MODULE_NAME: MetadataArgument<String?> = MetadataArgument("MODULE_NAME")
  }
}
