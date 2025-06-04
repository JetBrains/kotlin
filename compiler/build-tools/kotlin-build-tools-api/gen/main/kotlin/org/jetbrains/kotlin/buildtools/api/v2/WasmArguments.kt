package org.jetbrains.kotlin.buildtools.api.v2

import kotlin.Any
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.String
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.cli.common.arguments.K2WasmCompilerArguments

public open class WasmArguments : CommonKlibBasedArguments() {
  private val optionsMap: MutableMap<WasmArgument<*>, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: WasmArgument<V>): V = optionsMap[key] as V

  public operator fun <V> `set`(key: WasmArgument<V>, `value`: V) {
    optionsMap[key] = `value`
  }

  public fun toCompilerArguments(): K2WasmCompilerArguments {
    val arguments = K2WasmCompilerArguments()
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

  public class WasmArgument<V>(
    public val id: String,
  )

  public companion object {
    /**
     * Use the WebAssembly compiler backend.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XWASM: WasmArgument<Boolean> = WasmArgument("XWASM")

    /**
     * Set up the Wasm target (wasm-js or wasm-wasi).
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XWASM_TARGET: WasmArgument<String?> = WasmArgument("XWASM_TARGET")

    /**
     * Add debug info to the compiled WebAssembly module.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XWASM_DEBUG_INFO: WasmArgument<Boolean> = WasmArgument("XWASM_DEBUG_INFO")

    /**
     * Avoid optimizations that can break debugging.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XWASM_DEBUG_FRIENDLY: WasmArgument<Boolean> = WasmArgument("XWASM_DEBUG_FRIENDLY")

    /**
     * Generate a .wat file.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XWASM_GENERATE_WAT: WasmArgument<Boolean> = WasmArgument("XWASM_GENERATE_WAT")

    /**
     * Enable support for 'KClass.qualifiedName'.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XWASM_KCLASS_FQN: WasmArgument<Boolean> = WasmArgument("XWASM_KCLASS_FQN")

    /**
     * Turn on range checks for array access functions.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XWASM_ENABLE_ARRAY_RANGE_CHECKS: WasmArgument<Boolean> =
        WasmArgument("XWASM_ENABLE_ARRAY_RANGE_CHECKS")

    /**
     * Turn on asserts.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XWASM_ENABLE_ASSERTS: WasmArgument<Boolean> = WasmArgument("XWASM_ENABLE_ASSERTS")

    /**
     * Use traps instead of throwing exceptions.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XWASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS: WasmArgument<Boolean> =
        WasmArgument("XWASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS")

    /**
     * Use an updated version of the exception proposal with try_table.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XWASM_USE_NEW_EXCEPTION_PROPOSAL: WasmArgument<Boolean> =
        WasmArgument("XWASM_USE_NEW_EXCEPTION_PROPOSAL")

    /**
     * Attach a thrown by JS-value to the JsException class
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XWASM_ATTACH_JS_EXCEPTION: WasmArgument<Boolean> =
        WasmArgument("XWASM_ATTACH_JS_EXCEPTION")

    /**
     * Generates devtools custom formatters (https://firefox-source-docs.mozilla.org/devtools-user/custom_formatters) for Kotlin/Wasm values
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XWASM_DEBUGGER_CUSTOM_FORMATTERS: WasmArgument<Boolean> =
        WasmArgument("XWASM_DEBUGGER_CUSTOM_FORMATTERS")

    /**
     * Insert source mappings from libraries even if their sources are unavailable on the end-user machine.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XWASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES: WasmArgument<Boolean> =
        WasmArgument("XWASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES")

    /**
     * Preserve wasm file structure between IC runs.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XWASM_PRESERVE_IC_ORDER: WasmArgument<Boolean> =
        WasmArgument("XWASM_PRESERVE_IC_ORDER")

    /**
     * Do not commit IC cache updates.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XWASM_IC_CACHE_READONLY: WasmArgument<Boolean> =
        WasmArgument("XWASM_IC_CACHE_READONLY")

    /**
     * Generate DWARF debug information.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XWASM_GENERATE_DWARF: WasmArgument<Boolean> = WasmArgument("XWASM_GENERATE_DWARF")

    /**
     * Dump reachability information collected about declarations while performing DCE to a file. The format will be chosen automatically based on the file extension. Supported output formats include JSON for .json, a JS const initialized with a plain object containing information for .js, and plain text for all other file types.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_DCE_DUMP_REACHABILITY_INFO_TO_FILE: WasmArgument<String?> =
        WasmArgument("XIR_DCE_DUMP_REACHABILITY_INFO_TO_FILE")

    /**
     * Dump the IR size of each declaration into a file. The format will be chosen automatically depending on the file extension. Supported output formats include JSON for .json, a JS const initialized with a plain object containing information for .js, and plain text for all other file types.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_DUMP_DECLARATION_IR_SIZES_TO_FILE: WasmArgument<String?> =
        WasmArgument("XIR_DUMP_DECLARATION_IR_SIZES_TO_FILE")
  }
}
