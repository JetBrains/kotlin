// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:UseSerializers(PathAsStringSerializer::class)
@file:OptIn(ExperimentalCompilerArgument::class)
@file:Suppress("EnumValuesSoftDeprecate")

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import java.lang.IllegalStateException
import java.nio.`file`.Path
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.KlibIrInlinerMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.PartialLinkageLogLevel
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.PartialLinkageMode
import org.jetbrains.kotlin.buildtools.`internal`.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.`internal`.serializability.getPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.`internal`.serializability.setPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.CommonKlibBasedArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonKlibBasedArgumentsKlibArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonKlibBasedArgumentsLinkingArguments
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.cli.common.arguments.CommonKlibBasedCompilerArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

@Serializable
internal abstract class CommonKlibBasedArgumentsImpl() : CommonCompilerArgumentsImpl(),
    CommonKlibBasedArguments, CommonKlibBasedArguments.Builder,
    CommonKlibBasedArgumentsKlibArguments, CommonKlibBasedArgumentsKlibArguments.Builder,
    CommonKlibBasedArgumentsLinkingArguments, CommonKlibBasedArgumentsLinkingArguments.Builder {
  @Transient
  private val defaultArguments: CommonKlibBasedCompilerArguments = createDefaultArguments()

  @Transient
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @SerialName("X_KLIB_ABI_VERSION")
  internal var customKlibAbiVersion: String? = defaultArguments.customKlibAbiVersion

  @SerialName("X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY")
  internal var duplicatedUniqueNameStrategy: DuplicatedUniqueNameStrategy? =
      defaultArguments.duplicatedUniqueNameStrategy?.let { DuplicatedUniqueNameStrategy.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::duplicatedUniqueNameStrategy, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xklib-duplicated-unique-name-strategy value: $it") }

  @SerialName("X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS")
  internal var enableSignatureClashChecks: Boolean = defaultArguments.enableSignatureClashChecks

  @SerialName("X_KLIB_IR_INLINER")
  internal var irInlinerBeforeKlibSerialization: KlibIrInlinerMode =
      defaultArguments.irInlinerBeforeKlibSerialization.let { KlibIrInlinerMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::irInlinerBeforeKlibSerialization, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xklib-ir-inliner value: $it") }

  @SerialName("X_KLIB_RELATIVE_PATH_BASE")
  internal var relativePathBases: List<Path> =
      defaultArguments.relativePathBases.mapOrEmpty { kotlin.io.path.Path(it) }

  @SerialName("X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT")
  internal var klibZipFileAccessorCacheLimit: Int =
      defaultArguments.klibZipFileAccessorCacheLimit.let { it.toInt() }

  @SerialName("X_PARTIAL_LINKAGE")
  @Suppress("DEPRECATION")
  internal var partialLinkageMode: PartialLinkageMode? =
      defaultArguments.partialLinkageMode?.let { PartialLinkageMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::partialLinkageMode, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xpartial-linkage value: $it") }

  @SerialName("X_PARTIAL_LINKAGE_LOGLEVEL")
  internal var partialLinkageLogLevel: PartialLinkageLogLevel? =
      defaultArguments.partialLinkageLogLevel?.let { PartialLinkageLogLevel.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::partialLinkageLogLevel, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xpartial-linkage-loglevel value: $it") }

  @SerialName("X_SKIP_LIBRARY_SPECIAL_COMPATIBILITY_CHECKS")
  internal var skipLibrarySpecialCompatibilityChecks: Boolean =
      defaultArguments.skipLibrarySpecialCompatibilityChecks

  public constructor(
    argumentValidationErrors: Set<String> = emptySet(),
    restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
    argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
  ) : this() {
    _argumentValidationErrors += argumentValidationErrors
    _restrictedArgViolations += restrictedArgViolations
    this.argumentParseDiagnostics += argumentParseDiagnostics
  }

  abstract override fun createDefaultArguments(): CommonKlibBasedCompilerArguments

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: CommonKlibBasedArgument<V>): V = CommonKlibBasedArgumentsImpl::class.getPropertyWithSerialNameValue(this, key.id)

  public operator fun <V> `set`(key: CommonKlibBasedArgument<V>, `value`: V) {
    CommonKlibBasedArgumentsImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
  }

  public operator fun contains(key: CommonKlibBasedArgument<*>): Boolean = true

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: CommonKlibBasedArguments.CommonKlibBasedArgument<V>): V = try { CommonKlibBasedArgumentValueAdapter.toApi(CommonKlibBasedArgumentsImpl::class.getPropertyWithSerialNameValue(this, key.id)) } catch (_: IllegalStateException) { optionsMap[key.id] } as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonKlibBasedArguments.CommonKlibBasedArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    try { CommonKlibBasedArgumentsImpl::class.setPropertyWithSerialNameValue(this, key.id, CommonKlibBasedArgumentValueAdapter.toImpl(value)) } catch (_: IllegalStateException) { optionsMap[key.id] = value }
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: CommonKlibBasedArgumentsKlibArguments.CommonKlibBasedArgumentsKlibArgument<V>): V = try { CommonKlibBasedArgumentValueAdapter.toApi(CommonKlibBasedArgumentsImpl::class.getPropertyWithSerialNameValue(this, key.id)) } catch (_: IllegalStateException) { optionsMap[key.id] } as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonKlibBasedArgumentsKlibArguments.CommonKlibBasedArgumentsKlibArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    try { CommonKlibBasedArgumentsImpl::class.setPropertyWithSerialNameValue(this, key.id, CommonKlibBasedArgumentValueAdapter.toImpl(value)) } catch (_: IllegalStateException) { optionsMap[key.id] = value }
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: CommonKlibBasedArgumentsLinkingArguments.CommonKlibBasedArgumentsLinkingArgument<V>): V = try { CommonKlibBasedArgumentValueAdapter.toApi(CommonKlibBasedArgumentsImpl::class.getPropertyWithSerialNameValue(this, key.id)) } catch (_: IllegalStateException) { optionsMap[key.id] } as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonKlibBasedArgumentsLinkingArguments.CommonKlibBasedArgumentsLinkingArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    try { CommonKlibBasedArgumentsImpl::class.setPropertyWithSerialNameValue(this, key.id, CommonKlibBasedArgumentValueAdapter.toImpl(value)) } catch (_: IllegalStateException) { optionsMap[key.id] = value }
  }

  abstract override fun build(): CommonKlibBasedArgumentsImpl

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: CommonKlibBasedCompilerArguments): CommonKlibBasedCompilerArguments {
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys.filter {
      when (it) {
        "X_FAKE_OVERRIDE_VALIDATOR" -> throw IllegalStateException("""Compiler parameter not recognized: X_FAKE_OVERRIDE_VALIDATOR. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""")
        "X_KLIB_NORMALIZE_ABSOLUTE_PATH" -> throw IllegalStateException("""Compiler parameter not recognized: X_KLIB_NORMALIZE_ABSOLUTE_PATH. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""")
        else -> true
      }
    }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    arguments.customKlibAbiVersion = customKlibAbiVersion
    arguments.duplicatedUniqueNameStrategy = duplicatedUniqueNameStrategy?.stringValue
    arguments.enableSignatureClashChecks = enableSignatureClashChecks
    arguments.irInlinerBeforeKlibSerialization = irInlinerBeforeKlibSerialization.stringValue
    arguments.relativePathBases = relativePathBases.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.klibZipFileAccessorCacheLimit = klibZipFileAccessorCacheLimit.toString()
    arguments.partialLinkageMode = partialLinkageMode?.stringValue
    arguments.partialLinkageLogLevel = partialLinkageLogLevel?.stringValue
    arguments.skipLibrarySpecialCompatibilityChecks = skipLibrarySpecialCompatibilityChecks
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: CommonKlibBasedCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { customKlibAbiVersion = arguments.customKlibAbiVersion } catch (_: NoSuchMethodError) {  }
    try { duplicatedUniqueNameStrategy = arguments.duplicatedUniqueNameStrategy?.let { DuplicatedUniqueNameStrategy.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::duplicatedUniqueNameStrategy, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xklib-duplicated-unique-name-strategy value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { enableSignatureClashChecks = arguments.enableSignatureClashChecks } catch (_: NoSuchMethodError) {  }
    try { irInlinerBeforeKlibSerialization = arguments.irInlinerBeforeKlibSerialization.let { KlibIrInlinerMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::irInlinerBeforeKlibSerialization, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xklib-ir-inliner value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { relativePathBases = arguments.relativePathBases.mapOrEmpty { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { klibZipFileAccessorCacheLimit = arguments.klibZipFileAccessorCacheLimit.let { it.toInt() } } catch (_: NoSuchMethodError) {  }
    try { partialLinkageMode = arguments.partialLinkageMode?.let { PartialLinkageMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::partialLinkageMode, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xpartial-linkage value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { partialLinkageLogLevel = arguments.partialLinkageLogLevel?.let { PartialLinkageLogLevel.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::partialLinkageLogLevel, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xpartial-linkage-loglevel value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { skipLibrarySpecialCompatibilityChecks = arguments.skipLibrarySpecialCompatibilityChecks } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: CommonKlibBasedCompilerArguments): CommonKlibBasedCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    arguments.customKlibAbiVersion = customKlibAbiVersion
    arguments.duplicatedUniqueNameStrategy = duplicatedUniqueNameStrategy?.stringValue
    arguments.enableSignatureClashChecks = enableSignatureClashChecks
    arguments.irInlinerBeforeKlibSerialization = irInlinerBeforeKlibSerialization.stringValue
    arguments.relativePathBases = relativePathBases.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.klibZipFileAccessorCacheLimit = klibZipFileAccessorCacheLimit.toString()
    arguments.partialLinkageMode = partialLinkageMode?.stringValue
    arguments.partialLinkageLogLevel = partialLinkageLogLevel?.stringValue
    arguments.skipLibrarySpecialCompatibilityChecks = skipLibrarySpecialCompatibilityChecks
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

    public val X_KLIB_RELATIVE_PATH_BASE: CommonKlibBasedArgument<List<Path>> =
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
