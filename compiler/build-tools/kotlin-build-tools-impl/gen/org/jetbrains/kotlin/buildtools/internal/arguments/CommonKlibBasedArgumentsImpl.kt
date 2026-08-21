// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import java.lang.IllegalStateException
import java.lang.NoSuchMethodError
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
import kotlin.collections.mutableSetOf
import kotlin.collections.toTypedArray
import kotlin.io.path.Path
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.CommonKlibBasedArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonKlibBasedArgumentsKlibArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonKlibBasedArgumentsLinkingArguments
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KlibIrInlinerMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.PartialLinkageLogLevel
import org.jetbrains.kotlin.buildtools.api.arguments.enums.PartialLinkageMode
import org.jetbrains.kotlin.cli.common.arguments.CommonKlibBasedCompilerArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal abstract class CommonKlibBasedArgumentsImpl(
  protected override val compilerArguments: CommonKlibBasedCompilerArguments,
  protected override val optionsMap: MutableMap<String, Any?>,
  argumentValidationErrors: Set<String> = emptySet(),
  restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
  argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
) : CommonCompilerArgumentsImpl(compilerArguments, optionsMap, argumentValidationErrors, restrictedArgViolations, argumentParseDiagnostics),
    CommonKlibBasedArguments,
    CommonKlibBasedArguments.Builder,
    CommonKlibBasedArgumentsKlibArguments,
    CommonKlibBasedArgumentsKlibArguments.Builder,
    CommonKlibBasedArgumentsLinkingArguments,
    CommonKlibBasedArgumentsLinkingArguments.Builder {
  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: CommonKlibBasedArgument<V>): V = getOption(key.id) as V

  private operator fun <V> `set`(key: CommonKlibBasedArgument<V>, `value`: V) {
    setOption(key.id, value)
  }

  public operator fun contains(key: CommonKlibBasedArgument<*>): Boolean = isArgumentKnown(key.id) 

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: CommonKlibBasedArguments.CommonKlibBasedArgument<V>): V = getOption(key.id) as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonKlibBasedArguments.CommonKlibBasedArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    setOption(key.id, value)
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: CommonKlibBasedArgumentsKlibArguments.CommonKlibBasedArgumentsKlibArgument<V>): V = getOption(key.id) as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonKlibBasedArgumentsKlibArguments.CommonKlibBasedArgumentsKlibArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    setOption(key.id, value)
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: CommonKlibBasedArgumentsLinkingArguments.CommonKlibBasedArgumentsLinkingArgument<V>): V = getOption(key.id) as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonKlibBasedArgumentsLinkingArguments.CommonKlibBasedArgumentsLinkingArgument<V>, `value`: V) {
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
    "X_FAKE_OVERRIDE_VALIDATOR" -> {
    try { this.compilerArguments.getUsingReflection<Boolean>("fakeOverrideValidator") } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_FAKE_OVERRIDE_VALIDATOR. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    }
    "X_KLIB_ABI_VERSION" -> {
    this.compilerArguments.customKlibAbiVersion
    }
    "X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY" -> {
    this.compilerArguments.duplicatedUniqueNameStrategy?.let { DuplicatedUniqueNameStrategy.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, this.compilerArguments::duplicatedUniqueNameStrategy, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xklib-duplicated-unique-name-strategy value: $it") }
    }
    "X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS" -> {
    this.compilerArguments.enableSignatureClashChecks
    }
    "X_KLIB_IR_INLINER" -> {
    this.compilerArguments.irInlinerBeforeKlibSerialization.let { KlibIrInlinerMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, this.compilerArguments::irInlinerBeforeKlibSerialization, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xklib-ir-inliner value: $it") }
    }
    "X_KLIB_NORMALIZE_ABSOLUTE_PATH" -> {
    try { this.compilerArguments.getUsingReflection<Boolean>("normalizeAbsolutePath") } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_KLIB_NORMALIZE_ABSOLUTE_PATH. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    }
    "X_KLIB_RELATIVE_PATH_BASE" -> {
    this.compilerArguments.relativePathBases.mapOrEmpty { Path(it) }
    }
    "X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT" -> {
    try {
    this.compilerArguments.klibZipFileAccessorCacheLimit.let { it.toInt() }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_PARTIAL_LINKAGE" -> {
    this.compilerArguments.partialLinkageMode?.let { PartialLinkageMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, this.compilerArguments::partialLinkageMode, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xpartial-linkage value: $it") }
    }
    "X_PARTIAL_LINKAGE_LOGLEVEL" -> {
    this.compilerArguments.partialLinkageLogLevel?.let { PartialLinkageLogLevel.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, this.compilerArguments::partialLinkageLogLevel, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xpartial-linkage-loglevel value: $it") }
    }
    "X_SKIP_LIBRARY_SPECIAL_COMPATIBILITY_CHECKS" -> {
    try {
    this.compilerArguments.skipLibrarySpecialCompatibilityChecks
    } catch (_: NoSuchMethodError) { null }
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
      "X_FAKE_OVERRIDE_VALIDATOR" -> {
      try { this.compilerArguments.setUsingReflection("fakeOverrideValidator", (value as Boolean))
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_FAKE_OVERRIDE_VALIDATOR. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }}
      "X_KLIB_ABI_VERSION" -> {
      this.compilerArguments.customKlibAbiVersion = (value as String?)
      }
      "X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY" -> {
      this.compilerArguments.duplicatedUniqueNameStrategy = (value as DuplicatedUniqueNameStrategy?)?.stringValue
      }
      "X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS" -> {
      this.compilerArguments.enableSignatureClashChecks = (value as Boolean)
      }
      "X_KLIB_IR_INLINER" -> {
      this.compilerArguments.irInlinerBeforeKlibSerialization = (value as KlibIrInlinerMode).stringValue
      }
      "X_KLIB_NORMALIZE_ABSOLUTE_PATH" -> {
      try { this.compilerArguments.setUsingReflection("normalizeAbsolutePath", (value as Boolean))
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_KLIB_NORMALIZE_ABSOLUTE_PATH. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }}
      "X_KLIB_RELATIVE_PATH_BASE" -> {
      this.compilerArguments.relativePathBases = (value as List<java.nio.`file`.Path>).map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
      }
      "X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT" -> {
      try {
      this.compilerArguments.klibZipFileAccessorCacheLimit = (value as Int).toString()
      } catch (_: NoSuchMethodError) { }
      }
      "X_PARTIAL_LINKAGE" -> {
      this.compilerArguments.partialLinkageMode = (value as PartialLinkageMode?)?.stringValue
      }
      "X_PARTIAL_LINKAGE_LOGLEVEL" -> {
      this.compilerArguments.partialLinkageLogLevel = (value as PartialLinkageLogLevel?)?.stringValue
      }
      "X_SKIP_LIBRARY_SPECIAL_COMPATIBILITY_CHECKS" -> {
      try {
      this.compilerArguments.skipLibrarySpecialCompatibilityChecks = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      else -> optionsMap[keyId] = value
    }
  }

  abstract override fun build(): CommonKlibBasedArgumentsImpl

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: CommonKlibBasedCompilerArguments): CommonKlibBasedCompilerArguments {
    super.toCompilerArguments(arguments)
    return arguments
  }

  protected fun applyCompilerArguments(arguments: CommonKlibBasedCompilerArguments) {
    super.applyCompilerArguments(arguments)
  }

  protected override fun isArgumentKnown(name: String): Boolean = name in knownArguments || super.isArgumentKnown(name)

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: CommonKlibBasedCompilerArguments): CommonKlibBasedCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    try { arguments.setUsingReflection("fakeOverrideValidator", this.compilerArguments.getUsingReflection<Boolean>("fakeOverrideValidator")) } catch (_: NoSuchMethodError) { }
    arguments.customKlibAbiVersion = this.compilerArguments.customKlibAbiVersion
    arguments.duplicatedUniqueNameStrategy = this.compilerArguments.duplicatedUniqueNameStrategy
    arguments.enableSignatureClashChecks = this.compilerArguments.enableSignatureClashChecks
    arguments.irInlinerBeforeKlibSerialization = this.compilerArguments.irInlinerBeforeKlibSerialization
    try { arguments.setUsingReflection("normalizeAbsolutePath", this.compilerArguments.getUsingReflection<Boolean>("normalizeAbsolutePath")) } catch (_: NoSuchMethodError) { }
    arguments.relativePathBases = this.compilerArguments.relativePathBases
    arguments.klibZipFileAccessorCacheLimit = this.compilerArguments.klibZipFileAccessorCacheLimit
    arguments.partialLinkageMode = this.compilerArguments.partialLinkageMode
    arguments.partialLinkageLogLevel = this.compilerArguments.partialLinkageLogLevel
    arguments.skipLibrarySpecialCompatibilityChecks = this.compilerArguments.skipLibrarySpecialCompatibilityChecks
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

    public val X_FAKE_OVERRIDE_VALIDATOR: CommonKlibBasedArgument<Boolean> =
        CommonKlibBasedArgument("X_FAKE_OVERRIDE_VALIDATOR")

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
