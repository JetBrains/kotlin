// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

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
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.toTypedArray
import kotlin.io.path.Path
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonKlibBasedArgumentsImpl.Companion.X_KLIB_ABI_VERSION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonKlibBasedArgumentsImpl.Companion.X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonKlibBasedArgumentsImpl.Companion.X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonKlibBasedArgumentsImpl.Companion.X_KLIB_IR_INLINER
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonKlibBasedArgumentsImpl.Companion.X_KLIB_NORMALIZE_ABSOLUTE_PATH
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonKlibBasedArgumentsImpl.Companion.X_KLIB_RELATIVE_PATH_BASE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonKlibBasedArgumentsImpl.Companion.X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonKlibBasedArgumentsImpl.Companion.X_PARTIAL_LINKAGE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonKlibBasedArgumentsImpl.Companion.X_PARTIAL_LINKAGE_LOGLEVEL
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonKlibBasedArgumentsImpl.Companion.X_SKIP_LIBRARY_SPECIAL_COMPATIBILITY_CHECKS
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.CommonKlibBasedArguments
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KlibIrInlinerMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.PartialLinkageLogLevel
import org.jetbrains.kotlin.buildtools.api.arguments.enums.PartialLinkageMode
import org.jetbrains.kotlin.cli.common.arguments.CommonKlibBasedCompilerArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal abstract class CommonKlibBasedArgumentsImpl(
  private val adapter: CommonKlibBasedArgumentValueAdapter? = null,
  argumentValidationErrors: Set<String> = emptySet(),
  restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
) : CommonCompilerArgumentsImpl(adapter, argumentValidationErrors, restrictedArgViolations),
    CommonKlibBasedArguments,
    CommonKlibBasedArguments.Builder {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: CommonKlibBasedArguments.CommonKlibBasedArgument<V>): V {
    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
    return adapter?.mapFrom(optionsMap[key.id], key) ?: optionsMap[key.id] as V
  }

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonKlibBasedArguments.CommonKlibBasedArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 4, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    optionsMap[key.id] = adapter?.mapTo(`value`, key) ?: `value`
  }

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: CommonKlibBasedArgument<V>): V = optionsMap[key.id] as V

  private operator fun <V> `set`(key: CommonKlibBasedArgument<V>, `value`: V) {
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
    if (X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY in this) { arguments.duplicatedUniqueNameStrategy = get(X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY)?.stringValue}
    if (X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS in this) { arguments.enableSignatureClashChecks = get(X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS)}
    if (X_KLIB_IR_INLINER in this) { arguments.irInlinerBeforeKlibSerialization = get(X_KLIB_IR_INLINER).stringValue}
    if (X_KLIB_NORMALIZE_ABSOLUTE_PATH in this) { arguments.normalizeAbsolutePath = get(X_KLIB_NORMALIZE_ABSOLUTE_PATH)}
    if (X_KLIB_RELATIVE_PATH_BASE in this) { arguments.relativePathBases = get(X_KLIB_RELATIVE_PATH_BASE).map { it.absolutePathStringOrThrow() }.toTypedArray()}
    if (X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT in this) { arguments.klibZipFileAccessorCacheLimit = get(X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT).toString()}
    if (X_PARTIAL_LINKAGE in this) { arguments.partialLinkageMode = get(X_PARTIAL_LINKAGE)?.stringValue}
    if (X_PARTIAL_LINKAGE_LOGLEVEL in this) { arguments.partialLinkageLogLevel = get(X_PARTIAL_LINKAGE_LOGLEVEL)?.stringValue}
    if (X_SKIP_LIBRARY_SPECIAL_COMPATIBILITY_CHECKS in this) { arguments.skipLibrarySpecialCompatibilityChecks = get(X_SKIP_LIBRARY_SPECIAL_COMPATIBILITY_CHECKS)}
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: CommonKlibBasedCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { this[X_KLIB_ABI_VERSION] = arguments.customKlibAbiVersion } catch (_: NoSuchMethodError) {  }
    try { this[X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY] = arguments.duplicatedUniqueNameStrategy?.let { DuplicatedUniqueNameStrategy.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::duplicatedUniqueNameStrategy, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xklib-duplicated-unique-name-strategy value: $it") } } catch (_: NoSuchMethodError) {  }
    try { this[X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS] = arguments.enableSignatureClashChecks } catch (_: NoSuchMethodError) {  }
    try { this[X_KLIB_IR_INLINER] = arguments.irInlinerBeforeKlibSerialization.let { KlibIrInlinerMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::irInlinerBeforeKlibSerialization, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xklib-ir-inliner value: $it") } } catch (_: NoSuchMethodError) {  }
    try { this[X_KLIB_NORMALIZE_ABSOLUTE_PATH] = arguments.normalizeAbsolutePath } catch (_: NoSuchMethodError) {  }
    try { this[X_KLIB_RELATIVE_PATH_BASE] = arguments.relativePathBases.mapOrEmpty { Path(it) } } catch (_: NoSuchMethodError) {  }
    try { this[X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT] = arguments.klibZipFileAccessorCacheLimit.let { it.toInt() } } catch (_: NoSuchMethodError) {  }
    try { this[X_PARTIAL_LINKAGE] = arguments.partialLinkageMode?.let { PartialLinkageMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::partialLinkageMode, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xpartial-linkage value: $it") } } catch (_: NoSuchMethodError) {  }
    try { this[X_PARTIAL_LINKAGE_LOGLEVEL] = arguments.partialLinkageLogLevel?.let { PartialLinkageLogLevel.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::partialLinkageLogLevel, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xpartial-linkage-loglevel value: $it") } } catch (_: NoSuchMethodError) {  }
    try { this[X_SKIP_LIBRARY_SPECIAL_COMPATIBILITY_CHECKS] = arguments.skipLibrarySpecialCompatibilityChecks } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: CommonKlibBasedCompilerArguments): CommonKlibBasedCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    if (X_KLIB_ABI_VERSION in this) { arguments.customKlibAbiVersion = get(X_KLIB_ABI_VERSION)}
    if (X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY in this) { arguments.duplicatedUniqueNameStrategy = get(X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY)?.stringValue}
    if (X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS in this) { arguments.enableSignatureClashChecks = get(X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS)}
    if (X_KLIB_IR_INLINER in this) { arguments.irInlinerBeforeKlibSerialization = get(X_KLIB_IR_INLINER).stringValue}
    if (X_KLIB_NORMALIZE_ABSOLUTE_PATH in this) { arguments.normalizeAbsolutePath = get(X_KLIB_NORMALIZE_ABSOLUTE_PATH)}
    if (X_KLIB_RELATIVE_PATH_BASE in this) { arguments.relativePathBases = get(X_KLIB_RELATIVE_PATH_BASE).map { it.absolutePathStringOrThrow() }.toTypedArray()}
    if (X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT in this) { arguments.klibZipFileAccessorCacheLimit = get(X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT).toString()}
    if (X_PARTIAL_LINKAGE in this) { arguments.partialLinkageMode = get(X_PARTIAL_LINKAGE)?.stringValue}
    if (X_PARTIAL_LINKAGE_LOGLEVEL in this) { arguments.partialLinkageLogLevel = get(X_PARTIAL_LINKAGE_LOGLEVEL)?.stringValue}
    if (X_SKIP_LIBRARY_SPECIAL_COMPATIBILITY_CHECKS in this) { arguments.skipLibrarySpecialCompatibilityChecks = get(X_SKIP_LIBRARY_SPECIAL_COMPATIBILITY_CHECKS)}
    return arguments
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

    public val X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY:
        CommonKlibBasedArgument<DuplicatedUniqueNameStrategy?> =
        CommonKlibBasedArgument("X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY")

    public val X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS: CommonKlibBasedArgument<Boolean> =
        CommonKlibBasedArgument("X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS")

    public val X_KLIB_IR_INLINER: CommonKlibBasedArgument<KlibIrInlinerMode> =
        CommonKlibBasedArgument("X_KLIB_IR_INLINER")

    public val X_KLIB_NORMALIZE_ABSOLUTE_PATH: CommonKlibBasedArgument<Boolean> =
        CommonKlibBasedArgument("X_KLIB_NORMALIZE_ABSOLUTE_PATH")

    public val X_KLIB_RELATIVE_PATH_BASE: CommonKlibBasedArgument<List<java.nio.`file`.Path>> =
        CommonKlibBasedArgument("X_KLIB_RELATIVE_PATH_BASE")

    public val X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT: CommonKlibBasedArgument<Int> =
        CommonKlibBasedArgument("X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT")

    public val X_PARTIAL_LINKAGE: CommonKlibBasedArgument<PartialLinkageMode?> =
        CommonKlibBasedArgument("X_PARTIAL_LINKAGE")

    public val X_PARTIAL_LINKAGE_LOGLEVEL: CommonKlibBasedArgument<PartialLinkageLogLevel?> =
        CommonKlibBasedArgument("X_PARTIAL_LINKAGE_LOGLEVEL")

    public val X_SKIP_LIBRARY_SPECIAL_COMPATIBILITY_CHECKS: CommonKlibBasedArgument<Boolean> =
        CommonKlibBasedArgument("X_SKIP_LIBRARY_SPECIAL_COMPATIBILITY_CHECKS")
  }
}
