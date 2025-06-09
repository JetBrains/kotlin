package org.jetbrains.kotlin.buildtools.`internal`.v2

import kotlin.Any
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import org.jetbrains.kotlin.buildtools.api.v2.CommonKlibBasedArguments
import org.jetbrains.kotlin.buildtools.api.v2.CommonKlibBasedArguments.Companion.XKLIB_ABI_VERSION
import org.jetbrains.kotlin.buildtools.api.v2.CommonKlibBasedArguments.Companion.XKLIB_DUPLICATED_UNIQUE_NAME_STRATEGY
import org.jetbrains.kotlin.buildtools.api.v2.CommonKlibBasedArguments.Companion.XKLIB_ENABLE_SIGNATURE_CLASH_CHECKS
import org.jetbrains.kotlin.buildtools.api.v2.CommonKlibBasedArguments.Companion.XKLIB_IR_INLINER
import org.jetbrains.kotlin.buildtools.api.v2.CommonKlibBasedArguments.Companion.XKLIB_NORMALIZE_ABSOLUTE_PATH
import org.jetbrains.kotlin.buildtools.api.v2.CommonKlibBasedArguments.Companion.XKLIB_RELATIVE_PATH_BASE
import org.jetbrains.kotlin.buildtools.api.v2.CommonKlibBasedArguments.Companion.XPARTIAL_LINKAGE
import org.jetbrains.kotlin.buildtools.api.v2.CommonKlibBasedArguments.Companion.XPARTIAL_LINKAGE_LOGLEVEL
import org.jetbrains.kotlin.cli.common.arguments.CommonKlibBasedCompilerArguments

public open class CommonKlibBasedArgumentsImpl : CommonCompilerArgumentsImpl(),
    CommonKlibBasedArguments {
  private val optionsMap: MutableMap<CommonKlibBasedArguments.CommonKlibBasedArgument<*>, Any?> =
      mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  override operator fun <V> `get`(key: CommonKlibBasedArguments.CommonKlibBasedArgument<V>): V = optionsMap[key] as V

  override operator fun <V> `set`(key: CommonKlibBasedArguments.CommonKlibBasedArgument<V>, `value`: V) {
    optionsMap[key] = `value`
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: CommonKlibBasedCompilerArguments): CommonKlibBasedCompilerArguments {
    if (XKLIB_RELATIVE_PATH_BASE in optionsMap) { arguments.relativePathBases = get(XKLIB_RELATIVE_PATH_BASE)?.map{ it.toString() }?.toTypedArray() }
    if (XKLIB_NORMALIZE_ABSOLUTE_PATH in optionsMap) { arguments.normalizeAbsolutePath = get(XKLIB_NORMALIZE_ABSOLUTE_PATH) }
    if (XKLIB_ENABLE_SIGNATURE_CLASH_CHECKS in optionsMap) { arguments.enableSignatureClashChecks = get(XKLIB_ENABLE_SIGNATURE_CLASH_CHECKS) }
    if (XPARTIAL_LINKAGE in optionsMap) { arguments.partialLinkageMode = get(XPARTIAL_LINKAGE) }
    if (XPARTIAL_LINKAGE_LOGLEVEL in optionsMap) { arguments.partialLinkageLogLevel = get(XPARTIAL_LINKAGE_LOGLEVEL) }
    if (XKLIB_DUPLICATED_UNIQUE_NAME_STRATEGY in optionsMap) { arguments.duplicatedUniqueNameStrategy = get(XKLIB_DUPLICATED_UNIQUE_NAME_STRATEGY) }
    if (XKLIB_IR_INLINER in optionsMap) { arguments.irInlinerBeforeKlibSerialization = get(XKLIB_IR_INLINER) }
    if (XKLIB_ABI_VERSION in optionsMap) { arguments.customKlibAbiVersion = get(XKLIB_ABI_VERSION) }
    return arguments
  }
}
