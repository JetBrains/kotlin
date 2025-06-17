@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.v2

import kotlin.Any
import kotlin.Boolean
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import org.jetbrains.kotlin.buildtools.api.v2.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.X_WASM
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.X_WASM_ATTACH_JS_EXCEPTION
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.X_WASM_DEBUGGER_CUSTOM_FORMATTERS
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.X_WASM_DEBUG_FRIENDLY
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.X_WASM_DEBUG_INFO
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.X_WASM_ENABLE_ARRAY_RANGE_CHECKS
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.X_WASM_ENABLE_ASSERTS
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.X_WASM_GENERATE_DWARF
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.X_WASM_GENERATE_WAT
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.X_WASM_IC_CACHE_READONLY
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.X_WASM_KCLASS_FQN
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.X_WASM_PRESERVE_IC_ORDER
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.X_WASM_TARGET
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.X_WASM_USE_NEW_EXCEPTION_PROPOSAL
import org.jetbrains.kotlin.buildtools.api.v2.WasmArguments.Companion.X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS
import org.jetbrains.kotlin.cli.common.arguments.K2WasmCompilerArguments

public open class WasmArgumentsImpl : CommonKlibBasedArgumentsImpl(), WasmArguments {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  override operator fun <V> `get`(key: WasmArguments.WasmArgument<V>): V = optionsMap[key.id] as V

  override operator fun <V> `set`(key: WasmArguments.WasmArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: WasmArguments.WasmArgument<*>): Boolean = key.id in optionsMap

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: K2WasmCompilerArguments): K2WasmCompilerArguments {
    super.toCompilerArguments(arguments)
    if ("X_WASM" in optionsMap) { arguments.wasm = get(X_WASM) }
    if ("X_WASM_TARGET" in optionsMap) { arguments.wasmTarget = get(X_WASM_TARGET) }
    if ("X_WASM_DEBUG_INFO" in optionsMap) { arguments.wasmDebug = get(X_WASM_DEBUG_INFO) }
    if ("X_WASM_DEBUG_FRIENDLY" in optionsMap) { arguments.forceDebugFriendlyCompilation = get(X_WASM_DEBUG_FRIENDLY) }
    if ("X_WASM_GENERATE_WAT" in optionsMap) { arguments.wasmGenerateWat = get(X_WASM_GENERATE_WAT) }
    if ("X_WASM_KCLASS_FQN" in optionsMap) { arguments.wasmKClassFqn = get(X_WASM_KCLASS_FQN) }
    if ("X_WASM_ENABLE_ARRAY_RANGE_CHECKS" in optionsMap) { arguments.wasmEnableArrayRangeChecks = get(X_WASM_ENABLE_ARRAY_RANGE_CHECKS) }
    if ("X_WASM_ENABLE_ASSERTS" in optionsMap) { arguments.wasmEnableAsserts = get(X_WASM_ENABLE_ASSERTS) }
    if ("X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS" in optionsMap) { arguments.wasmUseTrapsInsteadOfExceptions = get(X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS) }
    if ("X_WASM_USE_NEW_EXCEPTION_PROPOSAL" in optionsMap) { arguments.wasmUseNewExceptionProposal = get(X_WASM_USE_NEW_EXCEPTION_PROPOSAL) }
    if ("X_WASM_ATTACH_JS_EXCEPTION" in optionsMap) { arguments.wasmUseJsTag = get(X_WASM_ATTACH_JS_EXCEPTION) }
    if ("X_WASM_DEBUGGER_CUSTOM_FORMATTERS" in optionsMap) { arguments.debuggerCustomFormatters = get(X_WASM_DEBUGGER_CUSTOM_FORMATTERS) }
    if ("X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES" in optionsMap) { arguments.includeUnavailableSourcesIntoSourceMap = get(X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES) }
    if ("X_WASM_PRESERVE_IC_ORDER" in optionsMap) { arguments.preserveIcOrder = get(X_WASM_PRESERVE_IC_ORDER) }
    if ("X_WASM_IC_CACHE_READONLY" in optionsMap) { arguments.icCacheReadonly = get(X_WASM_IC_CACHE_READONLY) }
    if ("X_WASM_GENERATE_DWARF" in optionsMap) { arguments.generateDwarf = get(X_WASM_GENERATE_DWARF) }
    if ("X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE" in optionsMap) { arguments.irDceDumpReachabilityInfoToFile = get(X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE) }
    if ("X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE" in optionsMap) { arguments.irDceDumpDeclarationIrSizesToFile = get(X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE) }
    return arguments
  }

  @Suppress("DEPRECATION")
  @OptIn(ExperimentalCompilerArgument::class)
  override fun toArgumentStrings(): List<String> {
    val arguments = mutableListOf<String>()
    arguments.addAll(super.toArgumentStrings())
    if ("X_WASM" in optionsMap) { arguments.add("-Xwasm=" + get(X_WASM)) }
    if ("X_WASM_TARGET" in optionsMap) { arguments.add("-Xwasm-target=" + get(X_WASM_TARGET)) }
    if ("X_WASM_DEBUG_INFO" in optionsMap) { arguments.add("-Xwasm-debug-info=" + get(X_WASM_DEBUG_INFO)) }
    if ("X_WASM_DEBUG_FRIENDLY" in optionsMap) { arguments.add("-Xwasm-debug-friendly=" + get(X_WASM_DEBUG_FRIENDLY)) }
    if ("X_WASM_GENERATE_WAT" in optionsMap) { arguments.add("-Xwasm-generate-wat=" + get(X_WASM_GENERATE_WAT)) }
    if ("X_WASM_KCLASS_FQN" in optionsMap) { arguments.add("-Xwasm-kclass-fqn=" + get(X_WASM_KCLASS_FQN)) }
    if ("X_WASM_ENABLE_ARRAY_RANGE_CHECKS" in optionsMap) { arguments.add("-Xwasm-enable-array-range-checks=" + get(X_WASM_ENABLE_ARRAY_RANGE_CHECKS)) }
    if ("X_WASM_ENABLE_ASSERTS" in optionsMap) { arguments.add("-Xwasm-enable-asserts=" + get(X_WASM_ENABLE_ASSERTS)) }
    if ("X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS" in optionsMap) { arguments.add("-Xwasm-use-traps-instead-of-exceptions=" + get(X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS)) }
    if ("X_WASM_USE_NEW_EXCEPTION_PROPOSAL" in optionsMap) { arguments.add("-Xwasm-use-new-exception-proposal=" + get(X_WASM_USE_NEW_EXCEPTION_PROPOSAL)) }
    if ("X_WASM_ATTACH_JS_EXCEPTION" in optionsMap) { arguments.add("-Xwasm-attach-js-exception=" + get(X_WASM_ATTACH_JS_EXCEPTION)) }
    if ("X_WASM_DEBUGGER_CUSTOM_FORMATTERS" in optionsMap) { arguments.add("-Xwasm-debugger-custom-formatters=" + get(X_WASM_DEBUGGER_CUSTOM_FORMATTERS)) }
    if ("X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES" in optionsMap) { arguments.add("-Xwasm-source-map-include-mappings-from-unavailable-sources=" + get(X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES)) }
    if ("X_WASM_PRESERVE_IC_ORDER" in optionsMap) { arguments.add("-Xwasm-preserve-ic-order=" + get(X_WASM_PRESERVE_IC_ORDER)) }
    if ("X_WASM_IC_CACHE_READONLY" in optionsMap) { arguments.add("-Xwasm-ic-cache-readonly=" + get(X_WASM_IC_CACHE_READONLY)) }
    if ("X_WASM_GENERATE_DWARF" in optionsMap) { arguments.add("-Xwasm-generate-dwarf=" + get(X_WASM_GENERATE_DWARF)) }
    if ("X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE" in optionsMap) { arguments.add("-Xir-dce-dump-reachability-info-to-file=" + get(X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE)) }
    if ("X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE" in optionsMap) { arguments.add("-Xir-dump-declaration-ir-sizes-to-file=" + get(X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE)) }
    return arguments
  }
}
