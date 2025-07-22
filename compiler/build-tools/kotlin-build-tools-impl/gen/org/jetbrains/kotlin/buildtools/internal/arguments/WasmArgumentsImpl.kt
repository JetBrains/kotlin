// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import kotlin.Any
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.WasmArguments
import org.jetbrains.kotlin.buildtools.api.arguments.WasmArguments.Companion.X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE
import org.jetbrains.kotlin.buildtools.api.arguments.WasmArguments.Companion.X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE
import org.jetbrains.kotlin.buildtools.api.arguments.WasmArguments.Companion.X_WASM
import org.jetbrains.kotlin.buildtools.api.arguments.WasmArguments.Companion.X_WASM_DEBUGGER_CUSTOM_FORMATTERS
import org.jetbrains.kotlin.buildtools.api.arguments.WasmArguments.Companion.X_WASM_DEBUG_FRIENDLY
import org.jetbrains.kotlin.buildtools.api.arguments.WasmArguments.Companion.X_WASM_DEBUG_INFO
import org.jetbrains.kotlin.buildtools.api.arguments.WasmArguments.Companion.X_WASM_ENABLE_ARRAY_RANGE_CHECKS
import org.jetbrains.kotlin.buildtools.api.arguments.WasmArguments.Companion.X_WASM_ENABLE_ASSERTS
import org.jetbrains.kotlin.buildtools.api.arguments.WasmArguments.Companion.X_WASM_GENERATE_DWARF
import org.jetbrains.kotlin.buildtools.api.arguments.WasmArguments.Companion.X_WASM_GENERATE_WAT
import org.jetbrains.kotlin.buildtools.api.arguments.WasmArguments.Companion.X_WASM_IC_CACHE_READONLY
import org.jetbrains.kotlin.buildtools.api.arguments.WasmArguments.Companion.X_WASM_KCLASS_FQN
import org.jetbrains.kotlin.buildtools.api.arguments.WasmArguments.Companion.X_WASM_NO_JSTAG
import org.jetbrains.kotlin.buildtools.api.arguments.WasmArguments.Companion.X_WASM_PRESERVE_IC_ORDER
import org.jetbrains.kotlin.buildtools.api.arguments.WasmArguments.Companion.X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES
import org.jetbrains.kotlin.buildtools.api.arguments.WasmArguments.Companion.X_WASM_TARGET
import org.jetbrains.kotlin.buildtools.api.arguments.WasmArguments.Companion.X_WASM_USE_NEW_EXCEPTION_PROPOSAL
import org.jetbrains.kotlin.buildtools.api.arguments.WasmArguments.Companion.X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS
import org.jetbrains.kotlin.cli.common.arguments.K2WasmCompilerArguments

public open class WasmArgumentsImpl : CommonKlibBasedArgumentsImpl(), WasmArguments {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  override operator fun <V> `get`(key: WasmArguments.WasmArgument<V>): V = optionsMap[key.id] as V

  override operator fun <V> `set`(key: WasmArguments.WasmArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

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
    if ("X_WASM_NO_JSTAG" in optionsMap) { arguments.wasmNoJsTag = get(X_WASM_NO_JSTAG) }
    if ("X_WASM_DEBUGGER_CUSTOM_FORMATTERS" in optionsMap) { arguments.debuggerCustomFormatters = get(X_WASM_DEBUGGER_CUSTOM_FORMATTERS) }
    if ("X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES" in optionsMap) { arguments.includeUnavailableSourcesIntoSourceMap = get(X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES) }
    if ("X_WASM_PRESERVE_IC_ORDER" in optionsMap) { arguments.preserveIcOrder = get(X_WASM_PRESERVE_IC_ORDER) }
    if ("X_WASM_IC_CACHE_READONLY" in optionsMap) { arguments.icCacheReadonly = get(X_WASM_IC_CACHE_READONLY) }
    if ("X_WASM_GENERATE_DWARF" in optionsMap) { arguments.generateDwarf = get(X_WASM_GENERATE_DWARF) }
    if ("X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE" in optionsMap) { arguments.irDceDumpReachabilityInfoToFile = get(X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE) }
    if ("X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE" in optionsMap) { arguments.irDceDumpDeclarationIrSizesToFile = get(X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE) }
    return arguments
  }
}
