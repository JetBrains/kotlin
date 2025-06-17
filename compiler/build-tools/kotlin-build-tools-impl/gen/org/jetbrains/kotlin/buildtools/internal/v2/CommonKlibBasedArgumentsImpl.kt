@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.v2

import kotlin.Any
import kotlin.Boolean
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import org.jetbrains.kotlin.buildtools.api.v2.CommonKlibBasedArguments
import org.jetbrains.kotlin.buildtools.api.v2.CommonKlibBasedArguments.Companion.X_KLIB_ABI_VERSION
import org.jetbrains.kotlin.buildtools.api.v2.CommonKlibBasedArguments.Companion.X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY
import org.jetbrains.kotlin.buildtools.api.v2.CommonKlibBasedArguments.Companion.X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS
import org.jetbrains.kotlin.buildtools.api.v2.CommonKlibBasedArguments.Companion.X_KLIB_IR_INLINER
import org.jetbrains.kotlin.buildtools.api.v2.CommonKlibBasedArguments.Companion.X_KLIB_NORMALIZE_ABSOLUTE_PATH
import org.jetbrains.kotlin.buildtools.api.v2.CommonKlibBasedArguments.Companion.X_KLIB_RELATIVE_PATH_BASE
import org.jetbrains.kotlin.buildtools.api.v2.CommonKlibBasedArguments.Companion.X_PARTIAL_LINKAGE
import org.jetbrains.kotlin.buildtools.api.v2.CommonKlibBasedArguments.Companion.X_PARTIAL_LINKAGE_LOGLEVEL
import org.jetbrains.kotlin.buildtools.api.v2.ExperimentalCompilerArgument
import org.jetbrains.kotlin.cli.common.arguments.CommonKlibBasedCompilerArguments

public open class CommonKlibBasedArgumentsImpl : CommonCompilerArgumentsImpl(),
    CommonKlibBasedArguments {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  override operator fun <V> `get`(key: CommonKlibBasedArguments.CommonKlibBasedArgument<V>): V = optionsMap[key.id] as V

  override operator fun <V> `set`(key: CommonKlibBasedArguments.CommonKlibBasedArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: CommonKlibBasedArguments.CommonKlibBasedArgument<*>): Boolean = key.id in optionsMap

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: CommonKlibBasedCompilerArguments): CommonKlibBasedCompilerArguments {
    super.toCompilerArguments(arguments)
    if ("X_KLIB_RELATIVE_PATH_BASE" in optionsMap) { arguments.relativePathBases = get(X_KLIB_RELATIVE_PATH_BASE) }
    if ("X_KLIB_NORMALIZE_ABSOLUTE_PATH" in optionsMap) { arguments.normalizeAbsolutePath = get(X_KLIB_NORMALIZE_ABSOLUTE_PATH) }
    if ("X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS" in optionsMap) { arguments.enableSignatureClashChecks = get(X_KLIB_ENABLE_SIGNATURE_CLASH_CHECKS) }
    if ("X_PARTIAL_LINKAGE" in optionsMap) { arguments.partialLinkageMode = get(X_PARTIAL_LINKAGE) }
    if ("X_PARTIAL_LINKAGE_LOGLEVEL" in optionsMap) { arguments.partialLinkageLogLevel = get(X_PARTIAL_LINKAGE_LOGLEVEL) }
    if ("X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY" in optionsMap) { arguments.duplicatedUniqueNameStrategy = get(X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY) }
    if ("X_KLIB_IR_INLINER" in optionsMap) { arguments.irInlinerBeforeKlibSerialization = get(X_KLIB_IR_INLINER) }
    if ("X_KLIB_ABI_VERSION" in optionsMap) { arguments.customKlibAbiVersion = get(X_KLIB_ABI_VERSION) }
    return arguments
  }
}
