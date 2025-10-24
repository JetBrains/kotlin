// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import java.lang.IllegalStateException
import kotlin.Any
import kotlin.Boolean
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.WasmArgumentsImpl.Companion.X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.WasmArgumentsImpl.Companion.X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.WasmArgumentsImpl.Companion.X_WASM
import org.jetbrains.kotlin.buildtools.`internal`.arguments.WasmArgumentsImpl.Companion.X_WASM_DEBUGGER_CUSTOM_FORMATTERS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.WasmArgumentsImpl.Companion.X_WASM_DEBUG_FRIENDLY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.WasmArgumentsImpl.Companion.X_WASM_DEBUG_INFO
import org.jetbrains.kotlin.buildtools.`internal`.arguments.WasmArgumentsImpl.Companion.X_WASM_ENABLE_ARRAY_RANGE_CHECKS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.WasmArgumentsImpl.Companion.X_WASM_ENABLE_ASSERTS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.WasmArgumentsImpl.Companion.X_WASM_GENERATE_DWARF
import org.jetbrains.kotlin.buildtools.`internal`.arguments.WasmArgumentsImpl.Companion.X_WASM_GENERATE_WAT
import org.jetbrains.kotlin.buildtools.`internal`.arguments.WasmArgumentsImpl.Companion.X_WASM_IC_CACHE_READONLY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.WasmArgumentsImpl.Companion.X_WASM_INCLUDED_MODULE_ONLY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.WasmArgumentsImpl.Companion.X_WASM_KCLASS_FQN
import org.jetbrains.kotlin.buildtools.`internal`.arguments.WasmArgumentsImpl.Companion.X_WASM_NO_JSTAG
import org.jetbrains.kotlin.buildtools.`internal`.arguments.WasmArgumentsImpl.Companion.X_WASM_PRESERVE_IC_ORDER
import org.jetbrains.kotlin.buildtools.`internal`.arguments.WasmArgumentsImpl.Companion.X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.WasmArgumentsImpl.Companion.X_WASM_TARGET
import org.jetbrains.kotlin.buildtools.`internal`.arguments.WasmArgumentsImpl.Companion.X_WASM_USE_NEW_EXCEPTION_PROPOSAL
import org.jetbrains.kotlin.buildtools.`internal`.arguments.WasmArgumentsImpl.Companion.X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.WasmArguments
import org.jetbrains.kotlin.cli.common.arguments.K2WasmCompilerArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal abstract class WasmArgumentsImpl : CommonKlibBasedArgumentsImpl(), WasmArguments {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: WasmArguments.WasmArgument<V>): V = optionsMap[key.id] as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: WasmArguments.WasmArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 3, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    optionsMap[key.id] = `value`
  }

  override operator fun contains(key: WasmArguments.WasmArgument<*>): Boolean = key.id in optionsMap

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: WasmArgument<V>): V = optionsMap[key.id] as V

  public operator fun <V> `set`(key: WasmArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: WasmArgument<*>): Boolean = key.id in optionsMap

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: K2WasmCompilerArguments): K2WasmCompilerArguments {
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys.filter { it !in knownArguments }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    if (X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE in this) { arguments.irDceDumpReachabilityInfoToFile = get(X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE)}
    if (X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE in this) { arguments.irDceDumpDeclarationIrSizesToFile = get(X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE)}
    if (X_WASM in this) { arguments.wasm = get(X_WASM)}
    if (X_WASM_DEBUG_FRIENDLY in this) { arguments.forceDebugFriendlyCompilation = get(X_WASM_DEBUG_FRIENDLY)}
    if (X_WASM_DEBUG_INFO in this) { arguments.wasmDebug = get(X_WASM_DEBUG_INFO)}
    if (X_WASM_DEBUGGER_CUSTOM_FORMATTERS in this) { arguments.debuggerCustomFormatters = get(X_WASM_DEBUGGER_CUSTOM_FORMATTERS)}
    if (X_WASM_ENABLE_ARRAY_RANGE_CHECKS in this) { arguments.wasmEnableArrayRangeChecks = get(X_WASM_ENABLE_ARRAY_RANGE_CHECKS)}
    if (X_WASM_ENABLE_ASSERTS in this) { arguments.wasmEnableAsserts = get(X_WASM_ENABLE_ASSERTS)}
    if (X_WASM_GENERATE_DWARF in this) { arguments.generateDwarf = get(X_WASM_GENERATE_DWARF)}
    if (X_WASM_GENERATE_WAT in this) { arguments.wasmGenerateWat = get(X_WASM_GENERATE_WAT)}
    if (X_WASM_IC_CACHE_READONLY in this) { arguments.icCacheReadonly = get(X_WASM_IC_CACHE_READONLY)}
    if (X_WASM_INCLUDED_MODULE_ONLY in this) { arguments.wasmIncludedModuleOnly = get(X_WASM_INCLUDED_MODULE_ONLY)}
    if (X_WASM_KCLASS_FQN in this) { arguments.wasmKClassFqn = get(X_WASM_KCLASS_FQN)}
    if (X_WASM_NO_JSTAG in this) { arguments.wasmNoJsTag = get(X_WASM_NO_JSTAG)}
    if (X_WASM_PRESERVE_IC_ORDER in this) { arguments.preserveIcOrder = get(X_WASM_PRESERVE_IC_ORDER)}
    if (X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES in this) { arguments.includeUnavailableSourcesIntoSourceMap = get(X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES)}
    if (X_WASM_TARGET in this) { arguments.wasmTarget = get(X_WASM_TARGET)}
    if (X_WASM_USE_NEW_EXCEPTION_PROPOSAL in this) { arguments.wasmUseNewExceptionProposal = get(X_WASM_USE_NEW_EXCEPTION_PROPOSAL)}
    if (X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS in this) { arguments.wasmUseTrapsInsteadOfExceptions = get(X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS)}
    return arguments
  }

  @Suppress("DEPRECATION")
  public fun applyCompilerArguments(arguments: K2WasmCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { this[X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE] = arguments.irDceDumpReachabilityInfoToFile } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE] = arguments.irDceDumpDeclarationIrSizesToFile } catch (_: NoSuchMethodError) {  }
    try { this[X_WASM] = arguments.wasm } catch (_: NoSuchMethodError) {  }
    try { this[X_WASM_DEBUG_FRIENDLY] = arguments.forceDebugFriendlyCompilation } catch (_: NoSuchMethodError) {  }
    try { this[X_WASM_DEBUG_INFO] = arguments.wasmDebug } catch (_: NoSuchMethodError) {  }
    try { this[X_WASM_DEBUGGER_CUSTOM_FORMATTERS] = arguments.debuggerCustomFormatters } catch (_: NoSuchMethodError) {  }
    try { this[X_WASM_ENABLE_ARRAY_RANGE_CHECKS] = arguments.wasmEnableArrayRangeChecks } catch (_: NoSuchMethodError) {  }
    try { this[X_WASM_ENABLE_ASSERTS] = arguments.wasmEnableAsserts } catch (_: NoSuchMethodError) {  }
    try { this[X_WASM_GENERATE_DWARF] = arguments.generateDwarf } catch (_: NoSuchMethodError) {  }
    try { this[X_WASM_GENERATE_WAT] = arguments.wasmGenerateWat } catch (_: NoSuchMethodError) {  }
    try { this[X_WASM_IC_CACHE_READONLY] = arguments.icCacheReadonly } catch (_: NoSuchMethodError) {  }
    try { this[X_WASM_INCLUDED_MODULE_ONLY] = arguments.wasmIncludedModuleOnly } catch (_: NoSuchMethodError) {  }
    try { this[X_WASM_KCLASS_FQN] = arguments.wasmKClassFqn } catch (_: NoSuchMethodError) {  }
    try { this[X_WASM_NO_JSTAG] = arguments.wasmNoJsTag } catch (_: NoSuchMethodError) {  }
    try { this[X_WASM_PRESERVE_IC_ORDER] = arguments.preserveIcOrder } catch (_: NoSuchMethodError) {  }
    try { this[X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES] = arguments.includeUnavailableSourcesIntoSourceMap } catch (_: NoSuchMethodError) {  }
    try { this[X_WASM_TARGET] = arguments.wasmTarget } catch (_: NoSuchMethodError) {  }
    try { this[X_WASM_USE_NEW_EXCEPTION_PROPOSAL] = arguments.wasmUseNewExceptionProposal } catch (_: NoSuchMethodError) {  }
    try { this[X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS] = arguments.wasmUseTrapsInsteadOfExceptions } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  public class WasmArgument<V>(
    public val id: String,
  ) {
    init {
      knownArguments.add(id)}
  }

  public companion object {
    private val knownArguments: MutableSet<String> = mutableSetOf()

    public val X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE: WasmArgument<String?> =
        WasmArgument("X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE")

    public val X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE: WasmArgument<String?> =
        WasmArgument("X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE")

    public val X_WASM: WasmArgument<Boolean> = WasmArgument("X_WASM")

    public val X_WASM_DEBUG_FRIENDLY: WasmArgument<Boolean> = WasmArgument("X_WASM_DEBUG_FRIENDLY")

    public val X_WASM_DEBUG_INFO: WasmArgument<Boolean> = WasmArgument("X_WASM_DEBUG_INFO")

    public val X_WASM_DEBUGGER_CUSTOM_FORMATTERS: WasmArgument<Boolean> =
        WasmArgument("X_WASM_DEBUGGER_CUSTOM_FORMATTERS")

    public val X_WASM_ENABLE_ARRAY_RANGE_CHECKS: WasmArgument<Boolean> =
        WasmArgument("X_WASM_ENABLE_ARRAY_RANGE_CHECKS")

    public val X_WASM_ENABLE_ASSERTS: WasmArgument<Boolean> = WasmArgument("X_WASM_ENABLE_ASSERTS")

    public val X_WASM_GENERATE_DWARF: WasmArgument<Boolean> = WasmArgument("X_WASM_GENERATE_DWARF")

    public val X_WASM_GENERATE_WAT: WasmArgument<Boolean> = WasmArgument("X_WASM_GENERATE_WAT")

    public val X_WASM_IC_CACHE_READONLY: WasmArgument<Boolean> =
        WasmArgument("X_WASM_IC_CACHE_READONLY")

    public val X_WASM_INCLUDED_MODULE_ONLY: WasmArgument<Boolean> =
        WasmArgument("X_WASM_INCLUDED_MODULE_ONLY")

    public val X_WASM_KCLASS_FQN: WasmArgument<Boolean> = WasmArgument("X_WASM_KCLASS_FQN")

    public val X_WASM_NO_JSTAG: WasmArgument<Boolean> = WasmArgument("X_WASM_NO_JSTAG")

    public val X_WASM_PRESERVE_IC_ORDER: WasmArgument<Boolean> =
        WasmArgument("X_WASM_PRESERVE_IC_ORDER")

    public val X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES: WasmArgument<Boolean> =
        WasmArgument("X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES")

    public val X_WASM_TARGET: WasmArgument<String?> = WasmArgument("X_WASM_TARGET")

    public val X_WASM_USE_NEW_EXCEPTION_PROPOSAL: WasmArgument<Boolean?> =
        WasmArgument("X_WASM_USE_NEW_EXCEPTION_PROPOSAL")

    public val X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS: WasmArgument<Boolean> =
        WasmArgument("X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS")
  }
}
