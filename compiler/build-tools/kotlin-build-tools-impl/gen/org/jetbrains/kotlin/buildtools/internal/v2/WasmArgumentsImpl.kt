package org.jetbrains.kotlin.buildtools.`internal`.v2

import kotlin.Any
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.XIR_DCE_DUMP_REACHABILITY_INFO_TO_FILE
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.XIR_DUMP_DECLARATION_IR_SIZES_TO_FILE
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.XWASM
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.XWASM_ATTACH_JS_EXCEPTION
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.XWASM_DEBUGGER_CUSTOM_FORMATTERS
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.XWASM_DEBUG_FRIENDLY
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.XWASM_DEBUG_INFO
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.XWASM_ENABLE_ARRAY_RANGE_CHECKS
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.XWASM_ENABLE_ASSERTS
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.XWASM_GENERATE_DWARF
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.XWASM_GENERATE_WAT
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.XWASM_IC_CACHE_READONLY
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.XWASM_KCLASS_FQN
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.XWASM_PRESERVE_IC_ORDER
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.XWASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.XWASM_TARGET
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.XWASM_USE_NEW_EXCEPTION_PROPOSAL
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.XWASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS
import org.jetbrains.kotlin.cli.common.arguments.K2WasmCompilerArguments

public open class WasmArgumentsImpl : CommonKlibBasedArgumentsImpl(), WasmArguments {
  private val optionsMap: MutableMap<WasmArguments.WasmArgument<*>, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  override operator fun <V> `get`(key: WasmArguments.WasmArgument<V>): V = optionsMap[key] as V

  override operator fun <V> `set`(key: WasmArguments.WasmArgument<V>, `value`: V) {
    optionsMap[key] = `value`
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: K2WasmCompilerArguments): K2WasmCompilerArguments {
    if (XWASM in optionsMap) { arguments.wasm = get(XWASM) }
    if (XWASM_TARGET in optionsMap) { arguments.wasmTarget = get(XWASM_TARGET) }
    if (XWASM_DEBUG_INFO in optionsMap) { arguments.wasmDebug = get(XWASM_DEBUG_INFO) }
    if (XWASM_DEBUG_FRIENDLY in optionsMap) { arguments.forceDebugFriendlyCompilation = get(XWASM_DEBUG_FRIENDLY) }
    if (XWASM_GENERATE_WAT in optionsMap) { arguments.wasmGenerateWat = get(XWASM_GENERATE_WAT) }
    if (XWASM_KCLASS_FQN in optionsMap) { arguments.wasmKClassFqn = get(XWASM_KCLASS_FQN) }
    if (XWASM_ENABLE_ARRAY_RANGE_CHECKS in optionsMap) { arguments.wasmEnableArrayRangeChecks = get(XWASM_ENABLE_ARRAY_RANGE_CHECKS) }
    if (XWASM_ENABLE_ASSERTS in optionsMap) { arguments.wasmEnableAsserts = get(XWASM_ENABLE_ASSERTS) }
    if (XWASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS in optionsMap) { arguments.wasmUseTrapsInsteadOfExceptions = get(XWASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS) }
    if (XWASM_USE_NEW_EXCEPTION_PROPOSAL in optionsMap) { arguments.wasmUseNewExceptionProposal = get(XWASM_USE_NEW_EXCEPTION_PROPOSAL) }
    if (XWASM_ATTACH_JS_EXCEPTION in optionsMap) { arguments.wasmUseJsTag = get(XWASM_ATTACH_JS_EXCEPTION) }
    if (XWASM_DEBUGGER_CUSTOM_FORMATTERS in optionsMap) { arguments.debuggerCustomFormatters = get(XWASM_DEBUGGER_CUSTOM_FORMATTERS) }
    if (XWASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES in optionsMap) { arguments.includeUnavailableSourcesIntoSourceMap = get(XWASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES) }
    if (XWASM_PRESERVE_IC_ORDER in optionsMap) { arguments.preserveIcOrder = get(XWASM_PRESERVE_IC_ORDER) }
    if (XWASM_IC_CACHE_READONLY in optionsMap) { arguments.icCacheReadonly = get(XWASM_IC_CACHE_READONLY) }
    if (XWASM_GENERATE_DWARF in optionsMap) { arguments.generateDwarf = get(XWASM_GENERATE_DWARF) }
    if (XIR_DCE_DUMP_REACHABILITY_INFO_TO_FILE in optionsMap) { arguments.irDceDumpReachabilityInfoToFile = get(XIR_DCE_DUMP_REACHABILITY_INFO_TO_FILE) }
    if (XIR_DUMP_DECLARATION_IR_SIZES_TO_FILE in optionsMap) { arguments.irDceDumpDeclarationIrSizesToFile = get(XIR_DUMP_DECLARATION_IR_SIZES_TO_FILE) }
    return arguments
  }
}
