// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import kotlin.Any
import kotlin.Boolean
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
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
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: WasmArguments.WasmArgument<V>): V = optionsMap[key.id] as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: WasmArguments.WasmArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: WasmArgument<V>): V = optionsMap[key.id] as V

  public operator fun <V> `set`(key: WasmArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: WasmArgument<*>): Boolean = key.id in optionsMap

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

  /**
   * Base class for [WasmArguments] options.
   *
   * @see get
   * @see set    
   */
  public class WasmArgument<V>(
    public val id: String,
  )

  public companion object {
    public val X_WASM: WasmArgument<Boolean> = WasmArgument("X_WASM")

    public val X_WASM_TARGET: WasmArgument<String?> = WasmArgument("X_WASM_TARGET")

    public val X_WASM_DEBUG_INFO: WasmArgument<Boolean> = WasmArgument("X_WASM_DEBUG_INFO")

    public val X_WASM_DEBUG_FRIENDLY: WasmArgument<Boolean> = WasmArgument("X_WASM_DEBUG_FRIENDLY")

    public val X_WASM_GENERATE_WAT: WasmArgument<Boolean> = WasmArgument("X_WASM_GENERATE_WAT")

    public val X_WASM_KCLASS_FQN: WasmArgument<Boolean> = WasmArgument("X_WASM_KCLASS_FQN")

    public val X_WASM_ENABLE_ARRAY_RANGE_CHECKS: WasmArgument<Boolean> =
        WasmArgument("X_WASM_ENABLE_ARRAY_RANGE_CHECKS")

    public val X_WASM_ENABLE_ASSERTS: WasmArgument<Boolean> = WasmArgument("X_WASM_ENABLE_ASSERTS")

    public val X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS: WasmArgument<Boolean> =
        WasmArgument("X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS")

    public val X_WASM_USE_NEW_EXCEPTION_PROPOSAL: WasmArgument<Boolean> =
        WasmArgument("X_WASM_USE_NEW_EXCEPTION_PROPOSAL")

    public val X_WASM_NO_JSTAG: WasmArgument<Boolean> = WasmArgument("X_WASM_NO_JSTAG")

    public val X_WASM_DEBUGGER_CUSTOM_FORMATTERS: WasmArgument<Boolean> =
        WasmArgument("X_WASM_DEBUGGER_CUSTOM_FORMATTERS")

    public val X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES: WasmArgument<Boolean> =
        WasmArgument("X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES")

    public val X_WASM_PRESERVE_IC_ORDER: WasmArgument<Boolean> =
        WasmArgument("X_WASM_PRESERVE_IC_ORDER")

    public val X_WASM_IC_CACHE_READONLY: WasmArgument<Boolean> =
        WasmArgument("X_WASM_IC_CACHE_READONLY")

    public val X_WASM_GENERATE_DWARF: WasmArgument<Boolean> = WasmArgument("X_WASM_GENERATE_DWARF")

    public val X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE: WasmArgument<String?> =
        WasmArgument("X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE")

    public val X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE: WasmArgument<String?> =
        WasmArgument("X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE")
  }
}
