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
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonKlibBasedArgumentsImpl.Companion.X_KLIB_ABI_VERSION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonKlibBasedArgumentsImpl.Companion.X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonKlibBasedArgumentsImpl.Companion.X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonKlibBasedArgumentsImpl.Companion.X_KLIB_NORMALIZE_ABSOLUTE_PATH
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonKlibBasedArgumentsImpl.Companion.X_KLIB_RELATIVE_PATH_BASE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonKlibBasedArgumentsImpl.Companion.X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonKlibBasedArgumentsImpl.Companion.X_PARTIAL_LINKAGE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonKlibBasedArgumentsImpl.Companion.X_PARTIAL_LINKAGE_LOGLEVEL
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.CommonKlibBasedArguments
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.cli.common.arguments.CommonKlibBasedCompilerArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal abstract class CommonKlibBasedArgumentsImpl : CommonCompilerArgumentsImpl(),
    CommonKlibBasedArguments {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: CommonKlibBasedArguments.CommonKlibBasedArgument<V>): V = optionsMap[key.id] as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonKlibBasedArguments.CommonKlibBasedArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 3, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    optionsMap[key.id] = `value`
  }

  override operator fun contains(key: CommonKlibBasedArguments.CommonKlibBasedArgument<*>): Boolean = key.id in optionsMap

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: CommonKlibBasedArgument<V>): V = optionsMap[key.id] as V

  public operator fun <V> `set`(key: CommonKlibBasedArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: CommonKlibBasedArgument<*>): Boolean = key.id in optionsMap

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: CommonKlibBasedCompilerArguments): CommonKlibBasedCompilerArguments {
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys.filter { it !in knownArguments }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    if (X_KLIB_ABI_VERSION in this) { arguments.customKlibAbiVersion = get(X_KLIB_ABI_VERSION)}
    if (X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY in this) { arguments.duplicatedUniqueNameStrategy = get(X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY)}
    if (X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS in this) { arguments.enableSignatureClashChecks = get(X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS)}
    if (X_KLIB_NORMALIZE_ABSOLUTE_PATH in this) { arguments.normalizeAbsolutePath = get(X_KLIB_NORMALIZE_ABSOLUTE_PATH)}
    if (X_KLIB_RELATIVE_PATH_BASE in this) { arguments.relativePathBases = get(X_KLIB_RELATIVE_PATH_BASE)}
    if (X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT in this) { arguments.klibZipFileAccessorCacheLimit = get(X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT).toString()}
    if (X_PARTIAL_LINKAGE in this) { arguments.partialLinkageMode = get(X_PARTIAL_LINKAGE)}
    if (X_PARTIAL_LINKAGE_LOGLEVEL in this) { arguments.partialLinkageLogLevel = get(X_PARTIAL_LINKAGE_LOGLEVEL)}
    return arguments
  }

  @Suppress("DEPRECATION")
  public fun applyCompilerArguments(arguments: CommonKlibBasedCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { this[X_KLIB_ABI_VERSION] = arguments.customKlibAbiVersion } catch (_: NoSuchMethodError) {  }
    try { this[X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY] = arguments.duplicatedUniqueNameStrategy } catch (_: NoSuchMethodError) {  }
    try { this[X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS] = arguments.enableSignatureClashChecks } catch (_: NoSuchMethodError) {  }
    try { this[X_KLIB_NORMALIZE_ABSOLUTE_PATH] = arguments.normalizeAbsolutePath } catch (_: NoSuchMethodError) {  }
    try { this[X_KLIB_RELATIVE_PATH_BASE] = arguments.relativePathBases } catch (_: NoSuchMethodError) {  }
    try { this[X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT] = arguments.klibZipFileAccessorCacheLimit.let { it.toInt() } } catch (_: NoSuchMethodError) {  }
    try { this[X_PARTIAL_LINKAGE] = arguments.partialLinkageMode } catch (_: NoSuchMethodError) {  }
    try { this[X_PARTIAL_LINKAGE_LOGLEVEL] = arguments.partialLinkageLogLevel } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  public class CommonKlibBasedArgument<V>(
    public val id: String,
  ) {
    init {
      knownArguments.add(id)}
  }

  public companion object {
    private val knownArguments: MutableSet<String> = mutableSetOf()

    public val X_KLIB_ABI_VERSION: CommonKlibBasedArgument<String?> =
        CommonKlibBasedArgument("X_KLIB_ABI_VERSION")

    public val X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY: CommonKlibBasedArgument<String?> =
        CommonKlibBasedArgument("X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY")

    public val X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS: CommonKlibBasedArgument<Boolean> =
        CommonKlibBasedArgument("X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS")

    public val X_KLIB_NORMALIZE_ABSOLUTE_PATH: CommonKlibBasedArgument<Boolean> =
        CommonKlibBasedArgument("X_KLIB_NORMALIZE_ABSOLUTE_PATH")

    public val X_KLIB_RELATIVE_PATH_BASE: CommonKlibBasedArgument<Array<String>?> =
        CommonKlibBasedArgument("X_KLIB_RELATIVE_PATH_BASE")

    public val X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT: CommonKlibBasedArgument<Int> =
        CommonKlibBasedArgument("X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT")

    public val X_PARTIAL_LINKAGE: CommonKlibBasedArgument<String?> =
        CommonKlibBasedArgument("X_PARTIAL_LINKAGE")

    public val X_PARTIAL_LINKAGE_LOGLEVEL: CommonKlibBasedArgument<String?> =
        CommonKlibBasedArgument("X_PARTIAL_LINKAGE_LOGLEVEL")
  }
}
