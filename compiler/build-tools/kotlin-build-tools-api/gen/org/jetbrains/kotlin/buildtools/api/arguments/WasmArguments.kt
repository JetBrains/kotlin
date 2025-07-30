// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Boolean
import kotlin.String
import kotlin.jvm.JvmField

/**
 * @since 2.3.0
 */
@ExperimentalCompilerArgument
public interface WasmArguments : CommonKlibBasedArguments {
  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: WasmArgument<V>): V

  /**
   * Set the [value] for option specified by [key], overriding any previous value for that option.
   */
  public operator fun <V> `set`(key: WasmArgument<V>, `value`: V)

  public operator fun contains(key: WasmArgument<*>): Boolean

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
    /**
     * Use the WebAssembly compiler backend.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM: WasmArgument<Boolean> = WasmArgument("X_WASM")

    /**
     * Set up the Wasm target (wasm-js or wasm-wasi).
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_TARGET: WasmArgument<String?> = WasmArgument("X_WASM_TARGET")

    /**
     * Add debug info to the compiled WebAssembly module.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_DEBUG_INFO: WasmArgument<Boolean> = WasmArgument("X_WASM_DEBUG_INFO")

    /**
     * Avoid optimizations that can break debugging.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_DEBUG_FRIENDLY: WasmArgument<Boolean> = WasmArgument("X_WASM_DEBUG_FRIENDLY")

    /**
     * Generate a .wat file.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_GENERATE_WAT: WasmArgument<Boolean> = WasmArgument("X_WASM_GENERATE_WAT")

    /**
     * Enable support for 'KClass.qualifiedName'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_KCLASS_FQN: WasmArgument<Boolean> = WasmArgument("X_WASM_KCLASS_FQN")

    /**
     * Turn on range checks for array access functions.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_ENABLE_ARRAY_RANGE_CHECKS: WasmArgument<Boolean> =
        WasmArgument("X_WASM_ENABLE_ARRAY_RANGE_CHECKS")

    /**
     * Turn on asserts.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_ENABLE_ASSERTS: WasmArgument<Boolean> = WasmArgument("X_WASM_ENABLE_ASSERTS")

    /**
     * Use traps instead of throwing exceptions.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS: WasmArgument<Boolean> =
        WasmArgument("X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS")

    /**
     * Use an updated version of the exception proposal with try_table.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_USE_NEW_EXCEPTION_PROPOSAL: WasmArgument<Boolean> =
        WasmArgument("X_WASM_USE_NEW_EXCEPTION_PROPOSAL")

    /**
     * Don't use WebAssembly.JSTag for throwing and catching exceptions
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_NO_JSTAG: WasmArgument<Boolean> = WasmArgument("X_WASM_NO_JSTAG")

    /**
     * Generates devtools custom formatters (https://firefox-source-docs.mozilla.org/devtools-user/custom_formatters) for Kotlin/Wasm values
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_DEBUGGER_CUSTOM_FORMATTERS: WasmArgument<Boolean> =
        WasmArgument("X_WASM_DEBUGGER_CUSTOM_FORMATTERS")

    /**
     * Insert source mappings from libraries even if their sources are unavailable on the end-user machine.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES: WasmArgument<Boolean> =
        WasmArgument("X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES")

    /**
     * Preserve wasm file structure between IC runs.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_PRESERVE_IC_ORDER: WasmArgument<Boolean> =
        WasmArgument("X_WASM_PRESERVE_IC_ORDER")

    /**
     * Do not commit IC cache updates.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_IC_CACHE_READONLY: WasmArgument<Boolean> =
        WasmArgument("X_WASM_IC_CACHE_READONLY")

    /**
     * Generate DWARF debug information.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_GENERATE_DWARF: WasmArgument<Boolean> = WasmArgument("X_WASM_GENERATE_DWARF")

    /**
     * Dump reachability information collected about declarations while performing DCE to a file. The format will be chosen automatically based on the file extension. Supported output formats include JSON for .json, a JS const initialized with a plain object containing information for .js, and plain text for all other file types.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE: WasmArgument<String?> =
        WasmArgument("X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE")

    /**
     * Dump the IR size of each declaration into a file. The format will be chosen automatically depending on the file extension. Supported output formats include JSON for .json, a JS const initialized with a plain object containing information for .js, and plain text for all other file types.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE: WasmArgument<String?> =
        WasmArgument("X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE")
  }
}
